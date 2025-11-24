package org.cryptomator.hub.cipherduck;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.quarkus.test.common.TestResourceScope;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakGrantAccessToVault;
import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakPrepareVault;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ServerInfoResource;

import java.util.UUID;

@QuarkusTest
@WithTestResource(value = KeycloakTestResourceLifecycleManager.class, scope = TestResourceScope.RESTRICTED_TO_CLASS) // alice must be clean.
class KeycloakCryptomatorVaultsHelperIT {
    @ConfigProperty(name = "hub.keycloak.realm")
    String keycloakRealm;

    @ConfigProperty(name = "hub.keycloak.oidc.cryptomator-vaults-client-id", defaultValue = "")
    String keycloakClientIdCryptomatorVaults;

    @KeycloakTestResourceLifecycleManager.InjectKeycloakContainer
    KeycloakContainer container;

    @ParameterizedTest
    @CsvSource({"true,true,2", "true,false,1", "false,true,1", "false,false,0"})
    public void testKeycloakPrepareVault(final boolean minio, final boolean aws, final int expected) {
        final Keycloak keycloak = container.getKeycloakAdminClient();

        Keycloak admin = container.getKeycloakAdminClient();
        ServerInfoResource serverInfoResource = admin.serverInfo();
        assertNotNull(serverInfoResource.getInfo());

        final RealmResource realm = keycloak.realm(keycloakRealm);
        final String vaultId = UUID.randomUUID().toString();

        final ClientScopeResource clientScopeResource = realm.clientScopes().get(vaultId);
        final ClientWebApplicationException exc = assertThrows(ClientWebApplicationException.class, () -> clientScopeResource.getProtocolMappers().getMappers());
        assertEquals(404, exc.getResponse().getStatus());
        keycloakPrepareVault(vaultId, keycloak, keycloakRealm, minio, aws);
        assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
    }

    @Test
    public void testKeycloakGrantAccessToVault() {
        final Keycloak keycloak = container.getKeycloakAdminClient();
        final RealmResource realm = keycloak.realm(keycloakRealm);

        final String vaultId = UUID.randomUUID().toString();
        final String alice = realm.users().searchByFirstName("alice", true).getFirst().getId();
        assertNull(realm.users().get(alice).roles().getAll().getClientMappings());
        keycloakGrantAccessToVault(vaultId, alice, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, false);
        assertTrue(realm.users().get(alice).roles().getAll().getClientMappings().get(keycloakClientIdCryptomatorVaults).getMappings().stream().anyMatch(r -> r.getName().equals(vaultId)));
    }
}