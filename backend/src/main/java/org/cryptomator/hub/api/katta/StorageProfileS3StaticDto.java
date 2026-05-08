package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.katta.StorageClass;
import org.cryptomator.hub.entities.katta.StorageProfileS3Static;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Objects;
import java.util.UUID;

@Schema(title = "StorageProfileS3StaticDto")
public sealed class StorageProfileS3StaticDto extends StorageProfileDto permits StorageProfileS3STSDto {

	public enum S3_STORAGE_CLASSES {
		STANDARD, INTELLIGENT_TIERING, STANDARD_IA, ONEZONE_IA, REDUCED_REDUNDANCY, GLACIER, GLACIER_IR, DEEP_ARCHIVE
	}

	//======================================================================
	// (1) STS and permanent:
	// - bucket creation frontend/desktop client (STS)
	// - template upload (STS and permanent)
	// - client profile (STS and permanent)
	//======================================================================

	@JsonProperty("endpoint")
	@Schema(description = "Full S3 endpoint URL for template upload/bucket creation. If unset, defaults to AWS SDK defaults.", examples = "https://s3-us-gov-west-1.amazonaws.com", nullable = true)
	String endpoint;

	@JsonProperty(value = "withPathStyleAccessEnabled")
	@Schema(description = "Whether to use path style for S3 endpoint for template upload/bucket creation.", examples = "false", defaultValue = "false")
	Boolean withPathStyleAccessEnabled = false;

	@JsonProperty(value = "storageClass", defaultValue = "STANDARD")
	@Schema(description = "Storage class for upload. Defaults to STANDARD", examples = "STANDARD", required = true)
	S3_STORAGE_CLASSES storageClass = S3_STORAGE_CLASSES.STANDARD;

	public StorageProfileS3StaticDto() {
		// jackson
	}

	public StorageProfileS3StaticDto(final UUID id, final String name, final Protocol protocol, final boolean archived, final String endpoint, final boolean withPathStyleAccessEnabled, final S3_STORAGE_CLASSES storageClass) {
		super(id, name, protocol, archived);
		this.endpoint = endpoint;
		this.withPathStyleAccessEnabled = withPathStyleAccessEnabled;
		this.storageClass = storageClass;
	}

	static StorageProfileS3StaticDto fromEntity(final StorageProfileS3Static storageProfile) {
		return new StorageProfileS3StaticDto(storageProfile.id, storageProfile.name, Protocol.s3static, storageProfile.archived, storageProfile.endpoint, storageProfile.withPathStyleAccessEnabled, S3_STORAGE_CLASSES.valueOf(storageProfile.storageClass.name()));
	}

	public StorageProfileS3Static toEntity() {
		final StorageProfileS3Static storageProfile = new StorageProfileS3Static();
		storageProfile.id = this.id;
		storageProfile.name = this.name;
		storageProfile.archived = this.archived;
		storageProfile.endpoint = this.endpoint;
		storageProfile.withPathStyleAccessEnabled = this.withPathStyleAccessEnabled;
		storageProfile.storageClass = StorageClass.valueOf(this.storageClass.name());
		return storageProfile;
	}

	public String endpoint() {
		return endpoint;
	}

	public Boolean withPathStyleAccessEnabled() {
		return withPathStyleAccessEnabled;
	}

	public S3_STORAGE_CLASSES storageClass() {
		return storageClass;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StorageProfileS3StaticDto that = (StorageProfileS3StaticDto) o;
		return Objects.equals(endpoint, that.endpoint) && Objects.equals(withPathStyleAccessEnabled, that.withPathStyleAccessEnabled) && storageClass == that.storageClass;
	}

	@Override
	public int hashCode() {
		return Objects.hash(endpoint, withPathStyleAccessEnabled, storageClass);
	}
}
