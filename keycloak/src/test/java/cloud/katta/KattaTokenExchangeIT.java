package cloud.katta;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.core.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.shaded.com.trilead.ssh2.crypto.Base64;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cloud.katta.JWTDecoder.deocdeJWT;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

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
 * <p>
 * The reason for this setup is as follows: As <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-enable><code>requester-client</code> needs to be a confidential client</a>,
 * we cannot use <code>cryptomator</code>, which must be a public client, see <a href"https://maheshnotes.com/2024/09/20/understanding-public-and-confidential-clients-in-keycloak/">Understanding Public and Confidential Clients in Keycloak</a>.
 * Hence, we re-use our <code>target-client</code>=<code>cryptomatorvaults</code> as <code>requester-client</code>.
 * On the other hand, in order to make the token exchange call on the <code>requester-client</code>, the intial token must already contain <code>requester-client</code> in its audiences,
 * as <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-scope>audience allows only for downscoping</a>.
 * As <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-scope>scopes allow for upscoping</a>, we request vault-specific scopes in the target client and client roles to check whether a user is allowed to request access to that specific vault.
 * <blockquote cite="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-enable">
 * The audience parameter can be used to filter the audiences that are coming from the used client scopes.
 * However, this parameter will not add more audiences. When the audience parameter is omitted, no filtering occurs.
 * As a result, the audience parameter is effectively used for "downscoping" the token to make sure that it contains only the requested audiences.
 * However, the scope parameter is used to add optional client scopes and hence it can be used for "upscoping" and adding more scopes.
 * </blockquote>
 * </p>
 * <p>
 * In summary, we need to verify the following properties:
 * <ul>
 *  <li>(P1) <code>cryptomator</code> client is public, <code>cryptomatorvaults</code> is confidential and has standard token exchange enabled.</li>
 * 	<li>(P2) the initial token contains audiences <code>cryptomator</code> and <code>cryptomatorvaults</code> and <code>azp</code>=<code>cryptomator</code></li>
 * 	<li>(P3) the initial token contains realm roles</li>
 * 	<li>(P4) the exchanged token contains audience <code>cryptomatorvaults</code> and <code>azp</code>=<code>cryptomatorvaults</code> </li>
 * 	<li>(P5) the exchanged token contains the claims from the requested client roles only (if allowed)</li>
 * 	<li>(P6) the exchanged token does not contain neither realm roles nor client roles</li>
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
public class KattaTokenExchangeIT {

	/**
	 * Test (P2) and (P3) on the initial token before token-exchange with our dev realm. Serves as regression test.
	 */
	@Test
	public void testDevRealm() throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.2")
				// comment in for local debugging:
				//				.withDebugFixedPort(5005, false)
				//				.withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/cryptomator-realm.json");
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

			{
				// serviceAccountsEnabled=true for cryptomatorhub-cli allows to fetch tokens with client_secret only:
				given()
						.header("Content-Type", "application/x-www-form-urlencoded")
						.header("Authorization", "Basic: " + new String(Base64.encode("cryptomatorhub-cli:top-secret".getBytes(StandardCharsets.UTF_8))))
						.formParam("client_id", "cryptomatorhub-cli")
						.formParam("grant_type", "client_credentials")
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.statusCode(200);

				// serviceAccountsEnabled=true allows to fetch tokens with client_secret only:
				cryptomatorvaultsClient.setServiceAccountsEnabled(true);
				keycloak.realm("cryptomator").clients().get(cryptomatorvaultsClient.getId()).update(cryptomatorvaultsClient);

				given()
						.header("Content-Type", "application/x-www-form-urlencoded")
						.header("Authorization", "Basic: " + new String(Base64.encode("cryptomatorvaults:top-secret".getBytes(StandardCharsets.UTF_8))))
						.formParam("client_id", "cryptomatorvaults")
						.formParam("grant_type", "client_credentials")
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.statusCode(200);


				// serviceAccountsEnabled=false disallows to fetch tokens with client_secret only:
				cryptomatorvaultsClient.setServiceAccountsEnabled(false);
				keycloak.realm("cryptomator").clients().get(cryptomatorvaultsClient.getId()).update(cryptomatorvaultsClient);

				given()
						.header("Content-Type", "application/x-www-form-urlencoded")
						.header("Authorization", "Basic: " + new String(Base64.encode("cryptomatorvaults:top-secret".getBytes(StandardCharsets.UTF_8))))
						.formParam("client_id", "cryptomatorvaults")
						.formParam("grant_type", "client_credentials")
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.statusCode(401);
			}

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

