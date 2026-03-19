package org.cryptomator.hub.cipherduck;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.restassured.RestAssured;

import java.lang.annotation.*;
import java.util.Map;

// We need to use TLS to circumvent to nasty unresolved bug in Docker Desktop not allowing for HTTPS-only, see e.g. https://github.com/dasniko/testcontainers-keycloak/issues/208.
// N.B. we do not use @QuarkusTest+keycloak-devservices:
// - to configure TLS easily (see e.g. https://www.orpiske.net/2025/08/programmatic-keycloak-configuration-for-quarkus-integration-tests/, https://quarkus.io/guides/security-openid-connect-dev-services#configuration-reference)
// - not to start Keycloak for all tests (too heavy - however, this might be achieved using a profile for tests requiring Keycloak: https://quarkus.io/guides/getting-started-testing#writing-a-profile).
// Alternatively, instead of dasniko.testcontainers.keycloak, custom io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager might be used.
public class KeycloakTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {
    private static final String currentKeycloakImage = "quay.io/keycloak/keycloak:26.5.5";
    private static KeycloakContainer container;

    @Override
    public Map<String, String> start() {
        try {
            RestAssured.useRelaxedHTTPSValidation();
            container = new KeycloakContainer(currentKeycloakImage)
                    // comment in for local debugging:
                    //				.withDebugFixedPort(5005, false)
                    //				.withCustomCommand("--log-level=DEBUG")
                    .withRealmImportFile("/cryptomator-realm.json")
                    .useTls();
            container.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println(container.getAuthServerUrl());
        return Map.of("quarkus.oidc.auth-server-url", container.getAuthServerUrl() + "/realms/cryptomator");
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(container, new TestInjector.AnnotatedAndMatchesType(InjectKeycloakContainer.class, KeycloakContainer.class));
    }

    @Override
    public void stop() {
        container.stop();
    }


    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface InjectKeycloakContainer {
    }

    public static void main(String[] args) throws InterruptedException {
        container = new KeycloakContainer(currentKeycloakImage)
                // comment in for local debugging:
                //				.withDebugFixedPort(5005, false)
                //				.withCustomCommand("--log-level=DEBUG")
                .withRealmImportFile("/cryptomator-realm.json")
                .useTls();
        container.start();
        System.out.println(container.getAuthServerUrl());
        Thread.sleep(5000000);
    }
}
