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
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
		String template = new String(ConfigResource.class.getResourceAsStream("/cipherduck/hubbookmark.duck").readAllBytes());
		// nickname
		template = template.replace("<string>Cipherduck</string>", String.format("<string>Cipherduck (%s://%s:%s)</string>", requestUri.getScheme(), requestUri.getHost(), requestUri.getPort()));
		// scheme
		template = template.replace("<string>hub-http</string>", String.format("<string>hub-%s</string>", requestUri.getScheme()));
		// hostname
		template = template.replace("<string>localhost</string>", String.format("<string>%s</string>", requestUri.getHost()));
		// port
		template = template.replace("<string>8080</string>", String.format("<string>%s</string>", requestUri.getPort()));
		// UUID
		template = template.replace("<string>c36acf24-e331-4919-9f19-ff52a08e7885</string>", String.format("<string>%s</string>", Settings.get().hubId));
		return template;
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