	/**
	 * Test behaviour of token exchange with additional scope.
	 * Serves as regression test.
	 */
	@ParameterizedTest
	@CsvSource({"26.2.2,true"})
	public void inspectTokenExchangeWithAdditionalScope(final String keycloakVersion) throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer(String.format("quay.io/keycloak/keycloak:%s", keycloakVersion))
				// comment in for local debugging:
				//              .withDebugFixedPort(5005, false)
				//              .withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/cryptomator-realm.json")
		) {
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloak = container.getKeycloakAdminClient();

			// enable direct access grant for client cryptomator
			final ClientRepresentation cryptomatorClient = keycloak.realm("cryptomator").clients().findByClientId("cryptomator").getFirst();
			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloak.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);

			final String accessTokenClient1 =
					given()
							.header("Content-Type", "application/x-www-form-urlencoded")
							// https://datatracker.ietf.org/doc/html/rfc6749 OAuth 2.0 authorization, see https://datatracker.ietf.org/doc/html/rfc8693#name-request
							.formParam("client_id", "cryptomator")
							.formParam("grant_type", "password")
							.formParam("username", "alice")
							.formParam("password", "asd")
							.when()
							.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
							.then()
							.log().everything()
							.statusCode(200)
							.extract().path("access_token");
			final JSONObject jwtClient1 = deocdeJWT(accessTokenClient1);
			List<Object> auds = jwtClient1.getJSONArray("aud").toList();
			final String scopes = jwtClient1.getString("scope");
			// default client scopes
			assertTrue(scopes.contains("profile"));
			assertTrue(scopes.contains("email"));
			assertTrue(scopes.contains("phone"));
			// openid is non-default scope -> not returned if not requested
			assertFalse(scopes.contains("openid"));
			assertFalse(scopes.contains("address"));
			// (P2) audiences
			// mapped in by protocol mapper
			assertTrue(auds.contains("cryptomator"));
			assertTrue(auds.contains("cryptomatorvaults"));
			assertEquals("cryptomator", jwtClient1.getString("azp"));

			// accessToken from cryptomator client containing cryptomatorvaults in aud claim allows to exchange token with additional scope
			{
				final String exchangedAccessTokenClient = given()
						// https://datatracker.ietf.org/doc/html/rfc6749 OAuth 2.0 authorization, see https://datatracker.ietf.org/doc/html/rfc8693#name-request
						.formParam("client_id", "cryptomatorvaults") // accessToken containing cryptomatorvaults in aud claim allows this
						.formParam("client_secret", "top-secret")
						// https://datatracker.ietf.org/doc/html/rfc8693#name-request / https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-request token-exchange
						.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.formParam("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.formParam("subject_token", accessTokenClient1)
						// we now request address scope of cryptomator!
						.formParam("scope", "address")
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.log().everything()
						.statusCode(200)
						.extract().path("access_token");
				final JSONObject jwtClient2 = deocdeJWT(exchangedAccessTokenClient);

				// (P4) audience and azp
				assertEquals("cryptomatorvaults", jwtClient2.getString("aud"));
				assertEquals("cryptomatorvaults", jwtClient2.getString("azp"));

				//exchange with additional scope, the non-default scope address will be contained in the list of scopes; there are no other default scopes
				assertEquals("address", jwtClient2.getString("scope"));
			}
		}
	}

	/**
	 * Test (P4), (P5), (P6) after token exchange with our dev realm.  Serves as regression test.
	 */
	@ParameterizedTest
	@CsvSource({"true,true,true", "false,true,true", "true,false,true", "true,true,false"})
	public void testKattaTokenExchange(final boolean sharedWithAlice, final boolean addMinioMapper, final boolean addAwsMapper) throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.2")
				// comment in for local debugging:
				//				.withDebugFixedPort(5005, false)
				//				.withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/cryptomator-realm.json");
		) {
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloak = container.getKeycloakAdminClient();
			final String vaultId = UUID.randomUUID().toString();

			// enable direct access grant for client cryptomator
			final ClientRepresentation cryptomatorClient = keycloak.realm("cryptomator").clients().findByClientId("cryptomator").getFirst();
			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloak.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);

			final String alice = keycloak.realm("cryptomator").users().searchByFirstName("alice", true).getFirst().getId();
			keycloakGrantAccessToVault(vaultId, alice, "cryptomatorvaults", keycloak, "cryptomator", false);
			keycloakPrepareVault(vaultId, keycloak, "cryptomator", addMinioMapper, addAwsMapper);
			if (!sharedWithAlice) {
				keycloakRemoveAccessToVault(vaultId, alice, "cryptomatorvaults", keycloak, "cryptomator", false);
			}
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
							.log().everything()
							.statusCode(200)
							.extract().path("access_token");
			// test katta behaviour
			{
				final String exchangedAccessToken = given()
						.formParam("client_id", "cryptomatorvaults")
						.formParam("client_secret", "top-secret")
						.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.formParam("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.formParam("subject_token", accessToken)
						.formParam("scope", vaultId)
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.log().everything()
						.statusCode(200)
						.extract().path("access_token");
				final JSONObject jwtExchanged = deocdeJWT(exchangedAccessToken);

				// (P4) audience and azp
				assertEquals("cryptomatorvaults", jwtExchanged.getString("azp"));
				assertEquals("cryptomatorvaults", jwtExchanged.getString("aud"));

				// (P5) scopes and claims
				final String scopes = jwtExchanged.getString("scope");
				assertEquals(sharedWithAlice, scopes.contains(vaultId));
				assertEquals(sharedWithAlice && addAwsMapper, jwtExchanged.has("https://aws.amazon.com/tags"));
				if (sharedWithAlice && addAwsMapper) {
					// {"sub":"91e714c5-9293-4be2-baff-8d789bd9cc12","azp":"cryptomatorvaults","scope":"b2a8dbc4-eaa0-4887-ad55-9bfb4641801a","https://aws.amazon.com/tags":{"transitive_tag_keys":["b2a8dbc4-eaa0-4887-ad55-9bfb4641801a"],"principal_tags":{"b2a8dbc4-eaa0-4887-ad55-9bfb4641801a":[""]}},"iss":"http://localhost:64618/realms/cryptomator","typ":"Bearer","exp":1747919626,"iat":1747919326,"jti":"ntrtte:b73fc934-4e0f-4f74-993f-8baf605940f4","client_id":"b2a8dbc4-eaa0-4887-ad55-9bfb4641801a","sid":"e7409b3f-a13a-441a-b0ce-aa66c39b1ebc"}
					assertEquals(1, jwtExchanged.getJSONObject("https://aws.amazon.com/tags").getJSONArray("transitive_tag_keys").length());
					assertEquals(vaultId, jwtExchanged.getJSONObject("https://aws.amazon.com/tags").getJSONArray("transitive_tag_keys").getString(0));
					assertEquals(1, jwtExchanged.getJSONObject("https://aws.amazon.com/tags").getJSONObject("principal_tags").length());
					assertEquals(1, jwtExchanged.getJSONObject("https://aws.amazon.com/tags").getJSONObject("principal_tags").getJSONArray(vaultId).length());
					assertEquals("", jwtExchanged.getJSONObject("https://aws.amazon.com/tags").getJSONObject("principal_tags").getJSONArray(vaultId).getString(0));
				}
				assertEquals(sharedWithAlice && addMinioMapper, jwtExchanged.has("client_id"));
				if (sharedWithAlice && addMinioMapper) {
					assertEquals(vaultId, jwtExchanged.getString("client_id"));
				}

				// (P6)
				assertFalse(jwtExchanged.has("realm_access"));
				assertFalse(jwtExchanged.has("resource_access"));
			}

			// test for fallback to default behaviour if no scope provided
			{
				final String exchangedAccessToken = given()
						.formParam("client_id", "cryptomatorvaults")
						.formParam("client_secret", "top-secret")
						.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.formParam("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.formParam("subject_token", accessToken)
						// no scope
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.log().everything()
						.statusCode(200)
						.extract().path("access_token");
				final JSONObject jwt = deocdeJWT(exchangedAccessToken);
				// (P4) audience and azp
				assertEquals("cryptomatorvaults", jwt.getString("aud"));
				assertEquals("cryptomatorvaults", jwt.getString("azp"));
				final String scopes = jwt.getString("scope");
				assertFalse(scopes.contains(vaultId));
				assertFalse(jwt.has("https://aws.amazon.com/tags"));
				assertFalse(jwt.has("client_id"));
			}

			// test for stats 400 if duplicate scope param
			{
				given()
						.formParam("client_id", "cryptomator")
						.formParam("audience", "cryptomatorvaults")
						.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.formParam("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.formParam("subject_token", accessToken)
						.formParam("scope", vaultId)
						// duplicate scope
						.formParam("scope", vaultId)
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.log().everything()
						.statusCode(400);
			}
		}
	}

	/**
	 * The following test shows that Keycloak does not down-scope upon token refresh,
	 * i.e. it seems to ignore the optional scope param defined in Sec. 6 of <a ahref="https://www.rfc-editor.org/rfc/rfc6749#page-47">RFC 6749: The OAuth 2.0 Authorization Framework</a>
	 * Furthermore, it shows that up-scoping fails.
	 */
	@Test
	public void testNoDownScopingTokenRefresh() throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.2")
				// comment in for local debugging:
				//				.withDebugFixedPort(5005, false)
				//				.withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/cryptomator-realm.json");
		) {
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloak = container.getKeycloakAdminClient();
			final String vaultId = UUID.randomUUID().toString();

			// enable direct access grant for client cryptomator
			final ClientRepresentation cryptomatorClient = keycloak.realm("cryptomator").clients().findByClientId("cryptomator").getFirst();
			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloak.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);


			final ValidatableResponse passwordGrant = given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "password")
					.formParam("username", "alice")
					.formParam("password", "asd")
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.log().everything()
					.statusCode(200);
			final String accessToken = passwordGrant.extract().path("access_token");
			final String refreshToken = passwordGrant.extract().path("refresh_token");
			final JSONObject jwt = deocdeJWT(accessToken);
			assertTrue(jwt.getString("scope").contains("phone"));
			assertTrue(jwt.getString("scope").contains("email"));
			assertTrue(jwt.getString("scope").contains("profile"));
			assertEquals(jwt.getString("scope").split(" ").length, 3);
			final JSONObject jwtRefresh = deocdeJWT(refreshToken);
			assertTrue(jwtRefresh.getString("scope").contains("phone"));
			assertTrue(jwtRefresh.getString("scope").contains("email"));
			assertTrue(jwtRefresh.getString("scope").contains("profile"));
			assertTrue(jwtRefresh.getString("scope").contains("basic"));
			assertTrue(jwtRefresh.getString("scope").contains("web-origins"));
			assertEquals(jwtRefresh.getString("scope").split(" ").length, 5);

			ValidatableResponse refreshTokenGrant = given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "refresh_token")
					.formParam("refresh_token", refreshToken)
					.formParam("scope", "phone")
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.log().everything()
					.statusCode(200);
			final String refreshedAccessToken =
					refreshTokenGrant
							.extract().path("access_token");

			final JSONObject jwtRefreshed = deocdeJWT(refreshedAccessToken);
			assertTrue(jwtRefreshed.getString("scope").contains("phone"));
			assertTrue(jwtRefreshed.getString("scope").contains("email"));
			assertTrue(jwtRefreshed.getString("scope").contains("profile"));
			assertEquals(jwtRefreshed.getString("scope").split(" ").length, 3);
			final JSONObject jwtRefreshedRefresh = deocdeJWT(refreshToken);
			assertTrue(jwtRefreshedRefresh.getString("scope").contains("phone"));
			assertTrue(jwtRefreshedRefresh.getString("scope").contains("email"));
			assertTrue(jwtRefreshedRefresh.getString("scope").contains("profile"));
			assertTrue(jwtRefreshedRefresh.getString("scope").contains("basic"));
			assertTrue(jwtRefreshedRefresh.getString("scope").contains("web-origins"));
			assertEquals(jwtRefreshedRefresh.getString("scope").split(" ").length, 5);

			// up-scoping is not possible
			given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "refresh_token")
					.formParam("refresh_token", refreshToken)
					.formParam("scope", "snoopy")
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.log().everything()
					.statusCode(400);
		}
	}

	// ============================================================
	// methods below copied from KeycloakCryptomatorVaultsHelper
	// ============================================================
	private static void keycloakPrepareVault(String vaultId, Keycloak keycloak, String keycloakRealm, boolean minio, boolean aws) {
		// https://www.keycloak.org/docs-api/21.1.1/rest-api
		final RealmResource realm = keycloak.realm(keycloakRealm);

		final ClientScopeResource clientScopeResource = realm.clientScopes().get(vaultId);
		if (minio) {
			final ProtocolMapperRepresentation minioProtocolMapper = minioProtocolMapper(vaultId);
			clientScopeResource.getProtocolMappers().createMapper(List.of(minioProtocolMapper));
		}
		if (aws) {
			final ProtocolMapperRepresentation awsProtocolMapper = awsProtocolMapper(vaultId);
			clientScopeResource.getProtocolMappers().createMapper(List.of(awsProtocolMapper));
		}
	}

	private static ProtocolMapperRepresentation awsProtocolMapper(String vaultId) {
		final ProtocolMapperRepresentation awsProtocolMapper = new ProtocolMapperRepresentation();
		awsProtocolMapper.setName(String.format("Hard-coded mapper for vault %s (AWS)", vaultId));
		awsProtocolMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
		awsProtocolMapper.setProtocol("openid-connect");

		Map<String, String> awsConfig = new HashMap<>();
		awsConfig.put("jsonType.label", "JSON");

		awsConfig.put("userinfo.token.claim", "false");
		awsConfig.put("id.token.claim", "false");
		awsConfig.put("access.token.claim", "true");
		awsConfig.put("access.tokenResponse.claim", "false");

		awsConfig.put("claim.name", "https://aws\\.amazon\\.com/tags");
		awsConfig.put("claim.value", String.format("{\"principal_tags\":{\"%s\":[\"\"]},\"transitive_tag_keys\":[\"%s\"]}", vaultId, vaultId));

		awsProtocolMapper.setConfig(awsConfig);
		return awsProtocolMapper;
	}

	private static ProtocolMapperRepresentation minioProtocolMapper(String vaultId) {
		final ProtocolMapperRepresentation minioProtocolMapper = new ProtocolMapperRepresentation();
		minioProtocolMapper.setName(String.format("Hard-coded mapper for vault %s (MinIO)", vaultId));
		minioProtocolMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
		minioProtocolMapper.setProtocol("openid-connect");

		Map<String, String> minioConfig = new HashMap<>();
		minioConfig.put("jsonType.label", "String");

		minioConfig.put("userinfo.token.claim", "false");
		minioConfig.put("id.token.claim", "false");
		minioConfig.put("access.token.claim", "true");
		minioConfig.put("access.tokenResponse.claim", "false");

		// exhaustive list of jwt claims evaluated in MinIO: https://min.io/docs/minio/linux/administration/identity-access-management/policy-based-access-control.html#policy-variables
		// let's use client_id, as aud etc. are already use by standard mappers
		minioConfig.put("claim.name", "client_id");
		minioConfig.put("claim.value", vaultId);

		minioProtocolMapper.setConfig(minioConfig);
		return minioProtocolMapper;
	}

	private static void keycloakGrantAccessToVault(final String vaultId, final String userOrGroupId, final String clientId, final Keycloak keycloak,
												   final String keycloakRealm, final boolean isGroup) {
		// https://www.keycloak.org/docs-api/21.1.1/rest-api
		final RealmResource realm = keycloak.realm(keycloakRealm);

		final List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
		if (byClientId.size() != 1) {
			throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
		}
		final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.getFirst();
		final ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());

		// create client scope <vaultId> (if necessary)
		if (realm.clientScopes().findAll().stream().map(ClientScopeRepresentation::getId).noneMatch(vaultId::equals)) {
			ClientScopeRepresentation vaultClientScope = new ClientScopeRepresentation();
			vaultClientScope.setId(vaultId);
			vaultClientScope.setName(vaultId);
			vaultClientScope.setDescription(String.format("Client scope for vault %s", vaultId));
			vaultClientScope.setAttributes(new HashMap<>());
			vaultClientScope.setProtocol("openid-connect");

			try (Response response = realm.clientScopes().create(vaultClientScope)) {
				if (response.getStatus() != 201) {
					throw new RuntimeException(String.format("Failed to create client for vault %s. %s", vaultId, response.getStatusInfo().getReasonPhrase()));
				}
			}
		}

		// add client scope to "cryptomatorvaults" client
		// -> requires role_manage-clients
		cryptomatorVaultsClientResource.addOptionalClientScope(vaultId);

		// create client role <vaultId> (if necessary)
		// -> requires role_manage-clients
		if (cryptomatorVaultsClientResource.roles().list().stream().map(RoleRepresentation::getName).noneMatch(vaultId::equals)) {
			RoleRepresentation vaultRole = new RoleRepresentation();
			vaultRole.setName(vaultId);
			vaultRole.setDescription(String.format("Role for vault %s", vaultId));
			vaultRole.setClientRole(true);

			cryptomatorVaultsClientResource.roles().create(vaultRole);
		}

		// scope the client scope to the client role for the vault
		realm.clientScopes().get(vaultId).getScopeMappings().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));

		// add client role to user/group
		// -> requires role_manage-users
		if (!isGroup) {
			realm.users().get(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
		} else {
			realm.groups().group(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
		}
	}

	private static void keycloakRemoveAccessToVault(final String vaultId, final String userOrGroupId, final String clientId, final Keycloak keycloak,
													final String keycloakRealm, boolean isGroup) {
		// https://www.keycloak.org/docs-api/21.1.1/rest-api
		final RealmResource realm = keycloak.realm(keycloakRealm);

		// add client scope to "cryptomatorvaults" client
		// -> requires role_manage-clients
		final List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
		if (byClientId.size() != 1) {
			throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
		}
		final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.getFirst();
		final ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());
		cryptomatorVaultsClientResource.addOptionalClientScope(vaultId);

		// remove client role from user/group
		// -> requires role_manage-users
		if (!isGroup) {
			realm.users().get(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).remove(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
		} else {
			realm.groups().group(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).remove(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
		}
	}
}