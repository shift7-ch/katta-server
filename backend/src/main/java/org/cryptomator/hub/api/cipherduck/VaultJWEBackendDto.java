package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of vault JWE specifying the vault bookmark.
 * Allows to create a bookmark in the client referencing the vendor in the storage profiles.
 * This Java record is unused in hub, only its ts counterpart in `backend.ts`.
 * It will used in Cipherduck client in the OpenAPI generator.
 */
public record VaultJWEBackendDto(

		@JsonProperty(value = "provider", required = true)
		// references id in StorageProfileDto (aka. vendor in client profile)
		String provider,
		@JsonProperty(value = "defaultPath", required = true)
		String defaultPath,
		@JsonProperty(value = "nickname", required = true)
		String nickname,
		@JsonProperty(value = "region", required = true)
		String region,

		@JsonProperty(value = "username")
		// for non-STS
		String username,
		@JsonProperty(value = "password")
		// for non-STS
		String password
) {
}