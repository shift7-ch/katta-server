package org.cryptomator.hub.api.cipherduck;


import java.util.Optional;

public record VaultJweBackendDto(
        String protocol,
        Optional<String> vendor,
        Optional<String> hostname,
        Optional<String> scheme,
        Optional<Integer> port,
        Optional<String> region,
        Optional<String> stsEndpoint,
        Optional<String> stsRoleArn,
        Optional<Integer> stsDurationSeconds,
        Optional<String> authorization,
        Optional<String> oauthAuthorizationUrl,
        Optional<String> oauthTokenUrl,
        Optional<String> oauthClientId,
        Optional<String> oauthRedirectUrl,
        Optional<String> usernameConfigurable,
        Optional<String> passwordConfigurable,
        Optional<String> tokenConfigurable
) implements VaultJWEBackend {
}