package org.cryptomator.hub.api.cipherduck;

import java.util.Optional;

public record StorageConfigDto(
        String id,
        String name,
        String bucketPrefix,
        String s3Type,
        Optional<String> oidcProvider,
        Optional<String> stsRoleArn,
        Optional<String> region,
        VaultJWEBackend jwe

) implements StorageConfig {
}
