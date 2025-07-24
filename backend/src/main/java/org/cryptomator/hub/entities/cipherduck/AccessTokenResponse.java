package org.cryptomator.hub.entities.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

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
	protected String scope;

	@JsonProperty("refresh_token")
	protected String refreshToken;
}
