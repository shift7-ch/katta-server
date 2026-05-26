package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.cryptomator.hub.entities.katta.S3StorageClass;
import org.cryptomator.hub.entities.katta.StorageProfileS3STS;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(title = "StorageProfileS3STSDto")
public final class StorageProfileS3STSDto extends StorageProfileS3StaticDto {

	//======================================================================
	// (2) STS only: bucket creation (only relevant for Desktop client)
	//======================================================================

	@NotNull
	private final String stsRoleCreateBucketClient;
	@NotNull
	private final String stsRoleCreateBucketHub;
	private final String stsEndpoint;
	private final boolean bucketVersioning;
	private final Boolean bucketAcceleration;

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@NotNull
	private final String stsRoleAccessBucketAssumeRoleWithWebIdentity;
	private final String stsRoleAccessBucketAssumeRoleTaggedSession;
	private final Integer stsDurationSeconds;
	@NotNull
	private final String stsSessionTag;

	@JsonCreator
	public StorageProfileS3STSDto(
			@JsonProperty("id") UUID id,
			@JsonProperty("name") String name,
			@JsonProperty("protocol") Protocol protocol,
			@JsonProperty("archived") boolean archived,
			@JsonProperty("endpoint") String endpoint,
			@JsonProperty("pathStyleAccessEnabled") boolean pathStyleAccessEnabled,
			@JsonProperty("storageClass") S3StorageClass storageClass,
			@JsonProperty("region") String region,
			@JsonProperty("regions") List<String> regions,
			@JsonProperty("bucketPrefix") String bucketPrefix,
			@JsonProperty("stsRoleCreateBucketClient") String stsRoleCreateBucketClient,
			@JsonProperty("stsRoleCreateBucketHub") String stsRoleCreateBucketHub,
			@JsonProperty("stsEndpoint") String stsEndpoint,
			@JsonProperty("bucketVersioning") boolean bucketVersioning,
			@JsonProperty("bucketAcceleration") Boolean bucketAcceleration,
			@JsonProperty("stsRoleAccessBucketAssumeRoleWithWebIdentity") String stsRoleAccessBucketAssumeRoleWithWebIdentity,
			@JsonProperty("stsRoleAccessBucketAssumeRoleTaggedSession") String stsRoleAccessBucketAssumeRoleTaggedSession,
			@JsonProperty("stsDurationSeconds") Integer stsDurationSeconds,
			@JsonProperty("stsSessionTag") String stsSessionTag) {
		super(id, name, protocol, archived, endpoint, pathStyleAccessEnabled, storageClass, region, regions, bucketPrefix);
		this.stsRoleCreateBucketClient = stsRoleCreateBucketClient;
		this.stsRoleCreateBucketHub = stsRoleCreateBucketHub;
		this.stsEndpoint = stsEndpoint;
		this.bucketVersioning = bucketVersioning;
		this.bucketAcceleration = bucketAcceleration;
		this.stsRoleAccessBucketAssumeRoleWithWebIdentity = stsRoleAccessBucketAssumeRoleWithWebIdentity;
		this.stsRoleAccessBucketAssumeRoleTaggedSession = stsRoleAccessBucketAssumeRoleTaggedSession;
		this.stsDurationSeconds = stsDurationSeconds;
		this.stsSessionTag = stsSessionTag;
	}

	@JsonProperty("stsRoleCreateBucketClient")
	@Schema(description = "STS role for clients to assume to create buckets. Will be the same as stsRoleCreateBucketHub for AWS, different for MinIO.", examples = "arn:aws:iam::<ACCOUNT ID>:role/katta-createbucket", required = true)
	public String getStsRoleCreateBucketClient() {
		return stsRoleCreateBucketClient;
	}

	@JsonProperty("stsRoleCreateBucketHub")
	@Schema(description = "STS role for frontend to assume to create buckets (used with inline policy and passed to hub storage). Will be the same as stsRoleCreateBucketClient for AWS, different for MinIO.", examples = "arn:aws:iam::<ACCOUNT ID>:role/katta-createbucket", required = true)
	public String getStsRoleCreateBucketHub() {
		return stsRoleCreateBucketHub;
	}

	@JsonProperty("stsEndpoint")
	@Schema(description = "STS endpoint to use for AssumeRoleWithWebIdentity and AssumeRole for getting a temporary access token passed to the storage. Defaults to AWS SDK default.", nullable = true)
	public String getStsEndpoint() {
		return stsEndpoint;
	}

	@JsonProperty("bucketVersioning")
	@Schema(description = "Enable bucket versioning upon bucket creation", defaultValue = "true", required = true)
	public boolean isBucketVersioning() {
		return bucketVersioning;
	}

	@JsonProperty("bucketAcceleration")
	@Schema(description = "Enable bucket versioning upon bucket creation (null for MinIO)", nullable = true)
	public Boolean getBucketAcceleration() {
		return bucketAcceleration;
	}

