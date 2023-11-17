package org.cryptomator.hub.api.cipherduck;

import java.util.Optional;

public record VaultJWEBackendDto(

		// (1) bookmark properties -> 7
		Optional<String> protocol,
		Optional<String> provider,
		Optional<String> hostname,
		Optional<Integer> port,
		Optional<String> defaultPath,
		Optional<String> nickname,
		Optional<String> uuid,


		// (2) protocol settings (go into bookmark's custom properties) -> 5
		Optional<String> oauthClientId,
		Optional<String> oauthTokenUrl,
		Optional<String> oauthAuthorizationUrl,
		Optional<String> stsEndpoint,
		Optional<String> region,

		// (3) boookmark custom properties -> 5
		Optional<String> stsRoleArn,

		Optional<String> stsRoleArn2,

		Optional<Integer> stsDurationSeconds,

		Optional<String> parentUUID,

		Optional<String> oAuthTokenExchangeAudience,


		// (4) keychain credentials -> 2
		Optional<String> username,

		Optional<String> password,

		// (5) misc -> 1
		Optional<Boolean> automaticAccessGrant
) implements VaultJWEBackend {


	public VaultJWEBackendDto(VaultJWEBackend s, final Optional<String> oAuthAuthorizationUrl, final Optional<String> oAuthTokenUrl, final Optional<String> oAuthClientId, final Optional<String> oAuthTokenExchangeAudience, final String hubId) {
		this(
				s.protocol(), s.provider(), s.hostname(), s.port(), s.defaultPath(), s.nickname(), s.uuid(), oAuthClientId, oAuthTokenUrl, oAuthAuthorizationUrl, s.stsEndpoint(), s.region(),
				s.stsRoleArn(),
				s.stsRoleArn2(),
				s.stsDurationSeconds(),
				Optional.of(hubId),
				oAuthTokenExchangeAudience,
				s.username(),
				s.password(),
				s.automaticAccessGrant()
		);
	}
}
