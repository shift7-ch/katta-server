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


    public StorageDto(String s3type, String scheme, String hostname, int port, String accessKeyId, String secretKey, String vaultId, String vaultConfigToken, String rootDirHash) {
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
