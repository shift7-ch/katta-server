package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record CreateS3STSBucketDto(
		@JsonProperty("vaultId")
		String vaultId,
		@JsonProperty("storageConfigId")
		UUID storageConfigId,
		@JsonProperty("vaultUvf")
		String vaultUvf,
		@JsonProperty("dirUvf")
		String dirUvf,
		@JsonProperty("rootDirHash")
		String rootDirHash,
		@JsonProperty("awsAccessKey")
		String awsAccessKey,
		@JsonProperty("awsSecretKey")
		String awsSecretKey,
		@JsonProperty("sessionToken")
		String sessionToken,
		@JsonProperty("region")
		@Nullable String region
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

