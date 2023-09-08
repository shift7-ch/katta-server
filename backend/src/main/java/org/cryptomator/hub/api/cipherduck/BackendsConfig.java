package org.cryptomator.hub.api.cipherduck;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

import java.util.List;

@StaticInitSafe
@ConfigMapping(prefix = "backends")

public interface BackendsConfig {
    List<StorageConfig> backends();
}
