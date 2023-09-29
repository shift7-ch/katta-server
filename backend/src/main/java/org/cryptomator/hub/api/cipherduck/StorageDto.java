package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageDto(
        @JsonProperty("vaultId")
        String vaultId,
        @JsonProperty("storageConfigId")
        String storageConfigId,
        @JsonProperty("vaultConfigToken")
        String vaultConfigToken,
        @JsonProperty("rootDirHash")
        String rootDirHash,
        @JsonProperty("awsAccessKey")
        String awsAccessKey,
        @JsonProperty("awsSecretKey")
        String awsSecretKey,
        @JsonProperty("sessionToken")
        String sessionToken

) {

}

