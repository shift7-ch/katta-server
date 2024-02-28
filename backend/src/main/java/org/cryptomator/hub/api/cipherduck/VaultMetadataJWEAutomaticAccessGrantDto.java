package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VaultMetadataJWEAutomaticAccessGrantDto(
		@JsonProperty(value = "enabled", defaultValue = "true")
		boolean enabled,

		// where -1 means "grant to anyone", where 0, 1, 2 would be the number of edges between any vault owner and the grantee. Exact algorithm tbd
		@JsonProperty(value = "maxWotDepth", defaultValue = "-1")
		int maxWotDepth) {
}
