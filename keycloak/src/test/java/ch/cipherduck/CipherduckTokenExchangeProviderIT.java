package ch.cipherduck;

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
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ch.cipherduck.JWTDecoder.deocdeJWT;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;


public class CipherduckTokenExchangeProviderIT {

	/**
	 * Document the new behaviour @see <a href="hhttps://github.com/keycloak/keycloak/issues/29614">Keycloak Issue 29614</a> which makes our spi necessary.
	 */
	@ParameterizedTest
	@CsvSource({"21.1.1,true", "24.0.4,false", "25.0.4,false"})
	public void inspectTokenExchangeWithAdditionalScope(final String keycloakVersion, final boolean exchangePossible) throws JSONException {
		try (final KeycloakContainer container = new KeycloakContainer(String.format("quay.io/keycloak/keycloak:%s", keycloakVersion))
				.withFeaturesEnabled("token-exchange", "admin-fine-grained-authz")
				// comment in for local debugging:
				//              .withDebugFixedPort(5005, false)
				//              .withCustomCommand("--log-level=DEBUG")
				// see https://github.com/dasniko/testcontainers-keycloak/blob/main/README.md
				//     https://github.com/dasniko/keycloak-extensions-demo/blob/1523b9c153f4325373c8d6787bfeb6c95d3dfed8/docker-compose.yml#L25
				.withRealmImportFile("/dev-realm.json")
				// Keycloak < 25 seems to expose /health/started on default port, and not on management port as expected in testcontainers-keycloak:
				//   https://github.com/dasniko/testcontainers-keycloak/blame/d910aa6d6919c0e0f9cd50f97c9bf878eb24f753/src/main/java/dasniko/testcontainers/keycloak/ExtendableKeycloakContainer.java#L203
				.waitingFor(Wait.forLogMessage(".*Listening.*", 1))
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
			final String scopes = deocdeJWT(accessToken).getString("scope");
			assertTrue(scopes.contains("profile"));
			assertTrue(scopes.contains("email"));
			assertTrue(scopes.contains("phone"));
			// openid is non-default scope -> not returned if not requested
			assertFalse(scopes.contains("openid"));
			final String exchangedAccessToken = given()
					.formParam("client_id", "cryptomator")
					.formParam("audience", "cryptomatorvaults")
					.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
					.formParam("subject_token", accessToken)
					// we request openid scope
					.formParam("scope", "openid")
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(200)
					.extract().path("access_token");
			// if exchange possible with additional scope, the non-default scope openid will be contained in the list of scopes
			assertEquals(exchangePossible ? "openid" : "", deocdeJWT(exchangedAccessToken).get("scope"));
			final JSONObject jwt = deocdeJWT(exchangedAccessToken);
			assertEquals("cryptomatorvaults", jwt.getString("aud"));
		}
	}

	/**
	 * Test our token exchange service provider and default behaviour
	 */
	@ParameterizedTest
	@CsvSource({"true,true,true,true", "true,false,true,true", "false,true,true,true"})
	public void testCipherduckTokenExchange(final boolean spiEnabled, final boolean shared, final boolean minio, final boolean aws) throws JSONException {
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
			if (spiEnabled) {
				container.withDefaultProviderClasses();
			}
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloak = Keycloak.getInstance(
					container.getAuthServerUrl(),
					"master",
					"admin",
					"admin",
					"admin-cli");
			final String vaultId = UUID.randomUUID().toString();

			// enable direct access grant for client cryptomator
			final ClientRepresentation cryptomatorClient = keycloak.realm("cryptomator").clients().findByClientId("cryptomator").getFirst();
			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloak.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);

			final String alice = keycloak.realm("cryptomator").users().searchByFirstName("alice", true).getFirst().getId();
			keycloakGrantAccessToVault(vaultId, alice, "cryptomatorvaults", keycloak, "cryptomator", false);
			keycloakPrepareVault(vaultId, keycloak, "cryptomator", minio, aws);
			if (!shared) {
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
							.statusCode(200)
							.extract().path("access_token");

			// test cipherduck behaviour
			{
				final String exchangedAccessToken = given()
						.formParam("client_id", "cryptomator")
						.formParam("audience", "cryptomatorvaults")
						.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.formParam("subject_token", accessToken)
						.formParam("scope", vaultId)
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.statusCode(200)
						.extract().path("access_token");
				final JSONObject jwt = deocdeJWT(exchangedAccessToken);
				assertEquals("cryptomatorvaults", jwt.getString("aud"));
				final String scopes = jwt.getString("scope");
				assertEquals(spiEnabled && shared, scopes.contains(vaultId));
				assertEquals(spiEnabled && shared && aws, jwt.has("https://aws.amazon.com/tags"));
				assertEquals(spiEnabled && shared && minio, jwt.has("client_id"));
			}

			// test for fallback to default behaviour if no scope provided
			{
				final String exchangedAccessToken = given()
						.formParam("client_id", "cryptomator")
						.formParam("audience", "cryptomatorvaults")
						.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.formParam("subject_token", accessToken)
						// no scope
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
						.statusCode(200)
						.extract().path("access_token");
				final JSONObject jwt = deocdeJWT(exchangedAccessToken);
				assertEquals("cryptomatorvaults", jwt.getString("aud"));
				final String scopes = jwt.getString("scope");
				assertFalse(scopes.contains(vaultId));
				assertFalse(jwt.has("https://aws.amazon.com/tags"));
				assertFalse(jwt.has("client_id"));
			}
			// test for fallback to default behaviour if no audience and no scope
			{
				final String exchangedAccessToken =
						given()
								.formParam("client_id", "cryptomator")
								// no audience
								.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
								.formParam("subject_token", accessToken)
								// no scope
								.when()
								.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
								.then()
								.statusCode(200)
								.extract().path("access_token");
				final JSONObject jwt = deocdeJWT(exchangedAccessToken);
				assertEquals("cryptomator", jwt.getString("aud"));
				final String scopes = jwt.getString("scope");
				assertFalse(scopes.contains(vaultId));
				assertFalse(jwt.has("https://aws.amazon.com/tags"));
				assertFalse(jwt.has("client_id"));
			}

			// test for fallback to default behaviour if duplicate scope param
			{
				given()
						.formParam("client_id", "cryptomator")
						.formParam("audience", "cryptomatorvaults")
						.formParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.formParam("subject_token", accessToken)
						.formParam("scope", vaultId)
						// duplicate scope
						.formParam("scope", vaultId)
						.when()
						.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
						.then()
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