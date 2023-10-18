package org.cryptomator.hub.api.cipherduck;

import java.util.List;

public record BackendsConfigDto(
		String hubId,

		List<StorageConfig> backends

) implements BackendsConfig {

}


