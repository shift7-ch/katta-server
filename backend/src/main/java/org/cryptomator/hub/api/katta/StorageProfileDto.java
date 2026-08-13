package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import org.cryptomator.hub.entities.katta.StorageProfile;
import org.cryptomator.hub.entities.katta.StorageProfileS3STS;
import org.cryptomator.hub.entities.katta.StorageProfileS3Static;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

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
// although we have a dto hierarchy (StorageProfileDto <- StorageProfileS3StaticDto <- StorageProfileS3STSDto), the DB schema keeps the the tables separate (without foreign keys). There is one common GET service for listing and a single polymorphic endpoint for POSTing profiles, dispatching on the `protocol` discriminator. Future profiles should inherit from StorageProfileDto.
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "protocol", visible = true)
@JsonSubTypes({
		@JsonSubTypes.Type(value = StorageProfileS3StaticDto.class, name = "S3STATIC"),
		@JsonSubTypes.Type(value = StorageProfileS3STSDto.class, name = "S3STS")
})
public abstract sealed class StorageProfileDto permits StorageProfileS3StaticDto {

	public enum Protocol {
		@JsonProperty("S3STATIC") S3_STATIC,
		@JsonProperty("S3STS") S3_STS
	}

	// id is assigned by the server on creation; clients must not supply it (any supplied value is ignored).
	private final @Nullable UUID id;
	@NotNull
	private final String name;
	@NotNull
	private final Protocol protocol;
	private final boolean archived;

	protected StorageProfileDto(@Nullable UUID id, String name, Protocol protocol, boolean archived) {
		this.id = id;
		this.name = name;
		this.protocol = protocol;
		this.archived = archived;
	}

	@JsonProperty("id")
	@Schema(description = "Technical identifier for a storage profile, assigned by the server on creation (read-only). Clients use this as vendor in profile and provider in vault bookmark.", required = true, readOnly = true)
	public @Nullable UUID getId() {
		return id;
	}

	@JsonProperty("name")
	@Schema(description = "Displayed when choosing type of a new vault in dropdown.")
	public String getName() {
		return name;
	}

	@JsonProperty("protocol")
	@Schema(description = "Storage protocol: S3STATIC (permanent credentials) or S3STS (STS).")
	public Protocol getProtocol() {
		return protocol;
	}

	@JsonProperty("archived")
	@Schema(description = "For archived storage profiles, no vaults can be created any more.")
	public boolean isArchived() {
		return archived;
	}

	static StorageProfileDto fromEntity(final StorageProfile storageProfile) {
		return switch (storageProfile) {
			case StorageProfileS3STS profile -> StorageProfileS3STSDto.fromEntity(profile);
			case StorageProfileS3Static profile -> StorageProfileS3StaticDto.fromEntity(profile);
			default -> throw new IllegalStateException("Unexpected value: " + storageProfile);
		};
	}

	public abstract StorageProfile toEntity();
}
