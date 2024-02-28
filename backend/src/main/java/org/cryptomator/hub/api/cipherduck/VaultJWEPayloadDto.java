package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VaultJWEPayloadDto(

		@JsonProperty(value = "key", required = true)
		// masterkey
		String key,

		@JsonProperty(value = "storage", required = true)
		VaultJWEBackendDto backend,

		@JsonProperty(value = "automaticAccessGrant", required = true)
		AutomaticAccessGrant automaticAccessGrant
) {
}
