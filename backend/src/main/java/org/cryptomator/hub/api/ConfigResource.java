package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.oidc.OidcConfigurationMetadata;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.io.IOException;
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

		return new ConfigDto(keycloakPublicUrl, keycloakRealm, keycloakClientIdHub, keycloakClientIdCryptomator, authUri, tokenUri, Instant.now().truncatedTo(ChronoUnit.MILLIS), 0);
	}

	@GET
	@Path("/cipherduckprofile")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_XML)
	@Operation(summary = "get cipherduckprofile for this hub")
	public String cipherduckprofile() throws IOException {
		// TODO https://github.com/chenkins/cipherduck-hub/issues/6 what kind of downstream pattern should we use (https://pubs.opengroup.org/architecture/o-aa-standard/DDD-strategic-patterns.html) - close to cyberduck/mountainduck or de-couple? Which representation: XML/JSON...?
		// DK: I would design the API to return a custom model and not resuse the *.cyberduckprofile XML
		// TODO https://github.com/chenkins/cipherduck-hub/issues/6 which properties do we need to make configurable and inject?
//		return new String(ConfigResource.class.getResourceAsStream("/cipherduck/S3-MinIO-STS-cryptomator-localhost.cyberduckprofile").readAllBytes());
		return new String(ConfigResource.class.getResourceAsStream("/cipherduck/S3-AWS-STS-cryptomator-staging.cyberduckprofile").readAllBytes());
	}

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
