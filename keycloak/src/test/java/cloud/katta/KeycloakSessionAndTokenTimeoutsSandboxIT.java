package cloud.katta;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.response.ExtractableResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static cloud.katta.JWTDecoder.deocdeJWT;
import static io.restassured.RestAssured.given;


public class KeycloakSessionAndTokenTimeoutsSandboxIT {

	/**
	 * Document <a href="https://www.keycloak.org/docs/latest/server_admin/#_timeouts">Keycloak session and token timeouts</a>.
	 * <p>
	 * <a href="https://medium.com/@elamarane90/keycloak-session-configuration-best-practices-and-principles-cdff9348f936">Two fundamental principles for effective session management in Keycloak</a>:
	 * <ul>
	 * 	<li>Access tokens must not outlast their corresponding refresh tokens, ensuring controlled access within the refresh token’s lifespan.</li>
	 * 	<li>Refresh tokens must align with the duration of the Keycloak session, maintaining session integrity and security.<li>
	 * </ul>
	 *
	 * @see <a href="https://stackoverflow.com/questions/52040265/how-to-specify-refresh-tokens-lifespan-in-keycloak/54679852#54679852">How to specify refresh tokens lifespan in Keycloak</a>
	 */
	@ParameterizedTest
	@CsvSource({"26.3.3,5", "26.4.1,5"})
	public void inspectRefreshWithRespectToSsoSessionMaxLifespan(final String keycloakVersion, final int ssoSessionMaxLifespanSeconds) throws JSONException, InterruptedException {
		try (final KeycloakContainer container = new KeycloakContainer(String.format("quay.io/keycloak/keycloak:%s", keycloakVersion))
				// comment in for local debugging:
				//              .withDebugFixedPort(5005, false)
				//              .withCustomCommand("--log-level=DEBUG")
				// see https://github.com/dasniko/testcontainers-keycloak/blob/main/README.md
				//     https://github.com/dasniko/keycloak-extensions-demo/blob/1523b9c153f4325373c8d6787bfeb6c95d3dfed8/docker-compose.yml#L25
				.withRealmImportFile("/cryptomator-realm.json")
		) {
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloakAdminClient = container.getKeycloakAdminClient();

			// enable direct access grant for client cryptomator
			final RealmResource realm = keycloakAdminClient.realm("cryptomator");
			final ClientRepresentation cryptomatorClient = realm.clients().findByClientId("cryptomator").getFirst();

			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloakAdminClient.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);

			// set ssoSessionMaxLifespan
			final RealmRepresentation realmRepresentation = realm.toRepresentation();
			realmRepresentation.setSsoSessionMaxLifespan(ssoSessionMaxLifespanSeconds); // seconds, see https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/models/RealmModel.html
			realm.update(realmRepresentation);

			// get access token and verify its expiry is smaller than ssoSessionMaxLifespan
			final ExtractableResponse<io.restassured.response.Response> tokenResponse = given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "password")
					.formParam("username", "alice")
					.formParam("password", "asd")
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(200).extract();

			final JSONObject accessToken = deocdeJWT(tokenResponse.path("access_token"));
			final String refreshToken = tokenResponse.path("refresh_token");
			final LocalDateTime expiry = LocalDateTime.ofEpochSecond(accessToken.getInt("exp"), 0, OffsetDateTime.now().getOffset());
			final LocalDateTime now = LocalDateTime.now();

			final long delta = ChronoUnit.SECONDS.between(now, expiry);
			assert delta >= 0;
			assert delta < ssoSessionMaxLifespanSeconds;

