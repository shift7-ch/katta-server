package org.cryptomator.hub.katta;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(value = KeycloakTestResourceLifecycleManager.class, restrictToAnnotatedClass = true)
public class TokenExchangeIT {

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @Test
    @DisplayName("authenticate as public client 'cryptomator' and exchange token for 'cryptomatorvaults'")
    public void testTokenExchange() throws GeneralSecurityException, IOException, InterruptedException {
        // 1. Authenticate as public client using Authorization Code Flow with PKCE
        var authResponse = TinyOAuth2.client("cryptomator") //
                .withTokenEndpoint(URI.create(authServerUrl + "/protocol/openid-connect/token")) //
                .authorizationCodeGrant(URI.create(authServerUrl + "/protocol/openid-connect/auth")) //
                .authorize(newTrustingHttpClient(), uri -> {
                    try (var webClient = new WebClient()) {
                        webClient.setCssErrorHandler(new SilentCssErrorHandler());
                        webClient.getOptions().setUseInsecureSSL(true);
                        HtmlPage page = webClient.getPage(uri.toASCIIString());
                        HtmlForm form = page.getForms().getFirst();
                        form.getInputByName("username").type("alice");
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

    @Test
    @DisplayName("Ensure 400 from Keycloak is mapped to 400 and exception is logged")
    public void testFailingTokenExchange() throws IOException, InterruptedException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // 1. Authenticate as public client using Authorization Code Flow with PKCE
        var authResponse = TinyOAuth2.client("cryptomator") //
                .withTokenEndpoint(URI.create(authServerUrl + "/protocol/openid-connect/token")) //
                .authorizationCodeGrant(URI.create(authServerUrl + "/protocol/openid-connect/auth")) //
                .authorize(newTrustingHttpClient(), uri -> {
                    try (var webClient = new WebClient()) {
                        webClient.setCssErrorHandler(new SilentCssErrorHandler());
                        webClient.getOptions().setUseInsecureSSL(true);
                        HtmlPage page = webClient.getPage(uri.toASCIIString());
                        HtmlForm form = page.getForms().getFirst();
                        form.getInputByName("username").type("alice");
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
                .queryParam("vault", "666") // "666" is none of cryptomatorvaults' optional client scopes.
                .post("/storage/s3-token");
        Assertions.assertEquals(400, tokenExchangeResponse.statusCode());
        Assertions.assertEquals("Received: 'Bad Request', status code 400 from Keycloak.", tokenExchangeResponse.body().asString());
    }

    private static HttpClient newTrustingHttpClient() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        return HttpClient.newBuilder()
                .sslContext(
                        new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                                return true;
                            }
                        }).build()
                )
                .build();
    }
}
