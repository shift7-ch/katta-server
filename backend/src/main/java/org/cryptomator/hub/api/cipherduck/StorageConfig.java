package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;


public interface StorageConfig {
    @JsonProperty("id")
    String id();

    @JsonProperty("name")
    String name();

    @JsonProperty("bucketPrefix")
    String bucketPrefix();

    @JsonProperty("oidcProvider")
    Optional<String> oidcProvider();

    @JsonProperty("stsRoleArn")
    Optional<String> stsRoleArn();

    @JsonProperty("region")
    Optional<String> region();

    @JsonProperty("jwe")
    VaultJWEBackend jwe();

    @JsonProperty("s3Type")
    String s3Type();

}


