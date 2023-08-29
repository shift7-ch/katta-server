package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageDto(
        @JsonProperty("s3type")
        String s3type,
        @JsonProperty("scheme")
        String scheme,
        @JsonProperty("hostname")
        String hostname,
        @JsonProperty("port")
        int port,
        @JsonProperty("accessKeyId")
        String accessKeyId,
        @JsonProperty("secretKey")
        String secretKey,
        @JsonProperty("vaultId")
        String vaultId,
        @JsonProperty("vaultConfigToken")
        String vaultConfigToken,
        @JsonProperty("rootDirHash")
        String rootDirHash

) {

}

