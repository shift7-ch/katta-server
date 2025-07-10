package org.cryptomator.hub.cipherduck;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;

@QuarkusTest
public class TokenExchangeIT {

	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	String keycloakAuthServerUrl;

	@Test
	@DisplayName("authenticate as public client 'cryptomator' and exchange token for 'cryptomatorvaults'")
	public void testTokenExchange() throws GeneralSecurityException, IOException, InterruptedException {
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
				.queryParam("vault", "address") // "address" is one of cryptomatorvaults' optional client scope. In production there will be scopes for each vault
				.post("/storage/s3-token");
		Assertions.assertEquals(200, tokenExchangeResponse.statusCode());
		var exchangedAccessToken = new ObjectMapper().reader().readTree(tokenExchangeResponse.body().asString()).get("access_token").asText();
		var jwt = JWT.decode(exchangedAccessToken);
		Assertions.assertEquals(1, jwt.getAudience().size());
		Assertions.assertEquals("cryptomatorvaults", jwt.getAudience().getFirst());
		Assertions.assertEquals("cryptomatorvaults", jwt.getClaim("azp").asString());
		MatcherAssert.assertThat(jwt.getClaim("scope").asString(), Matchers.containsStringIgnoringCase("address"));
	}

}
