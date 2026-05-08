package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.katta.StorageClass;
import org.cryptomator.hub.entities.katta.StorageProfileS3STS;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import software.amazon.awssdk.regions.Region;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(title = "StorageProfileS3STSDto")
public final class StorageProfileS3STSDto extends StorageProfileS3StaticDto {

	public enum S3_SERVERSIDE_ENCRYPTION {
		NONE, SSE_AES256, SSE_KMS_DEFAULT
	}

	//======================================================================
	// (2) STS only: bucket creation (only relevant for Desktop client)
	//======================================================================
	@JsonProperty(value = "region", required = true, defaultValue = "us-east-1")
	@Schema(description = "Default region selected in the frontend/client to create bucket in.", examples = "us-east-1", defaultValue = "us-east-1")
	String region = "us-east-1";

	@JsonProperty(value = "regions", required = true)
	@Schema(description = "List of selectable regions in the frontend/client to create bucket in. Defaults to full list from AWS SDK.")
	List<String> regions = Region.regions().stream().map(Region::id).toList();

	@JsonProperty(value = "bucketPrefix", required = true)
	@Schema(description = "Buckets are created with name <bucket prefix><vault UUID>.", examples = "katta")
	String bucketPrefix;

	@JsonProperty(value = "stsRoleCreateBucketClient", required = true)
	@Schema(description = "STS role for clients to assume to create buckets. Will be the same as stsRoleCreateBucketHub for AWS, different for MinIO.", examples = "arn:aws:iam::<ACCOUNT ID>:role/katta-createbucket")
	String stsRoleCreateBucketClient;

	@JsonProperty(value = "stsRoleCreateBucketHub", required = true)
	@Schema(description = "STS role for frontend to assume to create buckets (used with inline policy and passed to hub storage). Will be the same as stsRoleCreateBucketClient for AWS, different for MinIO.", examples = "arn:aws:iam::<ACCOUNT ID>:role/katta-createbucket")
	String stsRoleCreateBucketHub;

	@JsonProperty("stsEndpoint")
	@Schema(description = "STS endpoint to use for AssumeRoleWithWebIdentity and AssumeRole for getting a temporary access token passed to the storage. Defaults to AWS SDK default.", nullable = true)
	String stsEndpoint;

	@JsonProperty(value = "bucketVersioning", defaultValue = "true", required = true)
	@Schema(description = "Enable bucket versioning upon bucket creation", defaultValue = "true", required = true)
	Boolean bucketVersioning = true;

	@JsonProperty(value = "bucketAcceleration")
	@Schema(description = "Enable bucket versioning upon bucket creation (null for MinIO)", nullable = true)
	Boolean bucketAcceleration = null;

	@JsonProperty(value = "bucketEncryption", required = true)
	@Schema(description = "Enable bucket versioning upon bucket creation", required = true)
	S3_SERVERSIDE_ENCRYPTION bucketEncryption = S3_SERVERSIDE_ENCRYPTION.NONE;

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@JsonProperty(value = "stsRoleAccessBucketAssumeRoleWithWebIdentity", required = true)
	@Schema(description = "roleArn to for STS AssumeRoleWithWebIdentity (AWS and MinIO)", examples = "arn:aws:iam::930717317329:role/katta_chain_01")
	String stsRoleAccessBucketAssumeRoleWithWebIdentity;

	@JsonProperty(value = "stsRoleAccessBucketAssumeRoleTaggedSession")
	@Schema(description = "roleArn to assume for STS AssumeRole in role chaining (AWS only, not MinIO)", examples = "arn:aws:iam::930717317329:role/katta_chain_02", nullable = true)
	String stsRoleAccessBucketAssumeRoleTaggedSession;


	@JsonProperty(value = "stsDurationSeconds", required = false)
	@Schema(description = "Token lifetime for STS tokens assumed. Defaults to AWS/MinIO defaults", nullable = true)
	Integer stsDurationSeconds;

