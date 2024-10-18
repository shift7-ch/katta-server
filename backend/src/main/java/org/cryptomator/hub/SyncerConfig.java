package org.cryptomator.hub;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;

@ApplicationScoped
public class SyncerConfig {

	@ConfigProperty(name = "hub.keycloak.syncer-username")
	String username;

	@ConfigProperty(name = "hub.keycloak.syncer-password")
	String password;

	@ConfigProperty(name = "hub.keycloak.syncer-client-id")
	String keycloakClientId;

	@ConfigProperty(name = "hub.keycloak.local-url")
	String keycloakUrl;

	@ConfigProperty(name = "hub.keycloak.realm")
	String keycloakRealm;

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getKeycloakClientId() {
		return keycloakClientId;
	}

	public String getKeycloakUrl() {
		return keycloakUrl;
	}

	public String getKeycloakRealm() {
		return keycloakRealm;
	}

	// / start cipherduck addition
	public Keycloak getKeycloak() {
		return Keycloak.getInstance(getKeycloakUrl(), getUsername(), getPassword(), getKeycloakClientId());
	}
	// \ end cipherduck addition
}
