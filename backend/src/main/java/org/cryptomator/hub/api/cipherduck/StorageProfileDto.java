package org.cryptomator.hub.api.cipherduck;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// pro-memoria @Schema
// - "required" is taken from @JSONProperty
// - "defaultValue" needs to be repeated
@Entity
@Table(name = "storage_profile")
public class StorageProfileDto extends PanacheEntityBase {
	public static enum Protocol {
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

	//======================================================================
	// (1) STS and permanent:
	// - metadata (STS and permanent)
	// - bucket creation frontend/desktop client (STS)
	// - template upload (STS and permanent)
	// - client profile (STS and permanent)
	//======================================================================

	@Id
	@Column(name = "id", nullable = false)
	@JsonProperty(value = "id", required = true)
	@Schema(description = "Technical identifier for a storage profile. Must be unique UUID. Clients will use this as vendor in profile and provider in vault bookmark")
	UUID id;

	@JsonProperty(value = "name", required = true)
	@Schema(description = "Displayed when choosing type of a new vault in dropdown.")
	String name;

	@JsonProperty(value = "scheme", defaultValue = "https")
	@Schema(description = "Scheme of S3 endpoint for template upload/bucket creation.", example = "https", defaultValue = "null (=default for protocol, i.e. https in most cases)")
	String scheme;

	@JsonProperty("hostname")
	@Schema(description = "Hostname S3 endpoint for template upload/bucket creation.", example = "s3-us-gov-west-1.amazonaws.com", defaultValue = "null (=AWS SDK default)")
	String hostname;

	@JsonProperty("port")
	@Schema(description = "Port S3 endpoint for template upload/bucket creation.", example = "443", defaultValue = "null (=default port for scheme)")
	Integer port;

	@JsonProperty(value = "withPathStyleAccessEnabled")
	@Schema(description = "Whether to use path style for S3 endpoint for template upload/bucket creation.", example = "false", defaultValue = "false")
	Boolean withPathStyleAccessEnabled = false;


	//======================================================================
	// (3) client profile
	//======================================================================

	//----------------------------------------------------------------------
	// (3a) STS and permanent client profile attributes
	//----------------------------------------------------------------------
	@JsonProperty(value = "protocol", required = true)
	@Schema(description = "Storage protocol: s3-hub (permanent credentials) or s3-hub-sts (STS). Defaults to s3-hub-sts.")
	Protocol protocol = Protocol.s3sts;

	//======================================================================
	// (2) STS only: bucket creation
	//======================================================================
	@JsonProperty(value = "region", required = true, defaultValue = "us-east-1")
	@Schema(description = "Default region selected in the frontend/client to create bucket in.", example = "443", defaultValue = "us-east-1")
	String region = "us-east-1";

	@JsonProperty(value = "regions", required = true)
	@Schema(description = "List of selectable regions in the frontend/client to create bucket in. Defaults to full list from AWS SDK.")
	List<String> regions = Arrays.stream(Regions.values()).map(r -> r.getName()).toList();

	@JsonProperty(value = "bucketPrefix", required = true)
	@Schema(description = "Buckets are create with name <bucket prefix><vault UUID>.", example = "cipherduck")
	String bucketPrefix;

	@JsonProperty(value = "stsRoleArnClient", required = true)
	@Schema(description = "STS role for clients to assume to create buckets. Will be the same as stsRoleArnHub for AWS, different for MinIO.", example = "arn:aws:iam::<ACCOUNT ID>:role/cipherduck-createbucket")
	String stsRoleArnClient;

	@JsonProperty(value = "stsRoleArnHub", required = true)
	@Schema(description = "STS role for frontend to assume to create buckets (used with inline policy and passed to hub backend). Will be the same as stsRoleArnClient for AWS, different for MinIO.", example = "arn:aws:iam::<ACCOUNT ID>:role/cipherduck-createbucket")
	String stsRoleArnHub;

	@JsonProperty("stsEndpoint")
	@Schema(description = "STS endpoint to use for AssumeRoleWithWebIdentity and AssumeRole for getting a temporary access token passed to the backend", defaultValue = "null (=AWS SDK default)")
	String stsEndpoint = null;

