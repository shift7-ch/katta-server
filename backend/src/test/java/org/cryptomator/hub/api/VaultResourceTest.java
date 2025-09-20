package org.cryptomator.hub.api;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.security.TestSecurity;
import org.cryptomator.hub.api.cipherduck.CipherduckConfig;
import org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.license.LicenseHolder;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

class VaultResourceTest {
	private VaultResource vaultResource;

	private final EventLogger eventLogger = Mockito.mock(EventLogger.class);
	private final User.Repository userRepo = Mockito.mock(User.Repository.class);
	private final Group.Repository groupRepo = Mockito.mock(Group.Repository.class);
	private final EffectiveVaultAccess.Repository effectiveVaultAccessRepo = Mockito.mock(EffectiveVaultAccess.Repository.class);
	private final Vault.Repository vaultRepo = Mockito.mock(Vault.Repository.class);
	private final VaultAccess.Repository vaultAccessRepo = Mockito.mock(VaultAccess.Repository.class);

	private final SecurityIdentity identity = Mockito.mock(SecurityIdentity.class);
	private final LicenseHolder license = Mockito.mock(LicenseHolder.class);
	private final KeycloakCryptomatorVaultsHelper keycloakCryptomatorVaultsHelper = Mockito.mock(KeycloakCryptomatorVaultsHelper.class);
	private final CipherduckConfig cipherduckConfig = Mockito.mock(CipherduckConfig.class);


	@BeforeEach
	void setUp() {
		vaultResource = new VaultResource();
		vaultResource.eventLogger = eventLogger;
		vaultResource.userRepo = userRepo;
		vaultResource.groupRepo = groupRepo;
		vaultResource.effectiveVaultAccessRepo = effectiveVaultAccessRepo;
		vaultResource.vaultRepo = vaultRepo;
		vaultResource.vaultAccessRepo = vaultAccessRepo;
		vaultResource.jwt = new JsonWebToken() {
			@Override
			public String getName() {
				return "";
			}

			@Override
			public Set<String> getClaimNames() {
				return Set.of("sub");
			}

			@Override
			public String getClaim(String claimName) {
				switch (claimName) {
					case "sub":
						return "alice";
				}
				return null;
			}
		};
		vaultResource.identity = identity;
		vaultResource.license = license;
		vaultResource.keycloakCryptomatorVaultsHelper = keycloakCryptomatorVaultsHelper;
		vaultResource.cipherduckConfig = cipherduckConfig;

		final User user = Mockito.mock(User.class);
		Mockito.when(userRepo.findById("alice")).thenReturn(user);
		Mockito.when(cipherduckConfig.keycloakClientIdCryptomatorVaults()).thenReturn("pesto");
	}

	@ParameterizedTest
	@CsvSource({"false,false", "false,true", "true,false", "true,true"})
	@TestSecurity(user = "Alice", roles = {"user", "create-vaults"})
	public void testCreateOrUpdate(final boolean minio, final boolean aws) {
		final UUID vaultId = UUID.randomUUID();
		var vaultDto = new VaultResource.VaultDto(vaultId, "My Vault", "Test vault 4", false, Instant.parse("2112-12-21T21:12:21Z"), "uvfMetadata3", "uvfKeySet3", "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3");

		vaultResource.createOrUpdate(vaultId, vaultDto, minio, aws);

		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(1)).keycloakPrepareVault(vaultId.toString(), minio, aws);
		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(1)).keycloakGrantAccessToVault(vaultId.toString(), "alice", "pesto", groupRepo);
	}
}