package org.cryptomator.hub.api;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.event.Event;
import jakarta.ws.rs.NotFoundException;
import org.cryptomator.hub.api.katta.KattaConfig;
import org.cryptomator.hub.entities.EffectiveVaultAccess;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.entities.events.EventLogger;
import org.cryptomator.hub.events.VaultMembersJoined;
import org.cryptomator.hub.katta.KeycloakCryptomatorVaultsHelper;
import org.cryptomator.hub.license.HubLicenseEntitlements;
import org.cryptomator.hub.license.LicenseHolder;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class VaultResourceTest {
	private VaultResource vaultResource;

	private final UUID vaultId = UUID.randomUUID();

	private final EventLogger eventLogger = Mockito.mock(EventLogger.class);
	private final User.Repository userRepo = Mockito.mock(User.Repository.class);
	private final Group.Repository groupRepo = Mockito.mock(Group.Repository.class);
	private final EffectiveVaultAccess.Repository effectiveVaultAccessRepo = Mockito.mock(EffectiveVaultAccess.Repository.class);
	private final Vault.Repository vaultRepo = Mockito.mock(Vault.Repository.class);
	private final VaultAccess.Repository vaultAccessRepo = Mockito.mock(VaultAccess.Repository.class);

	private final SecurityIdentity identity = Mockito.mock(SecurityIdentity.class);
	private final LicenseHolder license = Mockito.mock(LicenseHolder.class);
	private final KeycloakCryptomatorVaultsHelper keycloakCryptomatorVaultsHelper = Mockito.mock(KeycloakCryptomatorVaultsHelper.class);
	private final KattaConfig kattaConfig = Mockito.mock(KattaConfig.class);
	private final Event<VaultMembersJoined> vaultMembersJoinedEvent = Mockito.mock();


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
		vaultResource.kattaConfig = kattaConfig;
		vaultResource.vaultMembersJoinedEvent = vaultMembersJoinedEvent;

		final User user = Mockito.mock(User.class);
		Mockito.when(userRepo.findById("alice")).thenReturn(user);
		Mockito.when(userRepo.findByIdOptional("alice")).thenReturn(Optional.of(user));
		Mockito.when(kattaConfig.keycloakClientIdCryptomatorVaults()).thenReturn("pesto");
		Mockito.when(license.getEntitlements()).thenReturn(HubLicenseEntitlements.create().withSeats(1L));
		Mockito.when(vaultRepo.findById(vaultId)).thenReturn(new Vault());
		Mockito.when(groupRepo.findByIdOptional("good cops")).thenReturn(Optional.of(new Group()));
		Mockito.when(vaultAccessRepo.deleteById(new VaultAccess.Id(vaultId, "good cops"))).thenReturn(true);
	}

	@ParameterizedTest
	@CsvSource({"false,false", "false,true", "true,false", "true,true"})
	public void testCreateOrUpdate(final boolean minio, final boolean aws) {
		final UUID vaultId = UUID.randomUUID();
		var vaultDto = new VaultResource.VaultDto(vaultId, "My Vault", Instant.parse("2112-12-21T21:12:21Z"), "Test vault 4", false, 0, Map.of(), "uvfMetadata3", "uvfKeySet3", "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3");

		vaultResource.createOrUpdate(vaultId, vaultDto, minio, aws);

		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(1)).keycloakPrepareVault(vaultId.toString(), minio, aws);
		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(1)).keycloakGrantAccessToVault(vaultId.toString(), "alice", "pesto", false);
	}

	@Test
	public void testAddUser() {
		vaultResource.addUser(vaultId, "alice", VaultAccess.Role.MEMBER);
		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(1)).keycloakGrantAccessToVault(vaultId.toString(), "alice", "pesto", false);
	}

	@Test
	public void testAddUserFailing() {
		assertThrows(NotFoundException.class, () -> vaultResource.addUser(vaultId, "bob", VaultAccess.Role.MEMBER));
		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(0)).keycloakGrantAccessToVault(vaultId.toString(), "bob", "pesto", false);
	}

	@Test
	public void testAddGroup() {
		vaultResource.addGroup(vaultId, "good cops", VaultAccess.Role.MEMBER);
		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(1)).keycloakGrantAccessToVault(vaultId.toString(), "good cops", "pesto", true);
	}

	@Test
	public void testAddGroupFailing() {
		assertThrows(NotFoundException.class, () -> vaultResource.addGroup(vaultId, "bad cops", VaultAccess.Role.MEMBER));
		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(0)).keycloakGrantAccessToVault(vaultId.toString(), "bad cops", "pesto", true);
	}

	@Test
	public void testRemoveAuthority() {
		vaultResource.removeAuthority(vaultId, "good cops");
		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(1)).keycloakRemoveAccessToVault(vaultId.toString(), "good cops", "pesto", true);
	}

	@Test
	public void testRemoveAuthorityFailing() {
		assertThrows(NotFoundException.class, () -> vaultResource.removeAuthority(vaultId, "bad cops"));
		Mockito.verify(keycloakCryptomatorVaultsHelper, Mockito.times(0)).keycloakRemoveAccessToVault(vaultId.toString(), "bad cops", "pesto", true);
	}
}