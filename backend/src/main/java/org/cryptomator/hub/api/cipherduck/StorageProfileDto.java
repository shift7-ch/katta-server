package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Id;
import org.cryptomator.hub.entities.cipherduck.StorageProfile;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3STS;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3Static;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;


// pro-memoria @Schema
// - "required" is taken from @JSONProperty
// - "defaultValue" needs to be repeated
@Schema(

		title = "StorageProfile",
		oneOf = {StorageProfileS3StaticDto.class, StorageProfileS3STSDto.class},
		discriminatorMapping = {

				@DiscriminatorMapping(value = "S3STATIC", schema = StorageProfileS3StaticDto.class),
				@DiscriminatorMapping(value = "S3STS", schema = StorageProfileS3STSDto.class),
		},
		discriminatorProperty = "protocol"
)
// although we have a dto hierarchy (StorageProfileDto <- StorageProfileS3Dto <- StorageProfileS3STSDto), the DB schema keeps the the tables separate (without foreign keys). There is one common GET service for listing and specific endpoints for POSTing profiles. Future profiles should inherit from StorageProfileDto. Serialized dtos are kept apart by a discriminator property, the openapi generators can de-serialized using it.
public abstract sealed class StorageProfileDto permits StorageProfileS3StaticDto {
	public enum Protocol {
		s3static("S3STATIC"),
		s3sts("S3STS");
		private final String protocol;

		private Protocol(final String protocol) {
			this.protocol = protocol;
		}

		@JsonValue
		public String getProtocol() {
			return protocol;
		}
	}

	@Id
	@JsonProperty(value = "id", required = true)
	@Schema(description = "Technical identifier for a storage profile. Must be unique UUID. Clients will use this as vendor in profile and provider in vault bookmark")
	UUID id;

	@JsonProperty(value = "name", required = true)
	@Schema(description = "Displayed when choosing type of a new vault in dropdown.")
	String name;

	//======================================================================
	// (3) client profile
	//======================================================================

	//----------------------------------------------------------------------
	// (3a) STS and permanent client profile attributes
	//----------------------------------------------------------------------
	@JsonProperty(value = "protocol", required = true)
	@Schema(description = "Storage protocol: S3 (permanent credentials) or S3STS (STS).")
	Protocol protocol;

	@JsonProperty(value = "archived", required = true, defaultValue = "false")
	@Schema(description = "For archived storage profiles, no vaults can be created any more.")
	boolean archived;

	public StorageProfileDto() {
		// jackson
	}

	public StorageProfileDto(final UUID id, final String name, final Protocol protocol, final boolean archived) {
		this.id = id;
		this.name = name;
		this.protocol = protocol;
		this.archived = archived;
	}

	static StorageProfileDto fromEntity(final StorageProfile storageProfile) {
		// TODO refactor to JEP 441 in JDK 21
		if (storageProfile instanceof StorageProfileS3STS storageProfileS3STS) {
			return StorageProfileS3STSDto.fromEntity(storageProfileS3STS);
		} else if (storageProfile instanceof StorageProfileS3Static storageProfileS3Static) {
			return StorageProfileS3StaticDto.fromEntity(storageProfileS3Static);
		} else {
			throw new IllegalStateException("StorageProfile is not of type StorageProfileS3 or StorageProfileS3STS");
		}
	}

	public UUID id() {
		return id;
	}
}
