package org.cryptomator.hub.api.cipherduck;


import java.util.Optional;

public record VaultJweBackendDto(
		// (1) protocol
		// (1a) protocol storage-specific
		Optional<String> protocol,
		Optional<String> vendor,
		Optional<String> region,
		Optional<String> stsEndpoint,
		Optional<String> scheme,

		// (1b) protocol hub-specific
		Optional<String> oauthAuthorizationUrl,
		Optional<String> oauthTokenUrl,
		Optional<String> oauthClientId,

		// (1c) protocol hub-independent
		Optional<String> authorization,
		Optional<String> oauthRedirectUrl,
		Optional<String> usernameConfigurable,
		Optional<String> passwordConfigurable,
		Optional<String> tokenConfigurable,

		// (2) bookmark aka. Host
		// (2a) bookmark direct fields
		Optional<String> hostname,
		Optional<Integer> port,
		Optional<String> defaultPath,

		// (2b) boookmark custom properties
		Optional<String> stsRoleArn,
		Optional<String> stsRoleArn2,
		Optional<Integer> stsDurationSeconds


) implements VaultJWEBackend {
}