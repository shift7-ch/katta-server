package cloud.katta;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;

import static cloud.katta.JWTDecoder.deocdeJWT;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class KattaDevRealmIT {
	/**
	 * Ensure access token to have
	 * - aud claim (required for STS)
	 * - sub claim (required for STS)
	 * - no client roles (as we add one client role per vault, the token would grow with the amount of vaults and quickly hit token size limits at AWS).
	 * <p>
	 * Note: Keycloak 25 introduces mapper for sub claim in scope "basic", the scope needs to added explicitly to the default scopes list as we override the list (in order to remove the "roles" scope):
	 * - {@see https://www.keycloak.org/docs/latest/upgrading/index.html#new-default-client-scope-basic}
	 * - {@see https://www.keycloak.org/docs/latest/release_notes/#keycloak-25-0-0}
	 */
	@Test
	public void testDevRealm() throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer("quay.io/keycloak/keycloak:25.0.4")
				.withFeaturesEnabled("token-exchange", "admin-fine-grained-authz")
				// comment in for local debugging:
				//				.withDebugFixedPort(5005, false)
				//				.withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/dev-realm.json")
				// N.B. remove once we're Keycloak >= 26, see https://github.com/dasniko/testcontainers-keycloak/issues/152
				.withEnv("KEYCLOAK_ADMIN", "admin")
				.withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
		) {
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloak = Keycloak.getInstance(
					container.getAuthServerUrl(),
					"master",
					"admin",
					"admin",
					"admin-cli");

			// enable direct access grant for client cryptomator
			final ClientRepresentation cryptomatorClient = keycloak.realm("cryptomator").clients().findByClientId("cryptomator").getFirst();
			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloak.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);

			// create client-level role "blup"
			final ClientRepresentation cryptomatorvaultsClient = keycloak.realm("cryptomator").clients().findByClientId("cryptomatorvaults").getFirst();
			keycloak.realm("cryptomator").clients().get(cryptomatorvaultsClient.getId()).roles().create(new RoleRepresentation("blup", "", false));

			// assign client-level role "blup" to user "alice"
			final UserRepresentation alice = keycloak.realm("cryptomator").users().searchByFirstName("alice", true).getFirst();
			alice.setClientRoles(Map.of("cryptomatorvaults", List.of("blup")));
			keycloak.realm("cryptomator").users().get(alice.getId()).roles().clientLevel(cryptomatorvaultsClient.getId()).add(List.of(keycloak.realm("cryptomator").clients().get(cryptomatorvaultsClient.getId()).roles().get("blup").toRepresentation()));

			final String aliceId = alice.getId();
			{
				final String accessToken =
						given()
								.header("Content-Type", "application/x-www-form-urlencoded")
								.formParam("client_id", "cryptomator")
								.formParam("grant_type", "password")
								.formParam("username", "alice")
								.formParam("password", "asd")
								.when()
								.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
								.then()
								.statusCode(200)
								.extract().path("access_token");
				final JSONObject jwt = deocdeJWT(accessToken);

				assertEquals("cryptomator", jwt.getString("aud"));
				assertEquals(aliceId, jwt.getString("sub"));
				assertTrue(((JSONArray) ((JSONObject) jwt.get("realm_access")).get("roles")).toList().contains("user"));
				assertThrows(JSONException.class, () -> jwt.get("resource_access"));

				// strangely, the "basic" scope is not added to the "scope" claim...
				assertTrue(jwt.getString("scope").contains("phone"));
				assertTrue(jwt.getString("scope").contains("email"));
				assertTrue(jwt.getString("scope").contains("profile"));
				assertEquals(3, jwt.getString("scope").split(" ").length);
			}
			final String accessToken =
					given()
							.header("Content-Type", "application/x-www-form-urlencoded")
							.formParam("client_id", "cryptomator")
							.formParam("grant_type", "password")
							.formParam("username", "alice")
							.formParam("password", "asd")
							.formParam("scope", "roles")
							.when()
							.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
							.then()
							.statusCode(200)
							.extract().path("access_token");
			final JSONObject jwt = deocdeJWT(accessToken);

			// roles scope adds additional value "account" to "aud" claim
			assertTrue(((JSONArray) jwt.get("aud")).toList().contains("cryptomator"));
			assertEquals(aliceId, jwt.getString("sub"));
			assertTrue(((JSONArray) ((JSONObject) jwt.get("realm_access")).get("roles")).toList().contains("user"));
			assertNotNull(jwt.get("resource_access"));
			assertTrue(((JSONArray) ((JSONObject) ((JSONObject) jwt.get("resource_access")).get("cryptomatorvaults")).get("roles")).toList().contains("blup"));


			assertTrue(jwt.getString("scope").contains("phone"));
			assertTrue(jwt.getString("scope").contains("email"));
			assertTrue(jwt.getString("scope").contains("profile"));

			// strangely, neither the "basic" nor the "roles" scope is not added to the "scope" claim, although the latter is requested explicitly....
			assertEquals(3, jwt.getString("scope").split(" ").length);
		}
	}
}
