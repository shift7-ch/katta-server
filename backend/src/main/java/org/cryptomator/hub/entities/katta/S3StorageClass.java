package org.cryptomator.hub.entities.katta;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum S3StorageClass {
	@JsonProperty("STANDARD") STANDARD,
	@JsonProperty("INTELLIGENT_TIERING") INTELLIGENT_TIERING,
	@JsonProperty("STANDARD_IA") STANDARD_IA,
	@JsonProperty("ONEZONE_IA") ONEZONE_IA,
	@JsonProperty("REDUCED_REDUNDANCY") REDUCED_REDUNDANCY,
	@JsonProperty("GLACIER") GLACIER,
	@JsonProperty("GLACIER_IR") GLACIER_IR,
	@JsonProperty("DEEP_ARCHIVE") DEEP_ARCHIVE
}
