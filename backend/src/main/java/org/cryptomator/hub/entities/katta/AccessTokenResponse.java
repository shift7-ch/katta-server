package org.cryptomator.hub.entities.katta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc8693#name-successful-response">RFC 8693 Token Exchange Response</a>.
 */
public class AccessTokenResponse {
	@JsonProperty("access_token")
	protected String token;

	@JsonProperty("issued_token_type")
	protected String issued_token_type;

	@JsonProperty("token_type")
	protected String token_type;

	@JsonProperty("expires_in")
	protected long expiresIn;

	@JsonProperty("scope")
	protected @Nullable String scope;

	@JsonProperty("refresh_token")
	protected @Nullable String refreshToken;
}
