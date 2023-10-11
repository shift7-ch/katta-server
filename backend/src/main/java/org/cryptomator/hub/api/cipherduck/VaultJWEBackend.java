package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface VaultJWEBackend {


    // (1) protocol
    // (1a) protocol storage-specific
    @JsonProperty("protocol")
    Optional<String> protocol();

    @JsonProperty("vendor")
    Optional<String> vendor();

    @JsonProperty("region")
    Optional<String> region();

    @JsonProperty("stsEndpoint")
    Optional<String> stsEndpoint();

    @JsonProperty("scheme")
    Optional<String> scheme();

    // (1b) protocol hub-specific
    @JsonProperty("oAuthAuthorizationUrl")
    Optional<String> oauthAuthorizationUrl();

    @JsonProperty("oAuthTokenUrl")
    Optional<String> oauthTokenUrl();

    @JsonProperty("oAuthClientId")
    Optional<String> oauthClientId();


    // (1c) protocol hub-independent
    // TODO https://github.com/chenkins/cipherduck-hub/issues/4 we could hard-code it in client?
    @JsonProperty("authorization")
    Optional<String> authorization();

    @JsonProperty("oAuthRedirectUrl")
    Optional<String> oauthRedirectUrl();

    @JsonProperty("usernameConfigurable")
    Optional<String> usernameConfigurable();

    @JsonProperty("passwordConfigurable")
    Optional<String> passwordConfigurable();

    @JsonProperty("tokenConfigurable")
    Optional<String> tokenConfigurable();


    // (2) bookmark aka. Host
    // (2a) bookmark direct fields
    // TODO https://github.com/chenkins/cipherduck-hub/issues/4 if we inject protocol dynamically, could also go to protocol
    @JsonProperty("hostname")
    Optional<String> hostname();

    @JsonProperty("port")
    Optional<Integer> port();

    @JsonProperty("defaultPath")
    Optional<String> defaultPath();

    // TODO https://github.com/chenkins/cipherduck-hub/issues/17 for permanent credentials?
    //    @JsonProperty("username")
    //    String username();
    //
    //    @JsonProperty("password")
    //    String password();

    // (2b) boookmark custom properties
    @JsonProperty("stsRoleArn")
    Optional<String> stsRoleArn();

    @JsonProperty("stsRoleArn2")
    Optional<String> stsRoleArn2();

    @JsonProperty("stsDurationSeconds")
    Optional<Integer> stsDurationSeconds();
}
