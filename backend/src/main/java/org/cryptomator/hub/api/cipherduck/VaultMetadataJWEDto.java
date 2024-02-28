package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record VaultMetadataJWEDto(
		@JsonProperty(value = "fileFormat", required = true)
		String fileFormat,
		@JsonProperty(value = "nameFormat", required = true)
		String nameFormat,

		@JsonProperty(value = "keys", required = true)
		Map<String, String> keys,

		@JsonProperty(value = "latestFileKey", required = true)
		String latestFileKey,

		@JsonProperty(value = "nameKey", required = true)
		String nameKey,

		@JsonProperty(value = "kdf", required = true)
		String kdf,

		@JsonProperty(value = "com.cipherduck.storage", required = true)
		VaultMetadataJWEStorageDto storage,

		@JsonProperty(value = "org.cryptomator.automaticAccessGrant", required = true)
		VaultMetadataJWEAutomaticAccessGrantDto automaticAccessGrant
) {
}