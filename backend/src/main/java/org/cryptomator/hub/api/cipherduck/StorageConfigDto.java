package org.cryptomator.hub.api.cipherduck;

import java.util.Optional;

public record StorageConfigDto(
		String id,
		String name,
		String bucketPrefix,
		Optional<String> oidcProvider,
		Optional<String> stsRoleArn,
		Optional<String> region,
		VaultJWEBackend jwe,
		Optional<Boolean> withPathStyleAccessEnabled

) implements StorageConfig {
}
