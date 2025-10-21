package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3STS;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import software.amazon.awssdk.regions.Region;

import java.util.List;
import java.util.UUID;

public final class StorageProfileS3STSDto extends StorageProfileS3Dto {



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