	@JsonProperty(value = "stsSessionTag", required = true)
	@Schema(description = "Session tag to use for role chaining (AWS only, not MinIO). Defaults to \"Vault\"", nullable = false, defaultValue = "Vault")
	String stsSessionTag;

	public StorageProfileS3STSDto() {
		// jackson
	}

	public StorageProfileS3STSDto(final UUID id, final String name, final Protocol protocol, final boolean archived, final String endpoint, final boolean withPathStyleAccessEnabled, final S3_STORAGE_CLASSES storageClass, final String region, final List<String> regions, final String bucketPrefix, final String stsRoleCreateBucketClient, final String stsRoleCreateBucketHub, final String stsEndpoint, final boolean bucketVersioning, final Boolean bucketAcceleration, final S3_SERVERSIDE_ENCRYPTION bucketEncryption, final String stsRoleAccessBucketAssumeRoleWithWebIdentity, final String stsRoleAccessBucketAssumeRoleTaggedSession, final Integer stsDurationSeconds, final String stsSessionTag) {
		super(id, name, protocol, archived, endpoint, withPathStyleAccessEnabled, storageClass);
		this.region = region;
		this.regions = regions;
		this.bucketPrefix = bucketPrefix;
		this.stsRoleCreateBucketClient = stsRoleCreateBucketClient;
		this.stsRoleCreateBucketHub = stsRoleCreateBucketHub;
		this.stsEndpoint = stsEndpoint;
		this.bucketVersioning = bucketVersioning;
		this.bucketAcceleration = bucketAcceleration;
		this.bucketEncryption = bucketEncryption;
		this.stsRoleAccessBucketAssumeRoleWithWebIdentity = stsRoleAccessBucketAssumeRoleWithWebIdentity;
		this.stsRoleAccessBucketAssumeRoleTaggedSession = stsRoleAccessBucketAssumeRoleTaggedSession;
		this.stsDurationSeconds = stsDurationSeconds;
		this.stsSessionTag = stsSessionTag;
	}

	static StorageProfileS3STSDto fromEntity(final StorageProfileS3STS storageProfile) {
		return new StorageProfileS3STSDto(
				storageProfile.id,
				storageProfile.name,
				Protocol.s3sts,
				storageProfile.archived,
				storageProfile.endpoint,
				storageProfile.withPathStyleAccessEnabled,
				S3_STORAGE_CLASSES.valueOf(storageProfile.storageClass.name()),
				storageProfile.region,
				storageProfile.regions,
				storageProfile.bucketPrefix,
				storageProfile.stsRoleCreateBucketClient,
				storageProfile.stsRoleCreateBucketHub,
				storageProfile.stsEndpoint,
				storageProfile.bucketVersioning,
				storageProfile.bucketAcceleration,
				S3_SERVERSIDE_ENCRYPTION.valueOf(storageProfile.bucketEncryption),
				storageProfile.stsRoleAccessBucketAssumeRoleWithWebIdentity,
				storageProfile.stsRoleAccessBucketAssumeRoleTaggedSession,
				storageProfile.stsDurationSeconds,
				storageProfile.stsSessionTag
		);
	}

	public StorageProfileS3STS toEntity() {
		final StorageProfileS3STS storageProfile = new StorageProfileS3STS();
		storageProfile.id = this.id;
		storageProfile.name = this.name;
		storageProfile.archived = this.archived;
		storageProfile.endpoint = this.endpoint;
		storageProfile.withPathStyleAccessEnabled = this.withPathStyleAccessEnabled;
		storageProfile.storageClass = StorageClass.valueOf(this.storageClass.name());
		storageProfile.region = this.region;
		storageProfile.regions = this.regions;
		storageProfile.bucketPrefix = this.bucketPrefix;
		storageProfile.stsRoleCreateBucketClient = this.stsRoleCreateBucketClient;
		storageProfile.stsRoleCreateBucketHub = this.stsRoleCreateBucketHub;
		storageProfile.stsEndpoint = this.stsEndpoint;
		storageProfile.bucketVersioning = this.bucketVersioning;
		storageProfile.bucketAcceleration = this.bucketAcceleration;
		storageProfile.bucketEncryption = this.bucketEncryption.name();
		storageProfile.stsRoleAccessBucketAssumeRoleWithWebIdentity = this.stsRoleAccessBucketAssumeRoleWithWebIdentity;
		storageProfile.stsRoleAccessBucketAssumeRoleTaggedSession = this.stsRoleAccessBucketAssumeRoleTaggedSession;
		storageProfile.stsDurationSeconds = this.stsDurationSeconds;
		storageProfile.stsSessionTag = this.stsSessionTag;
		return storageProfile;
	}

