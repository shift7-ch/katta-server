package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public interface StorageConfig {
	@JsonProperty("id")
	String id();

	@JsonProperty("name")
	String name();

	@JsonProperty("bucketPrefix")
	Optional<String> bucketPrefix();


	@JsonProperty("stsRoleArnClient")
	Optional<String> stsRoleArnClient();

	@JsonProperty("stsRoleArnHub")
	Optional<String> stsRoleArnHub();

	@JsonProperty("stsEndpoint")
	Optional<String> stsEndpoint();

	@JsonProperty("region")
	Optional<String> region();

	@JsonProperty(value = "regions")
	Optional<List<String>> regions();


	@JsonProperty(value = "withPathStyleAccessEnabled", defaultValue = "false", required = false)
	Optional<Boolean> withPathStyleAccessEnabled();

	@JsonProperty("s3Endpoint")
	Optional<String> s3Endpoint();

	@JsonProperty("jwe")
	VaultJWEBackend jwe();



}


