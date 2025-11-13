package org.cryptomator.hub.cipherduck;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class TokenExchangeIT {

	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	String keycloakAuthServerUrl;

	@Test
	@DisplayName("authenticate as public client 'cryptomator' and exchange token for 'cryptomatorvaults'")
	public void testTokenExchange() throws GeneralSecurityException, IOException, InterruptedException {

		// 0. set ssoSessionMaxLifespan to 5s
		{
			final String initialAccessToken =
					given()
							.header("Content-Type", "application/x-www-form-urlencoded")
							.formParam("client_id", "admin-cli")
							.formParam("grant_type", "password")
							.formParam("username", "admin")
							.formParam("password", "admin")
							.when()
							.post(URI.create(keycloakAuthServerUrl.replace("cryptomator", "master") + "/protocol/openid-connect/token"))
							.then()
							.statusCode(200)
							.extract().path("access_token");
			try (Keycloak keycloak = Keycloak.getInstance(keycloakAuthServerUrl.replace("/realms/cryptomator", ""), "master", "admin-cli", initialAccessToken)) {
				final RealmResource realm = keycloak.realm("cryptomator");
				final ClientRepresentation cryptomatorClient = realm.clients().findByClientId("cryptomator").getFirst();
				cryptomatorClient.setDirectAccessGrantsEnabled(true);
				realm.clients().get(cryptomatorClient.getId()).update(cryptomatorClient);
				// set ssoSessionMaxLifespan
				final RealmRepresentation realmRepresentation = realm.toRepresentation();
				final int ssoSessionMaxLifespanSeconds = 5;
				realmRepresentation.setSsoSessionMaxLifespan(ssoSessionMaxLifespanSeconds); // seconds, see https://www.keycloak.org/docs-api/latest/javadocs/org/keycloak/models/RealmModel.html
				realm.update(realmRepresentation);
			}
		}

		// 1. Authenticate as public client using Authorization Code Flow with PKCE, getting offline token
		var authResponse = TinyOAuth2.client("cryptomator") //
				.withTokenEndpoint(URI.create(keycloakAuthServerUrl + "/protocol/openid-connect/token")) //
				.authorizationCodeGrant(URI.create(keycloakAuthServerUrl + "/protocol/openid-connect/auth")) //
				.authorize(HttpClient.newHttpClient(), uri -> {
					try (var webClient = new WebClient()) {
						webClient.setCssErrorHandler(new SilentCssErrorHandler());
						HtmlPage page = webClient.getPage(uri.toASCIIString());
						HtmlForm form = page.getForms().getFirst();
						form.getInputByName("username").type("alice");
						form.getInputByName("password").type("asd");
						form.getInputByName("login").click();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}, "openid", "profile", "email", "offline_access"); // scopes of initial token
		Assertions.assertEquals(200, authResponse.statusCode());
		var initialAccessToken = new ObjectMapper().reader().readTree(authResponse.body()).get("access_token").asText();
		var initialRefreshToken = new ObjectMapper().reader().readTree(authResponse.body()).get("refresh_token").asText();

		// 2. Call the token exchange endpoint
		Date oldExpiresAt = null;
		{
			var tokenExchangeResponse = RestAssured.given()
					.auth().oauth2(initialAccessToken)
					.queryParam("vault", "address") // "address" is one of cryptomatorvaults' optional client scope. In production there will be scopes for each vault
					.post("/storage/s3-token");
			Assertions.assertEquals(200, tokenExchangeResponse.statusCode());

			var exchangedAccessToken = new ObjectMapper().reader().readTree(tokenExchangeResponse.body().asString()).get("access_token").asText();
			var jwt = JWT.decode(exchangedAccessToken);
			Assertions.assertEquals(1, jwt.getAudience().size());
			Assertions.assertEquals("cryptomatorvaults", jwt.getAudience().getFirst());
			Assertions.assertEquals("cryptomatorvaults", jwt.getClaim("azp").asString());
			MatcherAssert.assertThat(jwt.getClaim("scope").asString(), Matchers.containsStringIgnoringCase("address"));
			oldExpiresAt = jwt.getExpiresAt();
			System.out.println(oldExpiresAt);
		}

		// 2bis. wait for session expiry
		{
			Thread.sleep(8000);
			var tokenExchangeResponse = RestAssured.given()
					.auth().oauth2(initialAccessToken)
					.queryParam("vault", "address") // "address" is one of cryptomatorvaults' optional client scope. In production there will be scopes for each vault
					.post("/storage/s3-token");
			// 200 with offline_access, 401 unauthorized without offline_access
			Assertions.assertEquals(200, tokenExchangeResponse.statusCode());
		}

		// 3. get access token with refresh token
		final ValidatableResponse refreshTokenGrant = given()
				.header("Content-Type", "application/x-www-form-urlencoded")
				.formParam("client_id", "cryptomator")
				.formParam("grant_type", "refresh_token")
				.formParam("refresh_token", initialRefreshToken)
				// needs offline_access again - otherwise 400 below!
				.formParam("scope", "phone offline_access")
				.when()
				.post(URI.create(keycloakAuthServerUrl + "/protocol/openid-connect/token"))
				.then()
				.log().everything()
				.statusCode(200);
		final String refreshedAccessToken =
				refreshTokenGrant
						.extract().path("access_token");

		// 4. do token exchange
		{
			var tokenExchangeResponse = RestAssured.given()
					.auth().oauth2(refreshedAccessToken)
					.queryParam("vault", "address") // "address" is one of cryptomatorvaults' optional client scope. In production there will be scopes for each vault
					.post("/storage/s3-token");
			Assertions.assertEquals(200, tokenExchangeResponse.statusCode());

			var exchangedAccessToken = new ObjectMapper().reader().readTree(tokenExchangeResponse.body().asString()).get("access_token").asText();
			var jwt = JWT.decode(exchangedAccessToken);
			Assertions.assertEquals(1, jwt.getAudience().size());
			Assertions.assertEquals("cryptomatorvaults", jwt.getAudience().getFirst());
			Assertions.assertEquals("cryptomatorvaults", jwt.getClaim("azp").asString());
			MatcherAssert.assertThat(jwt.getClaim("scope").asString(), Matchers.containsStringIgnoringCase("address"));
			System.out.println(jwt.getExpiresAt());
			assertTrue(jwt.getExpiresAt().after(oldExpiresAt));
		}
	}

	@Test
	@DisplayName("Ensure 400 from Keycloak is mapped to 400 and exception is logged")
	public void testFailingTokenExchange() throws GeneralSecurityException, IOException, InterruptedException {
		// 1. Authenticate as public client using Authorization Code Flow with PKCE
		var authResponse = TinyOAuth2.client("cryptomator") //
				.withTokenEndpoint(URI.create(keycloakAuthServerUrl + "/protocol/openid-connect/token")) //
				.authorizationCodeGrant(URI.create(keycloakAuthServerUrl + "/protocol/openid-connect/auth")) //
				.authorize(HttpClient.newHttpClient(), uri -> {
					try (var webClient = new WebClient()) {
						webClient.setCssErrorHandler(new SilentCssErrorHandler());
						HtmlPage page = webClient.getPage(uri.toASCIIString());
						HtmlForm form = page.getForms().getFirst();
						form.getInputByName("username").type("alice");
						form.getInputByName("password").type("asd");
						form.getInputByName("login").click();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}, "openid", "profile", "email"); // scopes of initial token
		Assertions.assertEquals(200, authResponse.statusCode());
		var initialAccessToken = new ObjectMapper().reader().readTree(authResponse.body()).get("access_token").asText();

		// 2. Call the token exchange endpoint
		var tokenExchangeResponse = RestAssured.given()
				.auth().oauth2(initialAccessToken)
				.queryParam("vault", "666") // "666" is none of cryptomatorvaults' optional client scopes.
				.post("/storage/s3-token");
		Assertions.assertEquals(400, tokenExchangeResponse.statusCode());
		Assertions.assertEquals("Received: 'Bad Request', status code 400 from Keycloak.", tokenExchangeResponse.body().asString());
	}
}
