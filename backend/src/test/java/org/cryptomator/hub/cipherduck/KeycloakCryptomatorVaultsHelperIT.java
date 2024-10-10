package org.cryptomator.hub.cipherduck;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;

import java.util.UUID;

import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakGrantAccessToVault;
import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakPrepareVault;
import static org.junit.jupiter.api.Assertions.*;

// Run as QuarkusTest as Keycloak admin client fails with no resteasy client on class path
// Re-using Keycloak devservice fails as no way found to unset oidc quarkus.oidc.auth-server-url in test profile (as Keycloak mocked/disabled in other Quarkus tests)
@QuarkusTest
@QuarkusTestResource(KeycloakContainerResource.class)
class KeycloakCryptomatorVaultsHelperIT {
	@InjectKeycloakContainer
	KeycloakContainer container;

	@ParameterizedTest
	@CsvSource({"true,true,2", "true,false,1", "false,true,1", "false,false,0"})
	public void testKeycloakPrepareVault(final boolean minio, final boolean aws, final int expected) {
		final Keycloak keycloak = Keycloak.getInstance(
				container.getAuthServerUrl(),
				"master",
				"admin",
				"admin",
				"admin-cli");

		final String vaultId = UUID.randomUUID().toString();

		final String keycloakRealm = "cryptomator";
		final RealmResource realm = keycloak.realm("cryptomator");

		final ClientScopeResource clientScopeResource = realm.clientScopes().get(vaultId);
		final ClientWebApplicationException exc = assertThrows(ClientWebApplicationException.class, () -> clientScopeResource.getProtocolMappers().getMappers());
		assertEquals(404, exc.getResponse().getStatus());
		keycloakPrepareVault(vaultId, keycloak, keycloakRealm, minio, aws);
		assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
	}

	@Test
	public void testKeycloakGrantAccessToVault() {
		final Keycloak keycloak = Keycloak.getInstance(
				container.getAuthServerUrl(),
				"master",
				"admin",
				"admin",
				"admin-cli");

		final String vaultId = UUID.randomUUID().toString();
		final String alice = keycloak.realm("cryptomator").users().searchByFirstName("alice", true).getFirst().getId();

		assertNull(keycloak.realm("cryptomator").users().get(alice).roles().getAll().getClientMappings());
		keycloakGrantAccessToVault(vaultId, alice, "cryptomatorvaults", keycloak, "cryptomator", false);
		assertTrue(keycloak.realm("cryptomator").users().get(alice).roles().getAll().getClientMappings().get("cryptomatorvaults").getMappings().stream().anyMatch(r -> r.getName().equals(vaultId)));
	}
}