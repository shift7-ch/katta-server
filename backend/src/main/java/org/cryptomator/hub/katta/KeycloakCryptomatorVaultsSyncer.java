package org.cryptomator.hub.katta;


import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.cryptomator.hub.api.katta.KattaConfig;
import org.cryptomator.hub.entities.Vault;

@ApplicationScoped
public class KeycloakCryptomatorVaultsSyncer {


	@Inject
	KattaConfig kattaConfig;

	@Inject
	KeycloakCryptomatorVaultsHelper keycloakCryptomatorVaultsHelper;

	@Inject
	Vault.Repository vaultRepo;

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	void sync() {
		keycloakCryptomatorVaultsHelper.keycloakCleanupDanglingCryptomatorVaultsRoles(kattaConfig.keycloakClientIdCryptomatorVaults(), vaultRepo);
	}

}
