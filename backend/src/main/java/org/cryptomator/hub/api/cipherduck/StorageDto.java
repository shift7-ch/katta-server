package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageDto(
        @JsonProperty("protocol")
        String protocol,
        @JsonProperty("s3type")
        String s3type,
        @JsonProperty("scheme")
        String scheme,
        @JsonProperty("hostname")
        String hostname,
        @JsonProperty("port")
        int port,
        @JsonProperty("oidcProvider")
        String oidcProvider,
        @JsonProperty("region")
        String region,
        @JsonProperty("vaultId")
        String vaultId,
        @JsonProperty("bucketName")
        String bucketName,
        @JsonProperty("vaultConfigToken")
        String vaultConfigToken,
        @JsonProperty("rootDirHash")
        String rootDirHash

) {

}

