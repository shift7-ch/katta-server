package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface VaultJWEBackend {
    @JsonProperty("protocol")
    String protocol();

    @JsonProperty("vendor")
    Optional<String> vendor();

    @JsonProperty("hostname")
    Optional<String> hostname();

    @JsonProperty("scheme")
    Optional<String> scheme();

    @JsonProperty("port")
    Optional<Integer> port();

    // TODO https://github.com/chenkins/cipherduck-hub/issues/3 for permanent credentials?
    //    @JsonProperty("username")
    //    String username();
    //
    //    @JsonProperty("password")
    //    String password();

    @JsonProperty("region")
    Optional<String> region();

    @JsonProperty("stsEndpoint")
    Optional<String> stsEndpoint();

    @JsonProperty("stsRoleArn")
    Optional<String> stsRoleArn();

    @JsonProperty("stsDurationSeconds")
    Optional<Integer> stsDurationSeconds();

    @JsonProperty("authorization")
    Optional<String> authorization();

    @JsonProperty("oAuthAuthorizationUrl")
    Optional<String> oauthAuthorizationUrl();

    @JsonProperty("oAuthTokenUrl")
    Optional<String> oauthTokenUrl();

    @JsonProperty("oAuthClientId")
    Optional<String> oauthClientId();

    @JsonProperty("oAuthRedirectUrl")
    Optional<String> oauthRedirectUrl();

    @JsonProperty("usernameConfigurable")
    Optional<String> usernameConfigurable();

    @JsonProperty("passwordConfigurable")
    Optional<String> passwordConfigurable();

    @JsonProperty("tokenConfigurable")
    Optional<String> tokenConfigurable();
}
