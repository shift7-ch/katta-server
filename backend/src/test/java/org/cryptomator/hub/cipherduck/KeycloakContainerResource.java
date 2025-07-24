package org.cryptomator.hub.cipherduck;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

public class KeycloakContainerResource implements QuarkusTestResourceLifecycleManager {
	private KeycloakContainer container;

	@Override
	public Map<String, String> start() {
		container = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.2")
				// comment in for local debugging:
				//				.withDebugFixedPort(5005, false)
				//				.withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/cryptomator-realm.json");
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