			// do a refresh within session lifetime
			given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "refresh_token")
					.formParam("refresh_token", refreshToken)
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(200).extract();

			// let session expire
			Thread.sleep(ssoSessionMaxLifespanSeconds * 1000L);

			// after session expiry, refresh should not be possible anymore
			given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "refresh_token")
					.formParam("refresh_token", refreshToken)
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(400).extract();
		}
	}

	/**
	 * Document <a href="https://www.keycloak.org/docs/latest/server_admin/#_offline-access">Keycloak Offline Access</a>.
	 * <p>
	 * <a href="https://medium.com/@elamarane90/keycloak-session-configuration-best-practices-and-principles-cdff9348f936">Two fundamental principles for effective session management in Keycloak</a>:
	 * <ul>
	 * 	<li>The difference between a refresh token and an offline token is that an offline token never expires and is not subject to the SSO Session Idle timeout and SSO Session Max lifespan. The offline token is valid after a user logout.
	 * 	You must use the offline token for a refresh token action at least once per thirty days or for the value of the Offline Session Idle.
	 * If you enable Offline Session Max Limited, offline tokens expire after 60 days even if you use the offline token for a refresh token action. You can change this value, Offline Session Max, in the Admin Console.
	 * </li>
	 * 	<li>To issue an offline token, users must have the role mapping for the realm-level offline_access role. Clients must also have that role in their scope. Clients must add an offline_access client scope as an Optional client scope to the role, which is done by default.<li>
	 * </ul>
	 */
	@ParameterizedTest
	@CsvSource({"26.3.3,5,5", "26.4.1,5,5"})
	public void inspectRefreshWithOfflineAccess(final String keycloakVersion, final int ssoSessionMaxLifespanSeconds) throws JSONException, InterruptedException {
		try (final KeycloakContainer container = new KeycloakContainer(String.format("quay.io/keycloak/keycloak:%s", keycloakVersion))
				// comment in for local debugging:
				//              .withDebugFixedPort(5005, false)
				//              .withCustomCommand("--log-level=DEBUG")
				// see https://github.com/dasniko/testcontainers-keycloak/blob/main/README.md
				//     https://github.com/dasniko/keycloak-extensions-demo/blob/1523b9c153f4325373c8d6787bfeb6c95d3dfed8/docker-compose.yml#L25
				.withRealmImportFile("/cryptomator-realm.json")
		) {
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloakAdminClient = container.getKeycloakAdminClient();

			// enable direct access grant for client cryptomator
			final RealmResource realm = keycloakAdminClient.realm("cryptomator");
			final ClientRepresentation cryptomatorClient = realm.clients().findByClientId("cryptomator").getFirst();

			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloakAdminClient.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);

			// set ssoSessionMaxLifespan
			final RealmRepresentation realmRepresentation = realm.toRepresentation();
			realmRepresentation.setSsoSessionMaxLifespan(ssoSessionMaxLifespanSeconds); // seconds, see https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/models/RealmModel.html
			realm.update(realmRepresentation);

			// get access token and verify its expiry is smaller than ssoSessionMaxLifespan
			final ExtractableResponse<io.restassured.response.Response> tokenResponse = given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "password")
					.formParam("username", "alice")
					.formParam("password", "asd")
					.formParam("scope", "offline_access")
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(200).extract();

			final JSONObject accessToken = deocdeJWT(tokenResponse.path("access_token"));
			final String refreshToken = tokenResponse.path("refresh_token");
			final LocalDateTime expiry = LocalDateTime.ofEpochSecond(accessToken.getInt("exp"), 0, OffsetDateTime.now().getOffset());
			final LocalDateTime now = LocalDateTime.now();

			final long delta = ChronoUnit.SECONDS.between(now, expiry);
			assert delta >= 0;
			// TODO with offline access, access token has now validity of ~5 minutes, is this a bug?
			assert delta > ssoSessionMaxLifespanSeconds;

			// do a refresh within session lifetime
			given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "refresh_token")
					.formParam("refresh_token", refreshToken)
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(200).extract();

			// let session expire
			Thread.sleep(ssoSessionMaxLifespanSeconds * 1000L);

			// after session expiry, refresh with offline token is still possible
			given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "refresh_token")
					.formParam("refresh_token", refreshToken)
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(200).extract();
		}
	}

	/**
	 *
	 */
	@ParameterizedTest
	@CsvSource({"26.3.3,5,5", "26.4.1,5,5"})
	public void inspectRefreshWithOfflineAccessMaxOffline(final String keycloakVersion, final int ssoSessionMaxLifespanSeconds, final int offlineSessionMaxLifespan) throws JSONException, InterruptedException {
		try (final KeycloakContainer container = new KeycloakContainer(String.format("quay.io/keycloak/keycloak:%s", keycloakVersion))
				// comment in for local debugging:
				//              .withDebugFixedPort(5005, false)
				//              .withCustomCommand("--log-level=DEBUG")
				// see https://github.com/dasniko/testcontainers-keycloak/blob/main/README.md
				//     https://github.com/dasniko/keycloak-extensions-demo/blob/1523b9c153f4325373c8d6787bfeb6c95d3dfed8/docker-compose.yml#L25
				.withRealmImportFile("/cryptomator-realm.json")
		) {
			container.start();
			System.out.println(container.getAuthServerUrl());

			final Keycloak keycloakAdminClient = container.getKeycloakAdminClient();

			// enable direct access grant for client cryptomator
			final RealmResource realm = keycloakAdminClient.realm("cryptomator");
			final ClientRepresentation cryptomatorClient = realm.clients().findByClientId("cryptomator").getFirst();

			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloakAdminClient.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);

			// set ssoSessionMaxLifespan
			final RealmRepresentation realmRepresentation = realm.toRepresentation();
			realmRepresentation.setSsoSessionMaxLifespan(ssoSessionMaxLifespanSeconds); // seconds, see https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/models/RealmModel.html
			realmRepresentation.setOfflineSessionMaxLifespanEnabled(true);
			realmRepresentation.setOfflineSessionMaxLifespan(offlineSessionMaxLifespan); // seconds, see https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/models/RealmModel.html
			realm.update(realmRepresentation);

			// get access token and verify its expiry is smaller than ssoSessionMaxLifespan
			final ExtractableResponse<io.restassured.response.Response> tokenResponse = given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "password")
					.formParam("username", "alice")
					.formParam("password", "asd")
					.formParam("scope", "offline_access")
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(200).extract();

			final JSONObject accessToken = deocdeJWT(tokenResponse.path("access_token"));
			final String refreshToken = tokenResponse.path("refresh_token");
			final LocalDateTime expiry = LocalDateTime.ofEpochSecond(accessToken.getInt("exp"), 0, OffsetDateTime.now().getOffset());
			final LocalDateTime now = LocalDateTime.now();

			final long delta = ChronoUnit.SECONDS.between(now, expiry);
			assert delta >= 0;
			// TODO as expected vs. above.
			assert delta < ssoSessionMaxLifespanSeconds;

			// do a refresh within session lifetime
			given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "refresh_token")
					.formParam("refresh_token", refreshToken)
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(200).extract();

			// let offline access lifespan expire
			Thread.sleep(offlineSessionMaxLifespan * 1000L);

			// after offline access lifespan expire, refresh with offline token is not possible any more
			given()
					.header("Content-Type", "application/x-www-form-urlencoded")
					.formParam("client_id", "cryptomator")
					.formParam("grant_type", "refresh_token")
					.formParam("refresh_token", refreshToken)
					.when()
					.post(container.getAuthServerUrl() + "/realms/cryptomator/protocol/openid-connect/token")
					.then()
					.statusCode(400).extract();
		}
	}
}