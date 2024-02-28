package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VaultMasterkeyJWEDto(
		@JsonProperty(value = "key", required = true)
		// masterkey
		String key
) {
}
