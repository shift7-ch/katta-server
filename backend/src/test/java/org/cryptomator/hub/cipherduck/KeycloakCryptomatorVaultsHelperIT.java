package org.cryptomator.hub.cipherduck;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakGrantAccessToVault;
import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakPrepareVault;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;

import java.util.UUID;

// N.B. we use dasniko.testcontainers.keycloak instead of @QuarkusTest+devservices to configure tls (due to nasty bug in Docker Desktop not allowing for HTTPS-only)
class KeycloakCryptomatorVaultsHelperIT {
    private static final String currentKeycloakImage = "quay.io/keycloak/keycloak:26.4.5";

    private static final String keycloakRealm = "cryptomator";

    private static final String keycloakClientIdCryptomatorVaults = "cryptomatorvaults";


    @ParameterizedTest
    @CsvSource({"true,true,2", "true,false,1", "false,true,1", "false,false,0"})
    public void testKeycloakPrepareVault(final boolean minio, final boolean aws, final int expected) {
        try (final KeycloakContainer container = new KeycloakContainer(currentKeycloakImage)
                // comment in for local debugging:
                //				.withDebugFixedPort(5005, false)
                //				.withCustomCommand("--log-level=DEBUG")
                .withRealmImportFile("/cryptomator-realm.json")
                .useTls()
        ) {
            container.start();
            System.out.println(container.getAuthServerUrl());

            final Keycloak keycloak = container.getKeycloakAdminClient();

            final RealmResource realm = keycloak.realm(keycloakRealm);
            final String vaultId = UUID.randomUUID().toString();

            final ClientScopeResource clientScopeResource = realm.clientScopes().get(vaultId);
            final jakarta.ws.rs.NotFoundException exc = assertThrows(jakarta.ws.rs.NotFoundException.class, () -> clientScopeResource.getProtocolMappers().getMappers());
            assertEquals(404, exc.getResponse().getStatus());
            keycloakPrepareVault(vaultId, keycloak, keycloakRealm, minio, aws);
            assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
        }
    }

    @Test
    public void testKeycloakGrantAccessToVault() {
        try (final KeycloakContainer container = new KeycloakContainer(currentKeycloakImage)
                // comment in for local debugging:
                //				.withDebugFixedPort(5005, false)
                //				.withCustomCommand("--log-level=DEBUG")
                .withRealmImportFile("/cryptomator-realm.json")
                .useTls()
        ) {
            container.start();
            System.out.println(container.getAuthServerUrl());

            final Keycloak keycloak = container.getKeycloakAdminClient();
            final RealmResource realm = keycloak.realm(keycloakRealm);

            final String vaultId = UUID.randomUUID().toString();
            final String alice = realm.users().searchByFirstName("alice", true).getFirst().getId();
            assertNull(realm.users().get(alice).roles().getAll().getClientMappings());
            keycloakGrantAccessToVault(vaultId, alice, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, false);
            assertTrue(realm.users().get(alice).roles().getAll().getClientMappings().get(keycloakClientIdCryptomatorVaults).getMappings().stream().anyMatch(r -> r.getName().equals(vaultId)));
        }
    }
}