	@JsonProperty(value = "bucketVersioning", defaultValue = "true")
	@Schema(description = "Enable bucket versioning upon bucket creation", defaultValue = "true")
	Boolean bucketVersioning = true;

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@JsonProperty(value = "stsRoleArn", required = true)
	@Schema(description = "roleArn to for STS AssumeRoleWithWebIdentity (AWS and MinIO)", example = "arn:aws:iam::930717317329:role/cipherduck_chain_01")
	String stsRoleArn;

	@JsonProperty(value = "stsRoleArn2", required = false)
	@Schema(description = "roleArn to assume for STS AssumeRole in role chaining (AWS only, not MinIO)", example = "arn:aws:iam::930717317329:role/cipherduck_chain_02")
	String stsRoleArn2;

	@JsonProperty(value = "stsDurationSeconds", required = false)
	@Schema(description = "Token lifetime for STS tokens assumed", defaultValue = "null (=AWS/MinIO defaults)")
	Integer stsDurationSeconds = null;

	public UUID id() {
		return id;
	}

	public StorageProfileDto withId(UUID id) {
		this.id = id;
		return this;
	}

	public String name() {
		return name;
	}

	public StorageProfileDto withName(String name) {
		this.name = name;
		return this;
	}

	public String bucketPrefix() {
		return bucketPrefix;
	}

	public StorageProfileDto withBucketPrefix(String bucketPrefix) {
		this.bucketPrefix = bucketPrefix;
		return this;
	}

	public String stsRoleArnClient() {
		return stsRoleArnClient;
	}

	public StorageProfileDto withStsRoleArnClient(String stsRoleArnClient) {
		this.stsRoleArnClient = stsRoleArnClient;
		return this;
	}

	public String stsRoleArnHub() {
		return stsRoleArnHub;
	}

	public StorageProfileDto withStsRoleArnHub(String stsRoleArnHub) {
		this.stsRoleArnHub = stsRoleArnHub;
		return this;
	}

	public String stsEndpoint() {
		return stsEndpoint;
	}

	public StorageProfileDto withStsEndpoint(String stsEndpoint) {
		this.stsEndpoint = stsEndpoint;
		return this;
	}

	public Boolean bucketVersioning() {
		return bucketVersioning;
	}

	public StorageProfileDto withBucketVersioning(Boolean bucketVersioning) {
		this.bucketVersioning = bucketVersioning;
		return this;
	}

	public String region() {
		return region;
	}

	public StorageProfileDto withRegion(String region) {
		this.region = region;
		return this;
	}

	public List<String> regions() {
		return regions;
	}

	public StorageProfileDto withRegions(List<String> regions) {
		this.regions = regions;
		return this;
	}

	public Boolean withPathStyleAccessEnabled() {
		return withPathStyleAccessEnabled;
	}

	public StorageProfileDto withWithPathStyleAccessEnabled(Boolean withPathStyleAccessEnabled) {
		this.withPathStyleAccessEnabled = withPathStyleAccessEnabled;
		return this;
	}

	public String scheme() {
		return scheme;
	}

	public StorageProfileDto setScheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	public String hostname() {
		return hostname;
	}

	public StorageProfileDto setHostname(String hostname) {
		this.hostname = hostname;
		return this;
	}

	public Integer port() {
		return port;
	}

	public StorageProfileDto setPort(Integer port) {
		this.port = port;
		return this;
	}

	public Protocol protocol() {
		return protocol;
	}

	public StorageProfileDto withProtocol(Protocol protocol) {
		this.protocol = protocol;
		return this;
	}

	public String stsRoleArn() {
		return stsRoleArn;
	}

	public StorageProfileDto withStsRoleArn(String stsRoleArn) {
		this.stsRoleArn = stsRoleArn;
		return this;
	}

	public String stsRoleArn2() {
		return stsRoleArn2;
	}

	public StorageProfileDto withStsRoleArn2(String stsRoleArn2) {
		this.stsRoleArn2 = stsRoleArn2;
		return this;
	}

	public Integer stsDurationSeconds() {
		return stsDurationSeconds;
	}

	public StorageProfileDto withStsDurationSeconds(Integer stsDurationSeconds) {
		this.stsDurationSeconds = stsDurationSeconds;
		return this;
	}
}
