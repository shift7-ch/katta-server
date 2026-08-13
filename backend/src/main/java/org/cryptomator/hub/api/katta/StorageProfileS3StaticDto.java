package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.cryptomator.hub.entities.katta.S3StorageClass;
import org.cryptomator.hub.entities.katta.StorageProfileS3Static;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(title = "StorageProfileS3StaticDto")
public sealed class StorageProfileS3StaticDto extends StorageProfileDto permits StorageProfileS3STSDto {

	//======================================================================
	// (1) STS and permanent:
	// - bucket creation frontend/desktop client (STS and permanent)
	// - template upload (STS and permanent)
	// - client profile (STS and permanent)
	//======================================================================

	@URL
	private final @Nullable String endpoint;
	private final boolean pathStyleAccessEnabled;
	@NotNull
	private final S3StorageClass storageClass;
	@NotNull
	private final String region;
	@NotNull
	private final List<String> regions;
	@NotNull
	private final String bucketPrefix;

	@JsonCreator
	public StorageProfileS3StaticDto(
			@JsonProperty("id") @Nullable UUID id,
			@JsonProperty("name") String name,
			@JsonProperty("protocol") Protocol protocol,
			@JsonProperty("archived") boolean archived,
			@JsonProperty("endpoint") @Nullable String endpoint,
			@JsonProperty("pathStyleAccessEnabled") boolean pathStyleAccessEnabled,
			@JsonProperty("storageClass") S3StorageClass storageClass,
			@JsonProperty("region") String region,
			@JsonProperty("regions") List<String> regions,
			@JsonProperty("bucketPrefix") String bucketPrefix) {
		super(id, name, protocol, archived);
		this.endpoint = endpoint;
		this.pathStyleAccessEnabled = pathStyleAccessEnabled;
		this.storageClass = storageClass;
		this.region = region;
		this.regions = regions;
		this.bucketPrefix = bucketPrefix;
	}

	@JsonProperty("endpoint")
	@Schema(description = "Full S3 endpoint URL for template upload/bucket creation. If unset, defaults to AWS SDK defaults.", examples = "https://s3-us-gov-west-1.amazonaws.com", nullable = true)
	public @Nullable String getEndpoint() {
		return endpoint;
	}

	@JsonProperty("pathStyleAccessEnabled")
	@Schema(description = "Whether to use path style for S3 endpoint for template upload/bucket creation.", examples = "false", defaultValue = "false")
	public boolean isPathStyleAccessEnabled() {
		return pathStyleAccessEnabled;
	}

	@JsonProperty("storageClass")
	@Schema(description = "Storage class for upload. Defaults to STANDARD", examples = "STANDARD", required = true)
	public S3StorageClass getStorageClass() {
		return storageClass;
	}

	@JsonProperty("region")
	@Schema(description = "Default region selected in the frontend/client to create bucket in.", examples = "us-east-1", defaultValue = "us-east-1", required = true)
	public String getRegion() {
		return region;
	}

	@JsonProperty("regions")
	@Schema(description = "List of selectable regions in the frontend/client to create bucket in. Defaults to full list from AWS SDK.", required = true)
	public List<String> getRegions() {
		return regions;
	}

	@JsonProperty("bucketPrefix")
	@Schema(description = "Buckets are created with name <bucket prefix><vault UUID>.", examples = "katta", required = true)
	public String getBucketPrefix() {
		return bucketPrefix;
	}

	static StorageProfileS3StaticDto fromEntity(StorageProfileS3Static entity) {
		return new StorageProfileS3StaticDto(entity.id, entity.name, Protocol.S3_STATIC, entity.archived, entity.endpoint, entity.pathStyleAccessEnabled, entity.storageClass, entity.region, entity.regions == null ? List.of() : entity.regions, entity.bucketPrefix);
	}

	@Override
	public StorageProfileS3Static toEntity() {
		final StorageProfileS3Static entity = new StorageProfileS3Static();
		entity.id = getId();
		entity.name = getName();
		entity.archived = isArchived();
		entity.endpoint = endpoint;
		entity.pathStyleAccessEnabled = pathStyleAccessEnabled;
		entity.storageClass = storageClass;
		entity.region = region;
		entity.regions = regions;
		entity.bucketPrefix = bucketPrefix;
		return entity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StorageProfileS3StaticDto that = (StorageProfileS3StaticDto) o;
		return pathStyleAccessEnabled == that.pathStyleAccessEnabled
				&& Objects.equals(endpoint, that.endpoint)
				&& storageClass == that.storageClass
				&& Objects.equals(region, that.region)
				&& Objects.equals(regions, that.regions)
				&& Objects.equals(bucketPrefix, that.bucketPrefix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(endpoint, pathStyleAccessEnabled, storageClass, region, regions, bucketPrefix);
	}
}
