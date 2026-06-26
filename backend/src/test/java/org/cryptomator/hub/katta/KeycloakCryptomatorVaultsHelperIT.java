package org.cryptomator.hub.katta;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ServerInfoResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.cryptomator.hub.katta.KeycloakCryptomatorVaultsHelper.keycloakGrantAccessToVault;
import static org.cryptomator.hub.katta.KeycloakCryptomatorVaultsHelper.keycloakPrepareVault;
import static org.cryptomator.hub.katta.KeycloakCryptomatorVaultsHelper.keycloakRemoveAccessToVault;
import static org.cryptomator.hub.katta.TokenExchangeIT.newTrustingHttpClient;
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
		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, minio, aws);
		assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
		// must not change:
		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, null, null);
		assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
		// must not change:
		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, minio, null);
		assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
		// must not change:
		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, null, aws);
		assertEquals(expected, clientScopeResource.getProtocolMappers().getMappers().size());
		// must not change:
		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, true, true);
		assertEquals(2, clientScopeResource.getProtocolMappers().getMappers().size());

		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, false, null);
		assertEquals(1, clientScopeResource.getProtocolMappers().getMappers().size());
		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, null, false);
		assertEquals(0, clientScopeResource.getProtocolMappers().getMappers().size());
	}

	@Test
	public void testKeycloakGrantRemoveAccessToVault() throws IOException, NoSuchAlgorithmException, KeyStoreException, InterruptedException, KeyManagementException {
		final Keycloak keycloak = container.getKeycloakAdminClient();
		final RealmResource realm = keycloak.realm(keycloakRealm);
		final String authServerUrl = container.getAuthServerUrl() + "/realms/cryptomator";

		final String vaultId = UUID.randomUUID().toString();

		final String alice = realm.users().searchByFirstName("alice", true).getFirst().getId();

		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, true, true);

		assertNull(realm.users().get(alice).roles().getAll().getClientMappings());
		verifyNoAccess(tokenExchange(authServerUrl, vaultId, "alice"));

		keycloakGrantAccessToVault(vaultId, alice, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, false);
		assertTrue(realm.users().get(alice).roles().getAll().getClientMappings().get(keycloakClientIdCryptomatorVaults).getMappings().stream().anyMatch(r -> r.getName().equals(vaultId)));
		verifyAccess(tokenExchange(authServerUrl, vaultId, "alice"));

		keycloakRemoveAccessToVault(vaultId, alice, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, false);
		assertNull(realm.users().get(alice).roles().getAll().getClientMappings());
		verifyNoAccess(tokenExchange(authServerUrl, vaultId, "alice"));
	}

	@Test
	public void testKeycloakGrantRemoveAccessToVaultGroup() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, InterruptedException {
		final Keycloak keycloak = container.getKeycloakAdminClient();
		final RealmResource realm = keycloak.realm(keycloakRealm);
		final String authServerUrl = container.getAuthServerUrl() + "/realms/cryptomator";

		final String vaultId = UUID.randomUUID().toString();

		final String groupies = realm.groups().query("groupies").getFirst().getId();
		final String groupiesMember = "erin";

		keycloakPrepareVault("cryptomatorvaults", vaultId, keycloak, keycloakRealm, true, true);

		assertNull(realm.groups().group(groupies).roles().getAll().getClientMappings());
		verifyNoAccess(tokenExchange(authServerUrl, vaultId, groupiesMember));

		keycloakGrantAccessToVault(vaultId, groupies, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, true);
		assertTrue(realm.groups().group(groupies).roles().getAll().getClientMappings().get(keycloakClientIdCryptomatorVaults).getMappings().stream().anyMatch(r -> r.getName().equals(vaultId)));
		verifyAccess(tokenExchange(authServerUrl, vaultId, groupiesMember));

		keycloakRemoveAccessToVault(vaultId, groupies, keycloakClientIdCryptomatorVaults, keycloak, keycloakRealm, true);
		assertNull(realm.groups().group(groupies).roles().getAll().getClientMappings());
		verifyNoAccess(tokenExchange(authServerUrl, vaultId, groupiesMember));
	}

	private static Response tokenExchange(final String authServerUrl, final String vaultId, final String user) throws IOException, InterruptedException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
		// 1. Authenticate as public client using Authorization Code Flow with PKCE
		var authResponse = TinyOAuth2.client("cryptomator") //
				.withTokenEndpoint(URI.create(authServerUrl + "/protocol/openid-connect/token")) //
				.authorizationCodeGrant(URI.create(authServerUrl + "/protocol/openid-connect/auth")) //
				.authorize(newTrustingHttpClient(), uri -> {
					try (var webClient = new WebClient()) {
						webClient.setCssErrorHandler(new SilentCssErrorHandler());
						webClient.getOptions().setUseInsecureSSL(true);
						HtmlPage page = webClient.
								getPage(uri.toASCIIString());
						HtmlForm form = page.getForms().getFirst();

						form.getInputByName("username").type(user);
						form.getInputByName("password").type("asd");
						form.getButtonByName("login").click();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}, "openid", "profile", "email"); // scopes of initial token
		Assertions.assertEquals(200, authResponse.statusCode());
		var initialAccessToken = new ObjectMapper().reader().readTree(authResponse.body()).get("access_token").asText();

		// 2. Call the token exchange endpoint
		var tokenExchangeResponse = given()
				.auth().oauth2(initialAccessToken)
				.queryParam("vault", vaultId)
				.post("/storage/s3-token");
		return tokenExchangeResponse;
	}


	private static void verifyAccess(final Response tokenExchangeResponse) throws JsonProcessingException {
		// token exchange succeeds with claims added giving access
		Assertions.assertEquals(200, tokenExchangeResponse.statusCode());

		var exchangedAccessToken = new ObjectMapper().reader().readTree(tokenExchangeResponse.body().asString()).get("access_token").asText();
		var jwt = JWT.decode(exchangedAccessToken);
		Assertions.assertEquals(1, jwt.getAudience().size());
		Assertions.assertEquals("cryptomatorvaults", jwt.getAudience().getFirst());
		Assertions.assertEquals("cryptomatorvaults", jwt.getClaim("azp").asString());
		Assertions.assertTrue(jwt.getClaims().containsKey("client_id"));
		Assertions.assertTrue(jwt.getClaims().containsKey("https://aws.amazon.com/tags"));
	}

	private static void verifyNoAccess(final Response tokenExchangeResponse) throws JsonProcessingException {
		// token exchange succeeds (as scope is defined) but no claims added giving access
		Assertions.assertEquals(200, tokenExchangeResponse.statusCode());
		var exchangedAccessToken = new ObjectMapper().reader().readTree(tokenExchangeResponse.body().asString()).get("access_token").asText();

		var jwt = JWT.decode(exchangedAccessToken);
		Assertions.assertEquals(1, jwt.getAudience().size());
		Assertions.assertEquals("cryptomatorvaults", jwt.getAudience().getFirst());
		Assertions.assertEquals("cryptomatorvaults", jwt.getClaim("azp").asString());
		Assertions.assertFalse(jwt.getClaims().containsKey("client_id"));
		Assertions.assertFalse(jwt.getClaims().containsKey("https://aws.amazon.com/tags"));
	}
}