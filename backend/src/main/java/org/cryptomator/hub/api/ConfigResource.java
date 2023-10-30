package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.OidcConfigurationMetadata;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.cryptomator.hub.entities.Settings;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Path("/config")
public class ConfigResource {

	@Inject
	@ConfigProperty(name = "hub.keycloak.public-url", defaultValue = "")
	String keycloakPublicUrl;

	@Inject
	@ConfigProperty(name = "hub.keycloak.realm", defaultValue = "")
	String keycloakRealm;

	@Inject
	@ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "")
	String keycloakClientIdHub;

	@Inject
	@ConfigProperty(name = "hub.keycloak.oidc.cryptomator-client-id", defaultValue = "")
	String keycloakClientIdCryptomator;

	@Inject
	@ConfigProperty(name = "quarkus.oidc.auth-server-url")
	String internalRealmUrl;

	@Inject
	OidcConfigurationMetadata oidcConfData;


	@PermitAll
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public ConfigDto getConfig() {
		var publicRealmUri = trimTrailingSlash(keycloakPublicUrl + "/realms/" + keycloakRealm);
		var authUri = replacePrefix(oidcConfData.getAuthorizationUri(), trimTrailingSlash(internalRealmUrl), publicRealmUri);
		var tokenUri = replacePrefix(oidcConfData.getTokenUri(), trimTrailingSlash(internalRealmUrl), publicRealmUri);

		return new ConfigDto(keycloakPublicUrl, keycloakRealm, keycloakClientIdHub, keycloakClientIdCryptomator, authUri, tokenUri, Instant.now().truncatedTo(ChronoUnit.MILLIS), 1);
	}

    // / start cipherduck extension
    @PermitAll
    @GET
    @Path("/cipherduckhubbookmark")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(summary = "get cipherduck bookmark for this hub")
    public String cipherduckhubbookmark(@Context UriInfo uriInfo) throws IOException {
        final URI requestUri = uriInfo.getRequestUri();
		String bookmarkTemplate = new String(ConfigResource.class.getResourceAsStream("/cipherduck/hubbookmark.duck").readAllBytes());

		// Scheme
		bookmarkTemplate = bookmarkTemplate.replace("<string>Scheme</string>", String.format("<string>%s</string>", requestUri.getScheme()));

		// N.B. we use client_id="cryptomator" in cipherduck, see discussion https://github.com/chenkins/cipherduck-hub/issues/6
		bookmarkTemplate = bookmarkTemplate.replace("<string>OAuth Client ID</string>", String.format("<string>%s</string>", getConfig().keycloakClientIdCryptomator()));
		bookmarkTemplate = bookmarkTemplate.replace("<string>OAuth Authorization Url</string>", String.format("<string>%s</string>", getConfig().authEndpoint()));
		bookmarkTemplate = bookmarkTemplate.replace("<string>OAuth Token Url</string>", String.format("<string>%s</string>", getConfig().tokenEndpoint()));


        String hubUrl = String.format("%s://%s:%s", requestUri.getScheme(), requestUri.getHost(), requestUri.getPort());
        // nickname
        bookmarkTemplate = bookmarkTemplate.replace("<string>Cipherduck</string>", String.format("<string>Cipherduck (%s)</string>", hubUrl));
        // hostname
        bookmarkTemplate = bookmarkTemplate.replace("<string>localhost</string>", String.format("<string>%s</string>", requestUri.getHost()));
        // port
        bookmarkTemplate = bookmarkTemplate.replace("<string>8080</string>", String.format("<string>%s</string>", requestUri.getPort()));
        // UUID
        bookmarkTemplate = bookmarkTemplate.replace("<string>c36acf24-e331-4919-9f19-ff52a08e7885</string>", String.format("<string>%s</string>", Settings.get().hubId));

		// protocol
        bookmarkTemplate = bookmarkTemplate.replace("<string>serialized_protocol/string>", String.format("<string>%s</string>", new String(Base64.getEncoder().encode(bookmarkTemplate.getBytes(StandardCharsets.UTF_8)))));



        return bookmarkTemplate;
    }
	// \ end cipherduck extension

	//visible for testing
	String replacePrefix(String str, String prefix, String replacement) {
		int index = str.indexOf(prefix);
		if (index == 0) {
			return replacement + str.substring(prefix.length());
		} else {
			return str;
		}
	}

	//visible for testing
	String trimTrailingSlash(String str) {
		if (str.endsWith("/")) {
			return str.substring(0, str.length() - 1);
		} else {
			return str;
		}

	}

	public record ConfigDto(@JsonProperty("keycloakUrl") String keycloakUrl, @JsonProperty("keycloakRealm") String keycloakRealm,
							@JsonProperty("keycloakClientIdHub") String keycloakClientIdHub, @JsonProperty("keycloakClientIdCryptomator") String keycloakClientIdCryptomator,
							@JsonProperty("keycloakAuthEndpoint") String authEndpoint, @JsonProperty("keycloakTokenEndpoint") String tokenEndpoint,
							@JsonProperty("serverTime") Instant serverTime, @JsonProperty("apiLevel") Integer apiLevel) {
	}

}
