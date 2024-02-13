package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Id;
import org.cryptomator.hub.entities.cipherduck.StorageProfile;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3STS;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(
		title = "StorageProfile",
		oneOf = {StorageProfileS3Dto.class, StorageProfileS3STSDto.class},
		discriminatorMapping = {
				@DiscriminatorMapping(value = "s3-hub", schema = StorageProfileS3Dto.class),
				@DiscriminatorMapping(value = "s3-hub-sts", schema = StorageProfileS3STSDto.class),
		},
		discriminatorProperty = "protocol"
)
// pro-memoria @Schema
// - "required" is taken from @JSONProperty
// - "defaultValue" needs to be repeated
public abstract sealed class StorageProfileDto permits StorageProfileS3Dto {
	public enum Protocol {
		s3("s3-hub"),
		s3sts("s3-hub-sts");
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
	@Schema(description = "Storage protocol: s3-hub (permanent credentials) or s3-hub-sts (STS). Defaults to s3-hub-sts.")
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
		} else if (storageProfile instanceof StorageProfileS3 storageProfileS3) {
			return StorageProfileS3Dto.fromEntity(storageProfileS3);
		} else {
			throw new IllegalStateException("StorageProfile is not of type StorageProfileS3 or StorageProfileS3STS");
		}
	}

	public UUID id() {
		return id;
	}
}
