package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class StorageDto {
    @JsonProperty("s3type")
    public final String s3type;
    @JsonProperty("scheme")
    public final String scheme;
    @JsonProperty("hostname")
    public final String hostname;
    @JsonProperty("port")
    public final int port;
    @JsonProperty("accessKeyId")
    public final String accessKeyId;
    @JsonProperty("secretKey")
    public final String secretKey;
    @JsonProperty("vaultId")
    public final String vaultId;
    @JsonProperty("vaultConfigToken")
    public final String vaultConfigToken;
    @JsonProperty("rootDirHash")
    public final String rootDirHash;


    public StorageDto(
            @JsonProperty("s3type") String s3type,
            @JsonProperty("scheme") String scheme,
            @JsonProperty("hostname") String hostname,
            @JsonProperty("port") int port,
            @JsonProperty("accessKeyId") String accessKeyId,
            @JsonProperty("secretKey") String secretKey,
            @JsonProperty("vaultId") String vaultId,
            @JsonProperty("vaultConfigToken") String vaultConfigToken,
            @JsonProperty("rootDirHash") String rootDirHash
    ) {
        this.s3type = s3type;
        this.scheme = scheme;
        this.hostname = hostname;
        this.port = port;
        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
        this.vaultId = vaultId;
        this.vaultConfigToken = vaultConfigToken;
        this.rootDirHash = rootDirHash;
    }
}
