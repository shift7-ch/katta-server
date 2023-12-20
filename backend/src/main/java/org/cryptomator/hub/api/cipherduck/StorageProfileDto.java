package org.cryptomator.hub.api.cipherduck;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "storage_profile")
public class StorageProfileDto extends PanacheEntityBase {
	public enum Protocol {
		s3("s3"),
		s3sts("s3-sts");
		private final String protocol;

		private Protocol(final String protocol) {
			this.protocol = protocol;
		}

		@JsonValue
		public String getProtocol() {
			return protocol;
		}

	}


	@Id
	@Column(name = "id", nullable = false)
	@JsonProperty(value = "id", required = true)
	UUID id; // clients will use this as vendor in profile and provider in vault bookmark

	@JsonProperty(value = "name", required = true)
	String name;

	// (1) bucket creation, template upload and client profile
	@JsonProperty("scheme")
	String scheme; // defaults to AWS

	@JsonProperty("hostname")
	String hostname;  // defaults to AWS

	@JsonProperty("port")
	Integer port; // defaults to AWS

	@JsonProperty(value = "region")
	String region = "us-east-1";  // default region selected in the frontend/client

	@JsonProperty(value = "regions")
	List<String> regions = Arrays.stream(Regions.values()).map(r -> r.getName()).toList(); // defaults to full AWS list

	@JsonProperty(value = "withPathStyleAccessEnabled")
	Boolean withPathStyleAccessEnabled = false;


	// (2) bucket creation only (i.e. STS-case)
	@JsonProperty(value = "bucketPrefix")
	String bucketPrefix;

	@JsonProperty("stsRoleArnClient")
	String stsRoleArnClient;

	@JsonProperty("stsRoleArnHub")
	String stsRoleArnHub;

	@JsonProperty("stsEndpoint")
	String stsEndpoint; // defaults to AWS


	// (3) client profile
	// (3a) client profile attributes
	@JsonProperty(value = "protocol")
	Protocol protocol = Protocol.s3sts;
	@JsonProperty(value = "oauthClientId")
	String oauthClientId; // injected from hub config if STS

	@JsonProperty(value = "oauthTokenUrl")
	String oauthTokenUrl; // injected from hub config if STS

	@JsonProperty(value = "oauthAuthorizationUrl")
	String oauthAuthorizationUrl; // injected from hub config if STS


	// (3b) client profile custom properties
	@JsonProperty(value = "stsRoleArn")
	String stsRoleArn; // token exchange

	@JsonProperty(value = "stsRoleArn2")
	String stsRoleArn2; // role chaining AWS STS

	@JsonProperty(value = "stsDurationSeconds")
	Integer stsDurationSeconds = 900;

	@JsonProperty(value = "oAuthTokenExchangeAudience")
	String oAuthTokenExchangeAudience; // injected from hub config


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


	public String oauthClientId() {
		return oauthClientId;
	}

	public StorageProfileDto withOauthClientId(String oauthClientId) {
		this.oauthClientId = oauthClientId;
		return this;
	}

	public String oauthTokenUrl() {
		return oauthTokenUrl;
	}

	public StorageProfileDto withOauthTokenUrl(String oauthTokenUrl) {
		this.oauthTokenUrl = oauthTokenUrl;
		return this;
	}

	public String oauthAuthorizationUrl() {
		return oauthAuthorizationUrl;
	}

	public StorageProfileDto withOauthAuthorizationUrl(String oauthAuthorizationUrl) {
		this.oauthAuthorizationUrl = oauthAuthorizationUrl;
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

	public String oAuthTokenExchangeAudience() {
		return oAuthTokenExchangeAudience;
	}

	public StorageProfileDto withoAuthTokenExchangeAudience(String oAuthTokenExchangeAudience) {
		this.oAuthTokenExchangeAudience = oAuthTokenExchangeAudience;
		return this;
	}
}