	public String region() {
		return region;
	}

	public List<String> regions() {
		return regions;
	}

	public String bucketPrefix() {
		return bucketPrefix;
	}

	public String stsRoleCreateBucketClient() {
		return stsRoleCreateBucketClient;
	}

	public String stsRoleCreateBucketHub() {
		return stsRoleCreateBucketHub;
	}

	public String stsEndpoint() {
		return stsEndpoint;
	}

	public Boolean bucketVersioning() {
		return bucketVersioning;
	}

	public Boolean bucketAcceleration() {
		return bucketAcceleration;
	}

	public S3_SERVERSIDE_ENCRYPTION bucketEncryption() {
		return bucketEncryption;
	}

	public String stsRoleAccessBucketAssumeRoleWithWebIdentity() {
		return stsRoleAccessBucketAssumeRoleWithWebIdentity;
	}

	public String stsRoleAccessBucketAssumeRoleTaggedSession() {
		return stsRoleAccessBucketAssumeRoleTaggedSession;
	}

	public Integer stsDurationSeconds() {
		return stsDurationSeconds;
	}

	public String stsSessionTag() {
		return stsSessionTag;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		StorageProfileS3STSDto s3STSDto = (StorageProfileS3STSDto) o;
		return Objects.equals(region, s3STSDto.region) && Objects.equals(regions, s3STSDto.regions) && Objects.equals(bucketPrefix, s3STSDto.bucketPrefix) && Objects.equals(stsRoleCreateBucketClient, s3STSDto.stsRoleCreateBucketClient) && Objects.equals(stsRoleCreateBucketHub, s3STSDto.stsRoleCreateBucketHub) && Objects.equals(stsEndpoint, s3STSDto.stsEndpoint) && Objects.equals(bucketVersioning, s3STSDto.bucketVersioning) && Objects.equals(bucketAcceleration, s3STSDto.bucketAcceleration) && bucketEncryption == s3STSDto.bucketEncryption && Objects.equals(stsRoleAccessBucketAssumeRoleWithWebIdentity, s3STSDto.stsRoleAccessBucketAssumeRoleWithWebIdentity) && Objects.equals(stsRoleAccessBucketAssumeRoleTaggedSession, s3STSDto.stsRoleAccessBucketAssumeRoleTaggedSession) && Objects.equals(stsDurationSeconds, s3STSDto.stsDurationSeconds) && Objects.equals(stsSessionTag, s3STSDto.stsSessionTag);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Objects.hashCode(region);
		result = 31 * result + Objects.hashCode(regions);
		result = 31 * result + Objects.hashCode(bucketPrefix);
		result = 31 * result + Objects.hashCode(stsRoleCreateBucketClient);
		result = 31 * result + Objects.hashCode(stsRoleCreateBucketHub);
		result = 31 * result + Objects.hashCode(stsEndpoint);
		result = 31 * result + Objects.hashCode(bucketVersioning);
		result = 31 * result + Objects.hashCode(bucketAcceleration);
		result = 31 * result + Objects.hashCode(bucketEncryption);
		result = 31 * result + Objects.hashCode(stsRoleAccessBucketAssumeRoleWithWebIdentity);
		result = 31 * result + Objects.hashCode(stsRoleAccessBucketAssumeRoleTaggedSession);
		result = 31 * result + Objects.hashCode(stsDurationSeconds);
		result = 31 * result + Objects.hashCode(stsSessionTag);
		return result;
	}
}
