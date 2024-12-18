package org.cryptomator.hub.cipherduck;


import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.cryptomator.hub.SyncerConfig;
import org.cryptomator.hub.api.cipherduck.CipherduckConfig;
import org.cryptomator.hub.entities.Vault;

@ApplicationScoped
public class KeycloakCryptomatorVaultsSyncer {

	@Inject
	SyncerConfig syncerConfig;

	@Inject
	CipherduckConfig cipherduckConfig;

	@Inject
	Vault.Repository vaultRepo;

	@Scheduled(every = "{hub.keycloak.syncer-period}")
	void sync() {
		KeycloakCryptomatorVaultsHelper.keycloakCleanupDanglingCryptomatorVaultsRoles(syncerConfig, cipherduckConfig.keycloakClientIdCryptomatorVaults(), vaultRepo);
	}

}
