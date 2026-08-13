package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateS3STSBucketDto(
		@JsonProperty("vaultId")
		@NotNull String vaultId,
		@JsonProperty("storageConfigId")
		@NotNull UUID storageConfigId,
		@JsonProperty("vaultUvf")
		@NotNull String vaultUvf,
		@JsonProperty("dirUvf")
		@NotNull String dirUvf,
		@JsonProperty("rootDirHash")
		@NotNull String rootDirHash,
		@JsonProperty("awsAccessKey")
		@NotNull String awsAccessKey,
		@JsonProperty("awsSecretKey")
		@NotNull String awsSecretKey,
		@JsonProperty("sessionToken")
		@NotNull String sessionToken,
		@JsonProperty("region")
		@NotNull String region
) {

	@Override
	public String toString() {
		return "CreateS3STSBucketDto{" +
				"vaultId='" + vaultId + '\'' +
				", storageConfigId=" + storageConfigId +
				", vaultUvf='" + vaultUvf + '\'' +
				", dirUvf='" + dirUvf + '\'' +
				", rootDirHash='" + rootDirHash + '\'' +
				", sessionToken='" + sessionToken + '\'' +
				", region='" + region + '\'' +
				", awsAccessKey=***" +
				", awsSecretKey=***" +
				'}';
	}
}

