package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StorageProfileDto {
	// TODO auto-generate in DB -
	// clients will use this as vendor in profile and provider in vault bookmark
	@JsonProperty(value = "id", required = true)
			String id;

	@JsonProperty(value = "name", required = true)
	String name;

	@JsonProperty("bucketPrefix")
	String bucketPrefix;

	@JsonProperty("stsRoleArnClient")
	String stsRoleArnClient;

	@JsonProperty("stsRoleArnHub")
	String stsRoleArnHub;

	@JsonProperty("stsEndpoint")
	String stsEndpoint;

	@JsonProperty("region")
	String region;

	@JsonProperty(value = "regions")
	List<String> regions;


	@JsonProperty(value = "withPathStyleAccessEnabled", defaultValue = "false")
	Boolean withPathStyleAccessEnabled;

	@JsonProperty("scheme")
	String scheme;

	@JsonProperty("hostname")
	String hostname;

	@JsonProperty("port")
	Integer port;

	@JsonProperty(value = "protocol", required = true)
	String protocol;



	// (2) protocol withtings (go into bookmark's custom properties) -> 5
	@JsonProperty(value = "oauthClientId")
	String oauthClientId; // injected from hub config

	@JsonProperty(value = "oauthTokenUrl")
	String oauthTokenUrl; // injected from hub config

	@JsonProperty(value = "oauthAuthorizationUrl")
	String oauthAuthorizationUrl; // injected from hub config


	// (3) boookmark custom properties -> 5
	@JsonProperty(value = "stsRoleArn")
	String stsRoleArn; // X

	@JsonProperty(value = "stsRoleArn2")
	String stsRoleArn2; // X

	// TODO default?
	@JsonProperty(value = "stsDurationSeconds")
	Integer stsDurationSeconds; // X

	@JsonProperty(value = "oAuthTokenExchangeAudience")
	String oAuthTokenExchangeAudience; // injected from hub config


	public String id() {
		return id;
	}

	public StorageProfileDto withId(String id) {
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

	public String protocol() {
		return protocol;
	}

	public StorageProfileDto withProtocol(String protocol) {
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