	@JsonProperty("stsRoleAccessBucketAssumeRoleWithWebIdentity")
	@Schema(description = "roleArn to for STS AssumeRoleWithWebIdentity (AWS and MinIO)", examples = "arn:aws:iam::930717317329:role/katta_chain_01", required = true)
	public String getStsRoleAccessBucketAssumeRoleWithWebIdentity() {
		return stsRoleAccessBucketAssumeRoleWithWebIdentity;
	}

	@JsonProperty("stsRoleAccessBucketAssumeRoleTaggedSession")
	@Schema(description = "roleArn to assume for STS AssumeRole in role chaining (AWS only, not MinIO)", examples = "arn:aws:iam::930717317329:role/katta_chain_02", nullable = true)
	public String getStsRoleAccessBucketAssumeRoleTaggedSession() {
		return stsRoleAccessBucketAssumeRoleTaggedSession;
	}

	@JsonProperty("stsDurationSeconds")
	@Schema(description = "Token lifetime for STS tokens assumed. Defaults to AWS/MinIO defaults", nullable = true)
	public Integer getStsDurationSeconds() {
		return stsDurationSeconds;
	}

	@JsonProperty("stsSessionTag")
	@Schema(description = "Session tag to use for role chaining (AWS only, not MinIO). Defaults to \"Vault\"", defaultValue = "Vault", required = true)
	public String getStsSessionTag() {
		return stsSessionTag;
	}

	static StorageProfileS3STSDto fromEntity(StorageProfileS3STS entity) {
		return new StorageProfileS3STSDto(
				entity.id,
				entity.name,
				Protocol.S3_STS,
				entity.archived,
				entity.endpoint,
				entity.pathStyleAccessEnabled,
				entity.storageClass,
				entity.region,
				entity.regions == null ? List.of() : entity.regions,
				entity.bucketPrefix,
				entity.stsRoleCreateBucketClient,
				entity.stsRoleCreateBucketHub,
				entity.stsEndpoint,
				entity.bucketVersioning,
				entity.bucketAcceleration,
				entity.stsRoleAccessBucketAssumeRoleWithWebIdentity,
				entity.stsRoleAccessBucketAssumeRoleTaggedSession,
				entity.stsDurationSeconds,
				entity.stsSessionTag
		);
	}

	@Override
	public StorageProfileS3STS toEntity() {
		final StorageProfileS3STS entity = new StorageProfileS3STS();
		entity.id = getId();
		entity.name = getName();
		entity.archived = isArchived();
		entity.endpoint = getEndpoint();
		entity.pathStyleAccessEnabled = isPathStyleAccessEnabled();
		entity.storageClass = getStorageClass();
		entity.region = getRegion();
		entity.regions = getRegions();
		entity.bucketPrefix = getBucketPrefix();
		entity.stsRoleCreateBucketClient = stsRoleCreateBucketClient;
		entity.stsRoleCreateBucketHub = stsRoleCreateBucketHub;
		entity.stsEndpoint = stsEndpoint;
		entity.bucketVersioning = bucketVersioning;
		entity.bucketAcceleration = bucketAcceleration;
		entity.stsRoleAccessBucketAssumeRoleWithWebIdentity = stsRoleAccessBucketAssumeRoleWithWebIdentity;
		entity.stsRoleAccessBucketAssumeRoleTaggedSession = stsRoleAccessBucketAssumeRoleTaggedSession;
		entity.stsDurationSeconds = stsDurationSeconds;
		entity.stsSessionTag = stsSessionTag;
		return entity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		StorageProfileS3STSDto that = (StorageProfileS3STSDto) o;
		return bucketVersioning == that.bucketVersioning
				&& Objects.equals(stsRoleCreateBucketClient, that.stsRoleCreateBucketClient)
				&& Objects.equals(stsRoleCreateBucketHub, that.stsRoleCreateBucketHub)
				&& Objects.equals(stsEndpoint, that.stsEndpoint)
				&& Objects.equals(bucketAcceleration, that.bucketAcceleration)
				&& Objects.equals(stsRoleAccessBucketAssumeRoleWithWebIdentity, that.stsRoleAccessBucketAssumeRoleWithWebIdentity)
				&& Objects.equals(stsRoleAccessBucketAssumeRoleTaggedSession, that.stsRoleAccessBucketAssumeRoleTaggedSession)
				&& Objects.equals(stsDurationSeconds, that.stsDurationSeconds)
				&& Objects.equals(stsSessionTag, that.stsSessionTag);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), stsRoleCreateBucketClient, stsRoleCreateBucketHub, stsEndpoint, bucketVersioning, bucketAcceleration, stsRoleAccessBucketAssumeRoleWithWebIdentity, stsRoleAccessBucketAssumeRoleTaggedSession, stsDurationSeconds, stsSessionTag);
	}
}
