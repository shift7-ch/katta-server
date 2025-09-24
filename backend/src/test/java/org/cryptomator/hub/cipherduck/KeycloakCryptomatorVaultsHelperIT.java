package org.cryptomator.hub.cipherduck;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;

import java.util.UUID;

import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakGrantAccessToVault;
import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakPrepareVault;
import static org.junit.jupiter.api.Assertions.*;

// N.B. @Inject Keycloak points at points at 8180, use KeycloakBuilder with quarkus.oidc.auth-server-url instead
@QuarkusTest
class KeycloakCryptomatorVaultsHelperIT {
	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	String keycloakAuthServerUrl;

	@ConfigProperty(name = "hub.keycloak.realm")
	String keycloakRealm;

	@ConfigProperty(name = "hub.keycloak.oidc.cryptomator-vaults-client-id", defaultValue = "")
	String keycloakClientIdCryptomatorVaults;

	@ConfigProperty(name = "hub.keycloak.system-client-id", defaultValue = "")
	String keycloakSystemClientId;

	@ConfigProperty(name = "hub.keycloak.system-client-secret", defaultValue = "")
	String keycloakSystemClientSecret;

	@ParameterizedTest
	@CsvSource({"true,true,2", "true,false,1", "false,true,1", "false,false,0"})
	public void testKeycloakPrepareVault(final boolean minio, final boolean aws, final int expected) {
		final Keycloak keycloak = KeycloakBuilder.builder()
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.serverUrl(keycloakAuthServerUrl.replace(String.format("/realms/%s", keycloakRealm), ""))
				.realm("cryptomator")
				.clientId(keycloakSystemClientId)
				.clientSecret(keycloakSystemClientSecret)
				.build();
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
		final Keycloak keycloak = KeycloakBuilder.builder()
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.serverUrl(keycloakAuthServerUrl.replace(String.format("/realms/%s", keycloakRealm), ""))
				.realm("cryptomator")
				.clientId(keycloakSystemClientId)
				.clientSecret(keycloakSystemClientSecret)
				.build();
		final RealmResource realm = keycloak.realm(keycloakRealm);

		final String vaultId = UUID.randomUUID().toString();
		final String alice = realm.users().searchByFirstName("alice", true).getFirst().getId();
		assertNull(realm.users().get(alice).roles().getAll().getClientMappings());
		keycloakGrantAccessToVault(vaultId, alice, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, false);
		assertTrue(realm.users().get(alice).roles().getAll().getClientMappings().get(keycloakClientIdCryptomatorVaults).getMappings().stream().anyMatch(r -> r.getName().equals(vaultId)));
	}
}