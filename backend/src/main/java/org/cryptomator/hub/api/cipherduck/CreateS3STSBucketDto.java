package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CreateS3STSBucketDto(
		@JsonProperty("vaultId")
		String vaultId,
		@JsonProperty("storageConfigId")
		UUID storageConfigId,
		@JsonProperty("vaultUvf")
		String vaultUvf,
		@JsonProperty("rootDirHash")
		String rootDirHash,
		@JsonProperty("awsAccessKey")
		String awsAccessKey,
		@JsonProperty("awsSecretKey")
		String awsSecretKey,
		@JsonProperty("sessionToken")
		String sessionToken,
		@JsonProperty("region")
		String region
) {

}

