package org.cryptomator.hub.katta;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.quarkus.Generated;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.UUID;

import static org.cryptomator.hub.katta.KeycloakCryptomatorVaultsHelper.keycloakGrantAccessToVault;
import static org.cryptomator.hub.katta.KeycloakCryptomatorVaultsHelper.keycloakPrepareVault;
import static org.cryptomator.hub.katta.KeycloakCryptomatorVaultsHelper.keycloakRemoveAccessToVault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(value = KeycloakTestResourceLifecycleManager.class, restrictToAnnotatedClass = true)// alice must be clean.
class KeycloakCryptomatorVaultsHelperIT {
	@ConfigProperty(name = "hub.keycloak.realm")
	String keycloakRealm;

	@ConfigProperty(name = "hub.keycloak.oidc.cryptomator-vaults-client-id", defaultValue = "")
	String keycloakClientIdCryptomatorVaults;

	@KeycloakTestResourceLifecycleManager.InjectKeycloakContainer
	KeycloakContainer container;


	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {"true,true,2", "true,false,1", "false,true,1", "false,false,0", "true,null,1", "null,true,1", "null,null,0"})
	public void testKeycloakPrepareVault(final Boolean minio, final Boolean aws, final int expected) {
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
		// must not change:
		keycloakPrepareVault(vaultId, keycloak, keycloakRealm, null, null);
		assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
		// must not change:
		keycloakPrepareVault(vaultId, keycloak, keycloakRealm, minio, null);
		assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
		// must not change:
		keycloakPrepareVault(vaultId, keycloak, keycloakRealm, null, aws);
		assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
		// must not change:
		keycloakPrepareVault(vaultId, keycloak, keycloakRealm, true, true);
		assertEquals(2, clientScopeResource.getProtocolMappers().getMappers().size());

		keycloakPrepareVault(vaultId, keycloak, keycloakRealm, false, null);
		assertEquals(1, clientScopeResource.getProtocolMappers().getMappers().size());
		keycloakPrepareVault(vaultId, keycloak, keycloakRealm, null, false);
		assertEquals(0, clientScopeResource.getProtocolMappers().getMappers().size());
	}

	@Test
	public void testKeycloakGrantRemoveAccessToVault() {
		final Keycloak keycloak = container.getKeycloakAdminClient();
		final RealmResource realm = keycloak.realm(keycloakRealm);

		final String vaultId = UUID.randomUUID().toString();

		final String alice = realm.users().searchByFirstName("alice", true).getFirst().getId();

		assertNull(realm.users().get(alice).roles().getAll().getClientMappings());
		keycloakGrantAccessToVault(vaultId, alice, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, false);

		assertTrue(realm.users().get(alice).roles().getAll().getClientMappings().get(keycloakClientIdCryptomatorVaults).getMappings().stream().anyMatch(r -> r.getName().equals(vaultId)));
		keycloakRemoveAccessToVault(vaultId, alice, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, false);

		assertNull(realm.users().get(alice).roles().getAll().getClientMappings());
	}

	@Test
	public void testKeycloakGrantRemoveAccessToVaultGroup() {
		final Keycloak keycloak = container.getKeycloakAdminClient();
		final RealmResource realm = keycloak.realm(keycloakRealm);

		final String vaultId = UUID.randomUUID().toString();

		final String groupies = realm.groups().query("groupies").getFirst().getId();

		assertNull(realm.groups().group(groupies).roles().getAll().getClientMappings());

		keycloakGrantAccessToVault(vaultId, groupies, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, true);
		assertTrue(realm.groups().group(groupies).roles().getAll().getClientMappings().get(keycloakClientIdCryptomatorVaults).getMappings().stream().anyMatch(r -> r.getName().equals(vaultId)));

		keycloakRemoveAccessToVault(vaultId, groupies, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, true);
		assertNull(realm.groups().group(groupies).roles().getAll().getClientMappings());
	}
}