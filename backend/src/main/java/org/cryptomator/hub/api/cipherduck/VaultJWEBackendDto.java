package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record VaultJWEBackendDto(

		// (1) protocol
		// (1a) protocol hub-independent
		Optional<String> authorization,


		Optional<String> oauthRedirectUrl,

		Optional<String> usernameConfigurable,

		Optional<String> passwordConfigurable,

		Optional<String> tokenConfigurable,


		// (1b) protocol hub-specific
		Optional<String> oauthAuthorizationUrl,

		@JsonProperty("oAuthTokenUrl")
		Optional<String> oauthTokenUrl,

		Optional<String> oauthClientId,


		// (1c) protocol storage-specific
		Optional<String> protocol,

		Optional<String> vendor,

		Optional<String> region,

		Optional<String> stsEndpoint,

		Optional<String> scheme,


		// (2) bookmark aka. Host
		// (2a) bookmark direct fields
		Optional<String> hostname,

		Optional<Integer> port,

		Optional<String> defaultPath,

		Optional<String> nickname,

		Optional<String> uuid,


		// (2b) boookmark custom properties
		Optional<String> stsRoleArn,

		Optional<String> stsRoleArn2,

		Optional<Integer> stsDurationSeconds,

		Optional<String> parentUUID,

		Optional<String> oAuthTokenExchangeAudience,


		// (3) keychain credentials
		Optional<String> username,

		Optional<String> password,

		// (4) misc
		Optional<Boolean> automaticAccessGrant
) implements VaultJWEBackend {


	public VaultJWEBackendDto(VaultJWEBackend s, final String oAuthAuthorizationUrl, final String oAuthTokenUrl, final String oAuthClientId, final String oAuthTokenExchangeAudience, final String hubId) {
		this(s.authorization(),
				s.oauthRedirectUrl(),
				s.usernameConfigurable(),
				s.passwordConfigurable(),
				s.tokenConfigurable(),
				Optional.of(oAuthAuthorizationUrl),
				Optional.of(oAuthTokenUrl),
				Optional.of(oAuthClientId),
				s.protocol(),
				s.vendor(),
				s.region(),
				s.stsEndpoint(),
				s.scheme(),
				s.hostname(),
				s.port(),
				s.defaultPath(),
				s.nickname(),
				s.uuid(),
				s.stsRoleArn(),
				s.stsRoleArn2(),
				s.stsDurationSeconds(),
				Optional.of(hubId),
				Optional.of(oAuthTokenExchangeAudience),
				s.username(),
				s.password(),
				s.automaticAccessGrant()
				);
	}
}
