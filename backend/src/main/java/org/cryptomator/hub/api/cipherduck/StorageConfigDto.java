package org.cryptomator.hub.api.cipherduck;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record StorageConfigDto(
		String id,
		String name,
		String bucketPrefix,
		Optional<String> stsRoleArn,
		Optional<String> region,
		Optional<List<String>> regions,
		VaultJWEBackend jwe,
		Optional<Boolean> withPathStyleAccessEnabled

) implements StorageConfig {
	public StorageConfigDto(StorageConfig s) {
		// workaround for defaultValue in JSONPrroperty not working as expected
		this(s.id(), s.name(), s.bucketPrefix(), s.stsRoleArn(),
				s.region().isPresent() ? s.region() : Optional.of("us-east-1"),
				s.regions().isPresent() ? s.regions() : Optional.of(Arrays.asList(
						"af-south-1",
						"ap-east-1",
						"ap-south-1",
						"ap-south-2",
						"ap-northeast-1",
						"ap-northeast-2",
						"ap-northeast-3",
						"ap-southeast-1",
						"ap-southeast-2",
						"ap-southeast-3",
						"ca-central-1",
						"eu-west-1",
						"eu-west-2",
						"eu-west-3",
						"eu-north-1",
						"eu-south-1",
						"eu-south-2",
						"eu-central-1",
						"eu-central-2",
						"me-central-1",
						"me-south-1",
						"sa-east-1",
						"us-east-1",
						"us-east-2",
						"us-west-1",
						"us-west-2"
				)),
				s.jwe(), s.withPathStyleAccessEnabled());


	}
}
