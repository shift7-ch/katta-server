package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.katta.StorageProfileS3STS;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(title = "StorageProfileS3STSDto")
public final class StorageProfileS3STSDto extends StorageProfileS3StaticDto {

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@JsonProperty(value = "stsRoleAccessBucketAssumeRoleWithWebIdentity", required = true)
	@Schema(description = "roleArn to for STS AssumeRoleWithWebIdentity (AWS and MinIO)", example = "arn:aws:iam::930717317329:role/katta_chain_01")
	String stsRoleAccessBucketAssumeRoleWithWebIdentity;

	@JsonProperty(value = "stsRoleAccessBucketAssumeRoleTaggedSession")
	@Schema(description = "roleArn to assume for STS AssumeRole in role chaining (AWS only, not MinIO)", example = "arn:aws:iam::930717317329:role/katta_chain_02", nullable = true)
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

	public StorageProfileS3STSDto(final UUID id, final String name, final Protocol protocol, final boolean archived, final String scheme, final String hostname, final Integer port, final boolean withPathStyleAccessEnabled, final S3_STORAGE_CLASSES storageClass, final String region, final List<String> regions, final String bucketPrefix, final String stsRoleCreateBucketClient, final String stsRoleCreateBucketHub, final String stsEndpoint, final boolean bucketVersioning, final Boolean bucketAcceleration, final S3_SERVERSIDE_ENCRYPTION bucketEncryption, final String stsRoleAccessBucketAssumeRoleWithWebIdentity, final String stsRoleAccessBucketAssumeRoleTaggedSession, final Integer stsDurationSeconds, final String stsSessionTag) {
		super(id, name, protocol, archived, scheme, hostname, port, withPathStyleAccessEnabled, storageClass, region, regions, bucketPrefix, stsRoleCreateBucketClient, stsRoleCreateBucketHub, stsEndpoint, bucketVersioning, bucketAcceleration, bucketEncryption);
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
				storageProfile.scheme,
				storageProfile.hostname,
				storageProfile.port,
				storageProfile.withPathStyleAccessEnabled,
				S3_STORAGE_CLASSES.valueOf(storageProfile.storageClass),
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
		storageProfile.protocol = this.protocol;
		storageProfile.archived = this.archived;
		storageProfile.scheme = this.scheme;
		storageProfile.hostname = this.hostname;
		storageProfile.port = this.port;
		storageProfile.withPathStyleAccessEnabled = this.withPathStyleAccessEnabled;
		storageProfile.storageClass = this.storageClass.toString();
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
		return Objects.equals(stsRoleAccessBucketAssumeRoleWithWebIdentity, s3STSDto.stsRoleAccessBucketAssumeRoleWithWebIdentity) && Objects.equals(stsRoleAccessBucketAssumeRoleTaggedSession, s3STSDto.stsRoleAccessBucketAssumeRoleTaggedSession) && Objects.equals(stsDurationSeconds, s3STSDto.stsDurationSeconds) && Objects.equals(stsSessionTag, s3STSDto.stsSessionTag);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Objects.hashCode(stsRoleAccessBucketAssumeRoleWithWebIdentity);
		result = 31 * result + Objects.hashCode(stsRoleAccessBucketAssumeRoleTaggedSession);
		result = 31 * result + Objects.hashCode(stsDurationSeconds);
		result = 31 * result + Objects.hashCode(stsSessionTag);
		return result;
	}
}
