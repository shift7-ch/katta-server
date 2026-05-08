package org.cryptomator.hub.entities.katta;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum S3ServersideEncryption {
	@JsonProperty("NONE") NONE,
	@JsonProperty("SSE_AES256") SSE_AES256,
	@JsonProperty("SSE_KMS_DEFAULT") SSE_KMS_DEFAULT
}
