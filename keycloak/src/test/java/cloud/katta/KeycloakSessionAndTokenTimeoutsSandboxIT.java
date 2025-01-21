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
import org.testcontainers.containers.wait.strategy.Wait;

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
	 *	<li>Access tokens must not outlast their corresponding refresh tokens, ensuring controlled access within the refresh token’s lifespan.</li>
	 * 	<li>Refresh tokens must align with the duration of the Keycloak session, maintaining session integrity and security.<li>
	 * </ul>
	 *
	 * @see <a href="https://stackoverflow.com/questions/52040265/how-to-specify-refresh-tokens-lifespan-in-keycloak/54679852#54679852">How to specify refresh tokens lifespan in Keycloak</a>
	 */
	@ParameterizedTest
	@CsvSource({"25.0.6,5"})
	public void inspectRefreshWithRespectToSsoSessionMaxLifespan(final String keycloakVersion, final int ssoSessionMaxLifespan) throws JSONException, InterruptedException {
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
			final RealmResource realm = keycloak.realm("cryptomator");
			final ClientRepresentation cryptomatorClient = realm.clients().findByClientId("cryptomator").getFirst();

			cryptomatorClient.setDirectAccessGrantsEnabled(true);
			keycloak.realm("cryptomator").clients().get(cryptomatorClient.getId()).update(cryptomatorClient);

			// set ssoSessionMaxLifespan
			final RealmRepresentation realmRepresentation = realm.toRepresentation();
			realmRepresentation.setSsoSessionMaxLifespan(ssoSessionMaxLifespan); // seconds, see https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/models/RealmModel.html
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
			assert delta < ssoSessionMaxLifespan;

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
			Thread.sleep(ssoSessionMaxLifespan * 1000L);

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
}