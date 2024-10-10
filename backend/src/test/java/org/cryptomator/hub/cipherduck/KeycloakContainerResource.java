package org.cryptomator.hub.cipherduck;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.List;
import java.util.Map;

public class KeycloakContainerResource implements QuarkusTestResourceLifecycleManager {
	private KeycloakContainer container;

	@Override
	public Map<String, String> start() {
		container = new KeycloakContainer("quay.io/keycloak/keycloak:25.0.4")
				.withFeaturesEnabled("token-exchange", "admin-fine-grained-authz")
				// comment in for local debugging:
				//				.withDebugFixedPort(5005, false)
				//				.withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/dev-realm.json")
				// N.B. remove once we're Keycloak >= 26, see https://github.com/dasniko/testcontainers-keycloak/issues/152
				.withEnv("KEYCLOAK_ADMIN", "admin")
				.withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin");


		container.start();
		return Map.of();
	}

	@Override
	public void stop() {
		if(container!=null) {
			container.stop();
		}
		container = null;
	}

	@Override
	public void inject(TestInjector testInjector) {
		testInjector.injectIntoFields(container, new TestInjector.AnnotatedAndMatchesType(InjectKeycloakContainer.class, KeycloakContainer.class));
	}
}
