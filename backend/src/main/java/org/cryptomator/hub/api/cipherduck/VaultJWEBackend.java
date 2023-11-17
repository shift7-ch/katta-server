package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface VaultJWEBackend {
	// (1) bookmark properties -> 7
	@JsonProperty("protocol")
	Optional<String> protocol();

	@JsonProperty("provider")
	Optional<String> provider();

	@JsonProperty("hostname")
	Optional<String> hostname();

	@JsonProperty("port")
	Optional<Integer> port();

	@JsonProperty("defaultPath")
	Optional<String> defaultPath();

	@JsonProperty("nickname")
	Optional<String> nickname();

	@JsonProperty("uuid")
	Optional<String> uuid();

	// (2) protocol settings (go into bookmark's custom properties) -> 5
	@JsonProperty("oAuthAuthorizationUrl")
	Optional<String> oauthAuthorizationUrl();

	@JsonProperty("oAuthTokenUrl")
	Optional<String> oauthTokenUrl();

	@JsonProperty("oAuthClientId")
	Optional<String> oauthClientId();

	@JsonProperty("stsEndpoint")
	Optional<String> stsEndpoint();

	@JsonProperty("region")
	Optional<String> region();

	// (3) boookmark custom properties -> 5
	@JsonProperty("stsRoleArn")
	Optional<String> stsRoleArn();

	@JsonProperty("stsRoleArn2")
	Optional<String> stsRoleArn2();

	@JsonProperty("stsDurationSeconds")
	Optional<Integer> stsDurationSeconds();

	@JsonProperty("parentUUID")
	Optional<String> parentUUID();

	@JsonProperty("oAuthTokenExchangeAudience")
	Optional<String> oAuthTokenExchangeAudience();

	// (4) keychain credentials -> 2
	@JsonProperty("username")
	Optional<String> username();

	@JsonProperty("password")
	Optional<String> password();

	// (5) misc -> 1
	@JsonProperty("automaticAccessGrant")
	Optional<Boolean> automaticAccessGrant();
}
