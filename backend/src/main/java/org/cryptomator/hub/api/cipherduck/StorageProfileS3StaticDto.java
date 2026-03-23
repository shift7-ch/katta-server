package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3Static;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import software.amazon.awssdk.regions.Region;

import java.util.List;
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

	@JsonProperty(value = "scheme", defaultValue = "https")
	@Schema(description = "Scheme of S3 endpoint for template upload/bucket creation. Defaults to default for protocol, i.e. https in most cases.", example = "https", nullable = true)
	String scheme;

	@JsonProperty("hostname")
	@Schema(description = "Hostname S3 endpoint for template upload/bucket creation. Defaults to AWS SDK default.", example = "s3-us-gov-west-1.amazonaws.com", nullable = true)
	String hostname;

	@JsonProperty("port")
	@Schema(description = "Port S3 endpoint for template upload/bucket creation. Defaults to default port for scheme.", example = "443", nullable = true)
	Integer port;

	@JsonProperty(value = "withPathStyleAccessEnabled")
	@Schema(description = "Whether to use path style for S3 endpoint for template upload/bucket creation.", example = "false", defaultValue = "false")
	Boolean withPathStyleAccessEnabled = false;

	@JsonProperty(value = "storageClass", defaultValue = "STANDARD")
	@Schema(description = "Storage class for upload. Defaults to STANDARD", example = "STANDARD", required = true)
	S3_STORAGE_CLASSES storageClass = S3_STORAGE_CLASSES.STANDARD;

	public enum S3_SERVERSIDE_ENCRYPTION {
		NONE, SSE_AES256, SSE_KMS_DEFAULT
	}

	//======================================================================
	// (2) STS only: bucket creation  (only relevant for Desktop client)
	//======================================================================
	@JsonProperty(value = "region", required = true, defaultValue = "us-east-1")
	@Schema(description = "Default region selected in the frontend/client to create bucket in.", example = "us-east-1", defaultValue = "us-east-1")
	String region = "us-east-1";

	@JsonProperty(value = "regions", required = true)
	@Schema(description = "List of selectable regions in the frontend/client to create bucket in. Defaults to full list from AWS SDK.")
	List<String> regions = Region.regions().stream().map(Region::id).toList();

	@JsonProperty(value = "bucketPrefix", required = true)
	@Schema(description = "Buckets are created with name <bucket prefix><vault UUID>.", example = "cipherduck")
	String bucketPrefix;

	@JsonProperty(value = "stsRoleCreateBucketClient", required = true)
	@Schema(description = "STS role for clients to assume to create buckets. Will be the same as stsRoleCreateBucketHub for AWS, different for MinIO.", example = "arn:aws:iam::<ACCOUNT ID>:role/cipherduck-createbucket")
	String stsRoleCreateBucketClient;

	@JsonProperty(value = "stsRoleCreateBucketHub", required = true)
	@Schema(description = "STS role for frontend to assume to create buckets (used with inline policy and passed to hub storage). Will be the same as stsRoleCreateBucketClient for AWS, different for MinIO.", example = "arn:aws:iam::<ACCOUNT ID>:role/cipherduck-createbucket")
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

	public StorageProfileS3StaticDto() {
		// jackson
	}

	public StorageProfileS3StaticDto(final UUID id, final String name, final Protocol protocol, final boolean archived, final String scheme, final String hostname, final Integer port, final boolean withPathStyleAccessEnabled, final S3_STORAGE_CLASSES storageClass, final String region, final List<String> regions, final String bucketPrefix, final String stsRoleCreateBucketClient, final String stsRoleCreateBucketHub, final String stsEndpoint, final boolean bucketVersioning, final Boolean bucketAcceleration, final S3_SERVERSIDE_ENCRYPTION bucketEncryption) {
		super(id, name, protocol, archived);
		this.scheme = scheme;
		this.hostname = hostname;
		this.port = port;
		this.withPathStyleAccessEnabled = withPathStyleAccessEnabled;
		this.storageClass = storageClass;

		this.region = region;
		this.regions = regions;
		this.bucketPrefix = bucketPrefix;
		this.stsRoleCreateBucketClient = stsRoleCreateBucketClient;
		this.stsRoleCreateBucketHub = stsRoleCreateBucketHub;
		this.stsEndpoint = stsEndpoint;
		this.bucketVersioning = bucketVersioning;
		this.bucketAcceleration = bucketAcceleration;
		this.bucketEncryption = bucketEncryption;

	}

	static StorageProfileS3StaticDto fromEntity(final StorageProfileS3Static storageProfile) {
		return new StorageProfileS3StaticDto(storageProfile.id, storageProfile.name, Protocol.s3static, storageProfile.archived, storageProfile.scheme, storageProfile.hostname, storageProfile.port, storageProfile.withPathStyleAccessEnabled, S3_STORAGE_CLASSES.valueOf(storageProfile.storageClass), storageProfile.region, storageProfile.regions, storageProfile.bucketPrefix, storageProfile.stsRoleCreateBucketClient, storageProfile.stsRoleCreateBucketHub, storageProfile.stsEndpoint, storageProfile.bucketVersioning, storageProfile.bucketAcceleration, S3_SERVERSIDE_ENCRYPTION.valueOf(storageProfile.bucketEncryption));
	}

	public StorageProfileS3Static toEntity() {
		final StorageProfileS3Static storageProfile = new StorageProfileS3Static();
		storageProfile.id = this.id;
		storageProfile.name = this.name;
		storageProfile.protocol = this.protocol;
		storageProfile.archived = this.archived;
		storageProfile.scheme = this.scheme;
		storageProfile.hostname = this.hostname;
		storageProfile.port = this.port;
		storageProfile.withPathStyleAccessEnabled = this.withPathStyleAccessEnabled;
		storageProfile.storageClass = this.storageClass.name();
		storageProfile.region = this.region;
		storageProfile.regions = this.regions;
		storageProfile.bucketPrefix = this.bucketPrefix;
		storageProfile.stsRoleCreateBucketClient = this.stsRoleCreateBucketClient;
		storageProfile.stsRoleCreateBucketHub = this.stsRoleCreateBucketHub;
		storageProfile.stsEndpoint = this.stsEndpoint;
		storageProfile.bucketVersioning = this.bucketVersioning;
		storageProfile.bucketAcceleration = this.bucketAcceleration;
		storageProfile.bucketEncryption = this.bucketEncryption.name();
		return storageProfile;
	}

	public String scheme() {
		return scheme;
	}

	public String hostname() {
		return hostname;
	}

	public Integer port() {
		return port;
	}

	public Boolean withPathStyleAccessEnabled() {
		return withPathStyleAccessEnabled;
	}

	public S3_STORAGE_CLASSES storageClass() {
		return storageClass;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StorageProfileS3StaticDto that = (StorageProfileS3StaticDto) o;
		return Objects.equals(scheme, that.scheme) && Objects.equals(hostname, that.hostname) && Objects.equals(port, that.port) && Objects.equals(withPathStyleAccessEnabled, that.withPathStyleAccessEnabled) && storageClass == that.storageClass && Objects.equals(region, that.region) && Objects.equals(regions, that.regions) && Objects.equals(bucketPrefix, that.bucketPrefix) && Objects.equals(stsRoleCreateBucketClient, that.stsRoleCreateBucketClient) && Objects.equals(stsRoleCreateBucketHub, that.stsRoleCreateBucketHub) && Objects.equals(stsEndpoint, that.stsEndpoint) && Objects.equals(bucketVersioning, that.bucketVersioning) && Objects.equals(bucketAcceleration, that.bucketAcceleration) && bucketEncryption == that.bucketEncryption;
	}

	@Override
	public int hashCode() {
		return Objects.hash(scheme, hostname, port, withPathStyleAccessEnabled, storageClass, region, regions, bucketPrefix, stsRoleCreateBucketClient, stsRoleCreateBucketHub, stsEndpoint, bucketVersioning, bucketAcceleration, bucketEncryption);
	}
}
