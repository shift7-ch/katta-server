package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3STS;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import software.amazon.awssdk.regions.Region;

import java.util.List;
import java.util.UUID;

public final class StorageProfileS3STSDto extends StorageProfileS3Dto {

	public enum S3_SERVERSIDE_ENCRYPTION {
		NONE, SSE_AES256, SSE_KMS_DEFAULT
	}

	//======================================================================
	// (2) STS only: bucket creation
	//======================================================================
	@JsonProperty(value = "region", required = true, defaultValue = "us-east-1")
	@Schema(description = "Default region selected in the frontend/client to create bucket in.", example = "443", defaultValue = "us-east-1")
	String region = "us-east-1";

	@JsonProperty(value = "regions", required = true)
	@Schema(description = "List of selectable regions in the frontend/client to create bucket in. Defaults to full list from AWS SDK.")
	List<String> regions = Region.regions().stream().map(Region::id).toList();

	@JsonProperty(value = "bucketPrefix", required = true)
	@Schema(description = "Buckets are create with name <bucket prefix><vault UUID>.", example = "cipherduck")
	String bucketPrefix;

	@JsonProperty(value = "stsRoleArnClient", required = true)
	@Schema(description = "STS role for clients to assume to create buckets. Will be the same as stsRoleArnHub for AWS, different for MinIO.", example = "arn:aws:iam::<ACCOUNT ID>:role/cipherduck-createbucket")
	String stsRoleArnClient;

	@JsonProperty(value = "stsRoleArnHub", required = true)
	@Schema(description = "STS role for frontend to assume to create buckets (used with inline policy and passed to hub storage). Will be the same as stsRoleArnClient for AWS, different for MinIO.", example = "arn:aws:iam::<ACCOUNT ID>:role/cipherduck-createbucket")
	String stsRoleArnHub;

	@JsonProperty("stsEndpoint")
	@Schema(description = "STS endpoint to use for AssumeRoleWithWebIdentity and AssumeRole for getting a temporary access token passed to the storage. Defaults to AWS SDK default.", nullable = true)
	String stsEndpoint;

	@JsonProperty(value = "bucketVersioning", defaultValue = "true", required = true)
	@Schema(description = "Enable bucket versioning upon bucket creation", defaultValue = "true", required = true)
	Boolean bucketVersioning = true;

	@JsonProperty(value = "bucketAcceleration", defaultValue = "true")
	@Schema(description = "Enable bucket versioning upon bucket creation", defaultValue = "true")
	Boolean bucketAcceleration = true;

	@JsonProperty(value = "bucketEncryption", required = true)
	@Schema(description = "Enable bucket versioning upon bucket creation", required = true)
	S3_SERVERSIDE_ENCRYPTION bucketEncryption = S3_SERVERSIDE_ENCRYPTION.NONE;

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@JsonProperty(value = "stsRoleArn", required = true)
	@Schema(description = "roleArn to for STS AssumeRoleWithWebIdentity (AWS and MinIO)", example = "arn:aws:iam::930717317329:role/cipherduck_chain_01")
	String stsRoleArn;

	@JsonProperty(value = "stsRoleArn2")
	@Schema(description = "roleArn to assume for STS AssumeRole in role chaining (AWS only, not MinIO)", example = "arn:aws:iam::930717317329:role/cipherduck_chain_02", nullable = true)
	String stsRoleArn2;


	@JsonProperty(value = "stsDurationSeconds", required = false)
	@Schema(description = "Token lifetime for STS tokens assumed. Defaults to AWS/MinIO defaults", nullable = true)
	Integer stsDurationSeconds;

	public StorageProfileS3STSDto() {
		// jackson
	}

	public StorageProfileS3STSDto(final UUID id, final String name, final Protocol protocol, final boolean archived, final String scheme, final String hostname, final Integer port, final boolean withPathStyleAccessEnabled, final S3_STORAGE_CLASSES storageClass, final String region, final List<String> regions, final String bucketPrefix, final String stsRoleArnClient, final String stsRoleArnHub, final String stsEndpoint, final boolean bucketVersioning, final Boolean bucketAcceleration, final S3_SERVERSIDE_ENCRYPTION bucketEncryption, final String stsRoleArn, final String stsRoleArn2, final Integer stsDurationSeconds) {
		super(id, name, protocol, archived, scheme, hostname, port, withPathStyleAccessEnabled, storageClass);
		this.region = region;
		this.regions = regions;
		this.bucketPrefix = bucketPrefix;
		this.stsRoleArnClient = stsRoleArnClient;
		this.stsRoleArnHub = stsRoleArnHub;
		this.stsEndpoint = stsEndpoint;
		this.bucketVersioning = bucketVersioning;
		this.bucketAcceleration = bucketAcceleration;
		this.bucketEncryption = bucketEncryption;
		this.stsRoleArn = stsRoleArn;
		this.stsRoleArn2 = stsRoleArn2;
		this.stsDurationSeconds = stsDurationSeconds;
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
				storageProfile.stsRoleArnClient,
				storageProfile.stsRoleArnHub,
				storageProfile.stsEndpoint,
				storageProfile.bucketVersioning,
				storageProfile.bucketAcceleration,
				S3_SERVERSIDE_ENCRYPTION.valueOf(storageProfile.bucketEncryption),
				storageProfile.stsRoleArn,
				storageProfile.stsRoleArn2,
				storageProfile.stsDurationSeconds
		);
	}

	public StorageProfileS3STS toEntity() {
		final StorageProfileS3STS storageProfile = new StorageProfileS3STS();
		storageProfile.id = this.id;
		storageProfile.name = this.name;
		storageProfile.archived = this.archived;
		storageProfile.scheme = this.scheme;
		storageProfile.hostname = this.hostname;
		storageProfile.port = this.port;
		storageProfile.withPathStyleAccessEnabled = this.withPathStyleAccessEnabled;
		storageProfile.storageClass = this.storageClass.toString();
		storageProfile.region = this.region;
		storageProfile.regions = this.regions;
		storageProfile.bucketPrefix = this.bucketPrefix;
		storageProfile.stsRoleArnClient = this.stsRoleArnClient;
		storageProfile.stsRoleArnHub = this.stsRoleArnHub;
		storageProfile.stsEndpoint = this.stsEndpoint;
		storageProfile.bucketVersioning = this.bucketVersioning;
		storageProfile.bucketAcceleration = this.bucketAcceleration;
		storageProfile.bucketEncryption = this.bucketEncryption.name();
		storageProfile.stsRoleArn = this.stsRoleArn;
		storageProfile.stsRoleArn2 = this.stsRoleArn2;
		storageProfile.stsDurationSeconds = this.stsDurationSeconds;
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

	public String stsRoleArnClient() {
		return stsRoleArnClient;
	}

	public String stsRoleArnHub() {
		return stsRoleArnHub;
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

	public String stsRoleArn() {
		return stsRoleArn;
	}

	public String stsRoleArn2() {
		return stsRoleArn2;
	}

	public Integer stsDurationSeconds() {
		return stsDurationSeconds;
	}
}
