package org.cryptomator.hub.api.cipherduck;

import io.quarkus.oidc.OidcConfigurationMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CipherduckConfig {
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

	String replacePrefix(String str, String prefix, String replacement) {
		int index = str.indexOf(prefix);
		if (index == 0) {
			return replacement + str.substring(prefix.length());
		} else {
			return str;
		}
	}

	String trimTrailingSlash(String str) {
		if (str.endsWith("/")) {
			return str.substring(0, str.length() - 1);
		} else {
			return str;
		}

	}
	public String keycloakClientIdHub() {
		return keycloakClientIdHub;
	}

	public String keycloakClientIdCryptomator() {
		return keycloakClientIdCryptomator;
	}
	public String publicRealmUri() {
		return trimTrailingSlash(keycloakPublicUrl + "/realms/" + keycloakRealm);
	}

	public String authEndpoint() {
		return replacePrefix(oidcConfData.getAuthorizationUri(), trimTrailingSlash(internalRealmUrl), publicRealmUri());
	}

	public String tokenEndpoint() {
		return replacePrefix(oidcConfData.getTokenUri(), trimTrailingSlash(internalRealmUrl), publicRealmUri());
	}


}
