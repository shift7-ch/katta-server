package cloud.katta;

import dasniko.testcontainers.keycloak.KeycloakContainer;
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
	 * In katta, we want to use vault-specific tokens for
	 * <ul>
	 *     <li>fine-grained access management for S3 storage access</li>
	 *     <li>token size restriction (leading to <a href="https://repost.aws/knowledge-center/iam-role-aws-sts-error">PackedPolicyTooLarge</a>)</li>
	 * </ul>
	 * Therefore, we use <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange">Standard token exchange: version 2 (V2)</a> to exchange an initial token (requiring user interaction for authentication) for vault-specific target tokens we use for STS/S3 calls.
	 * <p>
	 * We implement <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-flow">Standard Token Exchange Flow</a>:
	 * <ul>
	 *   <li><code>initial-client</code>=<code>cryptomator</code></li>
	 *   <li><code>requester-client</code>=<code>target-client</code>=<code>cryptomatorvaults</code></li>
	 * </ul>
	 * </p>
	 * The reason for this setup is as follows: As <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-enable><code>requester-client</code> needs to be a confidential client</a>,
	 * we cannot use <code>cryptomator</code>, which must be a public client, see <a href"https://maheshnotes.com/2024/09/20/understanding-public-and-confidential-clients-in-keycloak/">Understanding Public and Confidential Clients in Keycloak</a>.
	 * Hence, we re-use our <code>target-client</code>=<code>cryptomatorvaults</code> as <code>requester-client</code>.
	 * On the other hand, in order to make the token exchange call on the <code>requester-client</code>, the intial token must already contain <code>requester-client</code> in its audiences,
	 * as <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-scope>audience allows only for downscoping</a>.
	 * As <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-scope>scopes allow for upscoping</a>, we request vault-specific scopes in the target client and client roles to check whether a user is allowed to request access to that specific vault.
	 * <p>
	 * In summary, we need to verify the following properties:
	 * <ul>
	 *  <li>(P1) <code>cryptomator</code> client is public, <code>cryptomatorvaults</code> is confidential and has standard token exchange enabled.</li>
	 * 	<li>(P2) the initial token contains audiences <code>cryptomator</code> and <code>cryptomatorvaults</code> and <code>azp</code>=<code>cryptomator</code></li>
	 * 	<li>(P3) the initial token contains realm roles</li>
	 * 	<li>(P4) the exchanged token contains audience <code>cryptomatorvaults</code> and <code>azp</code>=<code>cryptomatorvaults</code> </li>
	 * 	<li>(P5) the exchanged token contains the claims from the requested client roles only (if allowed)</li>
	 * 	<li>(P6) the exchanged token does not contain realm roles</li>
	 *
	 * </ul>
	 * </p>
	 * <p>
	 * Note: Keycloak 25 introduces mapper for sub claim in scope "basic", the scope needs to added explicitly to the default scopes list as we override the list (in order to remove the "roles" scope):
	 * <ul>
	 *   <li>{@see https://www.keycloak.org/docs/latest/upgrading/index.html#new-default-client-scope-basic}</li>
	 *   <li>{@see https://www.keycloak.org/docs/latest/release_notes/#keycloak-25-0-0}</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Note: access management for vault creation is handled differently.
	 * </p>
	 */
	@Test
	public void testDevRealm() throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.2")
				// comment in for local debugging:
				//				.withDebugFixedPort(5005, false)
				//				.withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/dev.json");
		) {
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloak = container.getKeycloakAdminClient();

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
				final String accessTokenWithoutRoles =
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
				final JSONObject jwt = deocdeJWT(accessTokenWithoutRoles);

				// (P2) audiences
				assertTrue(jwt.getJSONArray("aud").toList().contains("cryptomator"));
				assertTrue(jwt.getJSONArray("aud").toList().contains("cryptomatorvaults"));
				assertEquals(2, jwt.getJSONArray("aud").length());

				// (P3) realm roles
				assertTrue(jwt.getJSONObject("realm_access").getJSONArray("roles").toList().contains("user"));
				assertTrue(jwt.getJSONObject("realm_access").getJSONArray("roles").toList().contains("create-vaults"));

				assertFalse(jwt.has("resource_access"));

				assertEquals(aliceId, jwt.getString("sub"));

				// strangely, the "basic" scope is not added to the "scope" claim...
				assertTrue(jwt.getString("scope").contains("phone"));
				assertTrue(jwt.getString("scope").contains("email"));
				assertTrue(jwt.getString("scope").contains("profile"));
				assertEquals(3, jwt.getString("scope").split(" ").length);
			}
			{
				final String accessTokenWithRoles =
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
				final JSONObject jwt = deocdeJWT(accessTokenWithRoles);

				// (P2) audiences
				// "roles" scope adds additional value "account" to "aud" claim
				assertTrue(jwt.getJSONArray("aud").toList().contains("cryptomator"));
				assertTrue(jwt.getJSONArray("aud").toList().contains("account"));
				assertTrue(jwt.getJSONArray("aud").toList().contains("cryptomatorvaults"));
				assertEquals(3, jwt.getJSONArray("aud").length());

				// (P3) realm roles
				assertTrue(jwt.getJSONObject("realm_access").getJSONArray("roles").toList().contains("user"));
				assertTrue(jwt.getJSONObject("realm_access").getJSONArray("roles").toList().contains("create-vaults"));

				assertNotNull(jwt.get("resource_access"));
				// "roles" scope adds client scopes to "resource_access.<clientId>.roles"
				assertTrue(jwt.getJSONObject("resource_access").getJSONObject("cryptomatorvaults").getJSONArray("roles").toList().contains("blup"));

				assertEquals(aliceId, jwt.getString("sub"));

				assertTrue(jwt.getString("scope").contains("phone"));
				assertTrue(jwt.getString("scope").contains("email"));
				assertTrue(jwt.getString("scope").contains("profile"));
				// strangely, neither the "basic" nor the "roles" scope is not added to the "scope" claim, although the latter is requested explicitly....
				assertEquals(3, jwt.getString("scope").split(" ").length);
			}
		}
	}
}
