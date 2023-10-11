package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public interface VaultJWEBackend {


    // TODO https://github.com/chenkins/cipherduck-hub/issues/4 how generic do we need - which can we put into protocols?

    // (1) storage-specific for protocol
    @JsonProperty("region")
    Optional<String> region();

    @JsonProperty("stsEndpoint")
    Optional<String> stsEndpoint();

    @JsonProperty("scheme")
    Optional<String> scheme();


    // (2) hub-specific for protocol
    @JsonProperty("oAuthAuthorizationUrl")
    Optional<String> oauthAuthorizationUrl();

    @JsonProperty("oAuthTokenUrl")
    Optional<String> oauthTokenUrl();

    @JsonProperty("oAuthClientId")
    Optional<String> oauthClientId();


    // (3) hub-independent for protocol
    // TODO https://github.com/chenkins/cipherduck-hub/issues/3 we don't need them here.
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


    // (4) for bookmark
    // TODO https://github.com/chenkins/cipherduck-hub/issues/3 add defaultPath, add UUID? = bucketName

    @JsonProperty("protocol")
    String protocol();

    // TODO https://github.com/chenkins/cipherduck-hub/issues/3 how do we use vendor - if we can inject (1) and (2) on the fly, we can use separate Protocol/Vendor
    @JsonProperty("vendor")
    Optional<String> vendor();

    @JsonProperty("hostname")
    Optional<String> hostname();

    @JsonProperty("port")
    Optional<Integer> port();

    // TODO https://github.com/chenkins/cipherduck-hub/issues/3 for permanent credentials?
    //    @JsonProperty("username")
    //    String username();
    //
    //    @JsonProperty("password")
    //    String password();

    @JsonProperty("stsRoleArn")
    Optional<String> stsRoleArn();

    @JsonProperty("stsRoleArn2")
    Optional<String> stsRoleArn2();

    @JsonProperty("stsDurationSeconds")
    Optional<Integer> stsDurationSeconds();
}
