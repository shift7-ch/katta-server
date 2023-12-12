package org.cryptomator.hub.api.cipherduck;

import java.util.Optional;


// TODO we actually don't need it here - only for code generation in client
public record VaultJWEBackendDto(

		// (1) bookmark properties -> 7
		String provider, // references vendor in backend config
		Optional<String> defaultPath,
		Optional<String> nickname,
		Optional<String> uuid,

		// if overridden in bookmark
		Optional<String> region,


		// (4) keychain credentials -> 2
		Optional<String> username,

		Optional<String> password,

		// (5) misc -> 1
		Optional<Boolean> automaticAccessGrant
) {


//	public VaultJWEBackendDto(VaultJWEBackend s, final Optional<String> oAuthAuthorizationUrl, final Optional<String> oAuthTokenUrl, final Optional<String> oAuthClientId, final Optional<String> oAuthTokenExchangeAudience, final String hubId) {
//		this(
//				s.protocol(), s.provider(), s.defaultPath(), s.nickname(), s.uuid(), s.hostname(), s.port(), oAuthClientId, oAuthTokenUrl, oAuthAuthorizationUrl, s.stsEndpoint(), s.region(),
//				s.stsRoleArn(),
//				s.stsRoleArn2(),
//				s.stsDurationSeconds(),
//				Optional.of(hubId),
//				oAuthTokenExchangeAudience,
//				s.username(),
//				s.password(),
//				s.automaticAccessGrant()
//		);
//	}
}
