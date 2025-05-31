package cloud.katta;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.core.Response;
import org.json.JSONException;
import org.json.JSONObject;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cloud.katta.JWTDecoder.deocdeJWT;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;


public class KattaTokenExchangeIT {

	/**
	 * Document token-exchange-standard:v2 behaviour according to <a href="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-enable">How to enable token exchange</a>:
	 * - downscoping audiences: the access token passed to token-exchange must come with target client id included in aud claim (i.e. cryptomatorvaults in our case)
	 * - upscoping scopes: only the requested scopes (plus the default scopes) must be in the exchanged token
	 *
	 * <blockquote cite="https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange-enable">
	 * The audience parameter can be used to filter the audiences that are coming from the used client scopes.
	 * However, this parameter will not add more audiences. When the audience parameter is omitted, no filtering occurs.
	 * As a result, the audience parameter is effectively used for "downscoping" the token to make sure that it contains only the requested audiences.
	 * However, the scope parameter is used to add optional client scopes and hence it can be used for "upscoping" and adding more scopes.
	 * </blockquote>
	 */
	@ParameterizedTest
	@CsvSource({"26.2.2,true"})
	public void inspectTokenExchangeWithAdditionalScope(final String keycloakVersion) throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer(String.format("quay.io/keycloak/keycloak:%s", keycloakVersion))
				// comment in for local debugging:
				//              .withDebugFixedPort(5005, false)
				//              .withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/dev.json")
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
						.formParam("client_secret", "")
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
	 * Test our token exchange service provider and default behaviour
	 */
	@ParameterizedTest
	@CsvSource({"true,true,true", "false,true,true", "true,false,true", "true,true,false"})
	public void testKattaTokenExchange(final boolean sharedWithAlice, final boolean addMinioMapper, final boolean addAwsMapper) throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.2")
				// comment in for local debugging:
				//				.withDebugFixedPort(5005, false)
				//				.withCustomCommand("--log-level=DEBUG")
				.withRealmImportFile("/dev.json");
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
						.formParam("client_secret", "")
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
			}

			// test for fallback to default behaviour if no scope provided
			{
				final String exchangedAccessToken = given()
						.formParam("client_id", "cryptomatorvaults")
						.formParam("client_secret", "")
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