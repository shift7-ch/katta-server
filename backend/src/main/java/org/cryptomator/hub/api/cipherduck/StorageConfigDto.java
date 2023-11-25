package org.cryptomator.hub.api.cipherduck;

import com.amazonaws.regions.Regions;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record StorageConfigDto(
		String id,
		String name,
		Optional<String> bucketPrefix,
		Optional<String> stsRoleArnClient,
		Optional<String> stsRoleArnHub,
		Optional<String> stsEndpoint,
		Optional<String> region,
		Optional<List<String>> regions,
		VaultJWEBackend jwe,
		Optional<Boolean> withPathStyleAccessEnabled,
		Optional<String> s3Endpoint

) implements StorageConfig {
	public StorageConfigDto(final StorageConfig s, final VaultJWEBackend jwe) {
		// workaround for defaultValue in JSONPrroperty not working as expected
		this(s.id(), s.name(), s.bucketPrefix(), s.stsRoleArnClient(), s.stsRoleArnHub(), s.stsEndpoint(),
				s.region().isPresent() ? s.region() : Optional.of("us-east-1"),
				s.regions().isPresent() ? s.regions() : Optional.of(Arrays.stream(Regions.values()).map(r -> r.getName()).toList()),
				jwe, s.withPathStyleAccessEnabled(), s.s3Endpoint());
	}

}
