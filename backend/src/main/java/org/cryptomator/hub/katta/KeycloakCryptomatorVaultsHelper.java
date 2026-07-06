package org.cryptomator.hub.katta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.api.VaultResource;
import org.cryptomator.hub.entities.Vault;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakCryptomatorVaultsHelper {

	private static final Logger LOG = Logger.getLogger(KeycloakCryptomatorVaultsHelper.class);

	@Inject
	Keycloak keycloak;

	@ConfigProperty(name = "hub.keycloak.realm")
	protected String keycloakRealm;

	public void keycloakPrepareVault(final String clientId, final String vaultId, final Boolean minio, final Boolean aws) {
		keycloakPrepareVault(clientId, vaultId, getKeycloak(), keycloakRealm, minio, aws);
	}

	public void keycloakGrantAccessToVault(final String vaultId, final String userOrGroupId, final String clientId, final boolean isGroup) {
		keycloakGrantAccessToVault(vaultId, userOrGroupId, clientId, getKeycloak(), keycloakRealm, isGroup);
	}

	public void keycloakRemoveAccessToVault(final String vaultId, final String userOrGroupId, final String clientId, final boolean isGroup) {
		keycloakRemoveAccessToVault(vaultId, userOrGroupId, clientId, getKeycloak(), keycloakRealm, isGroup);
	}

	// TODO review: this loop might not be safe enough to run in production - should we just disable this feature or remove from code entirely?
	// Deleting the cryptomatorvaults client also deletes the client roles under the client, however, the client scopes are at the realm level and will not be removed by this procedure.
	// Although safe, this can quickly become a mess in developing scenarios.
	public void keycloakCleanupDanglingCryptomatorVaultsRoles(final String clientId, final Vault.Repository vaultRepo) {
		Set<String> existingVaultIds = vaultRepo.findAll().stream().map(VaultResource.VaultDto::fromEntity).map(vdto -> vdto.id().toString()).collect(Collectors.toSet());
		// https://www.keycloak.org/docs-api/21.1.1/rest-api
		final RealmResource realm = getKeycloak().realm(keycloakRealm);

		List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
		if (byClientId.size() != 1) {
			throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
		}
		final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.getFirst();
		final ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());

		for (final RoleRepresentation roleRepresentation : cryptomatorVaultsClientResource.roles().list()) {
			final String vaultId = roleRepresentation.getName();
			if (!existingVaultIds.contains(vaultId)) {
				cryptomatorVaultsClientResource.roles().deleteRole(vaultId);
				try {
					realm.clientScopes().get(vaultId).remove();
				} catch (ClientWebApplicationException e) {
					if (LOG.isInfoEnabled()) {
						LOG.info(String.format("Could not delete client scope %s", vaultId), e);
					}
				}
			}
		}
	}

	protected static void keycloakPrepareVault(final String clientId, final String vaultId, final Keycloak keycloak, final String keycloakRealm, final Boolean minio, final Boolean aws) {
		// https://www.keycloak.org/docs-api/21.1.1/rest-api
		final RealmResource realm = keycloak.realm(keycloakRealm);


		final List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
		if (byClientId.size() != 1) {
			throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
		}
		final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.getFirst();
		final ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());

		// create client scope <vaultId> (if necessary)
		ensureClientScopeForVaultExists(vaultId, realm);

		// add client scope to "cryptomatorvaults" client
		// -> requires role_manage-clients
		cryptomatorVaultsClientResource.addOptionalClientScope(vaultId);

		// create client role <vaultId> (if necessary)
		// -> requires role_manage-clients
		if (cryptomatorVaultsClientResource.roles().list().stream().map(RoleRepresentation::getName).noneMatch(vaultId::equals)) {
			final RoleRepresentation vaultRole = new RoleRepresentation();
			vaultRole.setName(vaultId);
			vaultRole.setDescription(String.format("Role for vault %s", vaultId));
			vaultRole.setClientRole(true);

			cryptomatorVaultsClientResource.roles().create(vaultRole);
		}

		// scope the client scope to the client role for the vault
		// IMPORTANT: if there is no role scope mapping defined, each user is permitted to use this client scope. If there are role scope mappings defined, the user must be a member of at least one of the roles.
		realm.clientScopes().get(vaultId).getScopeMappings().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));

		// add protocol mappers
		final ClientScopeResource clientScopeResource = realm.clientScopes().get(vaultId);
		if (minio != null) {
			final ProtocolMapperRepresentation minioProtocolMapper = minioProtocolMapper(vaultId);
			final Optional<ProtocolMapperRepresentation> mapper = clientScopeResource.getProtocolMappers().getMappers().stream().filter(m -> m.getName().contains("MinIO")).findFirst();
			if (mapper.isEmpty()) {
				if (minio) {
					clientScopeResource.getProtocolMappers().createMapper(List.of(minioProtocolMapper));
				}
			} else {
				final String mapperId = mapper.get().getId();
				if (minio) {
					minioProtocolMapper.setId(mapperId);
					clientScopeResource.getProtocolMappers().update(mapperId, minioProtocolMapper);
				} else {
					clientScopeResource.getProtocolMappers().delete(mapperId);
				}
			}
		}
		if (aws != null) {
			final ProtocolMapperRepresentation awsProtocolMapper = awsProtocolMapper(vaultId);
			final Optional<ProtocolMapperRepresentation> mapper = clientScopeResource.getProtocolMappers().getMappers().stream().filter(m -> m.getName().contains("AWS")).findFirst();
			if (mapper.isEmpty()) {
				if (aws) {
					clientScopeResource.getProtocolMappers().createMapper(List.of(awsProtocolMapper));
				}
			} else {
				final String mapperId = mapper.get().getId();
				if (aws) {
					awsProtocolMapper.setId(mapperId);
					clientScopeResource.getProtocolMappers().update(mapperId, awsProtocolMapper);
				} else {
					clientScopeResource.getProtocolMappers().delete(mapperId);
				}
			}
		}
	}

	protected static ProtocolMapperRepresentation awsProtocolMapper(final String vaultId) {
		final ProtocolMapperRepresentation awsProtocolMapper = new ProtocolMapperRepresentation();
		awsProtocolMapper.setName(String.format("Hard-coded mapper for vault %s (AWS)", vaultId));
		awsProtocolMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
		awsProtocolMapper.setProtocol("openid-connect");

		Map<String, String> awsConfig = new HashMap<>();
		awsConfig.put("jsonType.label", "JSON");

		awsConfig.put("userinfo.token.claim", "false");
		awsConfig.put("id.token.claim", "false");
		awsConfig.put("access.token.claim", "true");
		awsConfig.put("access.tokenResponse.claim", "false");

		awsConfig.put("claim.name", "https://aws\\.amazon\\.com/tags");
		awsConfig.put("claim.value", String.format("{\"principal_tags\":{\"%s\":[\"\"]},\"transitive_tag_keys\":[\"%s\"]}", vaultId, vaultId));

		awsProtocolMapper.setConfig(awsConfig);
		return awsProtocolMapper;
	}

	protected static ProtocolMapperRepresentation minioProtocolMapper(final String vaultId) {
		final ProtocolMapperRepresentation minioProtocolMapper = new ProtocolMapperRepresentation();
		minioProtocolMapper.setName(String.format("Hard-coded mapper for vault %s (MinIO)", vaultId));
		minioProtocolMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
		minioProtocolMapper.setProtocol("openid-connect");

		Map<String, String> minioConfig = new HashMap<>();
		minioConfig.put("jsonType.label", "String");

		minioConfig.put("userinfo.token.claim", "false");
		minioConfig.put("id.token.claim", "false");
		minioConfig.put("access.token.claim", "true");
		minioConfig.put("access.tokenResponse.claim", "false");

		// exhaustive list of jwt claims evaluated in MinIO: https://min.io/docs/minio/linux/administration/identity-access-management/policy-based-access-control.html#policy-variables
		// let's use client_id, as aud etc. are already use by standard mappers
		minioConfig.put("claim.name", "client_id");
		minioConfig.put("claim.value", vaultId);

		minioProtocolMapper.setConfig(minioConfig);
		return minioProtocolMapper;
	}

	protected static void keycloakGrantAccessToVault(final String vaultId, final String userOrGroupId, final String clientId, final Keycloak keycloak, final String keycloakRealm, final boolean isGroup) {
		// https://www.keycloak.org/docs-api/21.1.1/rest-api
		final RealmResource realm = keycloak.realm(keycloakRealm);

		final List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
		if (byClientId.size() != 1) {
			throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
		}
		final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.getFirst();
		final ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());
		
		// add client role to user/group
		// -> requires role_manage-users
		if (!isGroup) {
			realm.users().get(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
		} else {
			realm.groups().group(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
		}
	}

	private static void ensureClientScopeForVaultExists(final String vaultId, final RealmResource realm) {
		if (realm.clientScopes().findAll().stream().map(ClientScopeRepresentation::getId).noneMatch(vaultId::equals)) {
			ClientScopeRepresentation vaultClientScope = new ClientScopeRepresentation();
			vaultClientScope.setId(vaultId);
			vaultClientScope.setName(vaultId);
			vaultClientScope.setDescription(String.format("Client scope for vault %s", vaultId));
			vaultClientScope.setAttributes(new HashMap<>());
			vaultClientScope.setProtocol("openid-connect");

			try (Response response = realm.clientScopes().create(vaultClientScope)) {
				if (response.getStatus() != 201) {
					throw new RuntimeException(String.format("Failed to create client for vault %s. %s", vaultId, response.getStatusInfo().getReasonPhrase()));
				}
			}
		}
	}

	protected static void keycloakRemoveAccessToVault(final String vaultId, final String userOrGroupId, final String clientId, final Keycloak keycloak, final String keycloakRealm, boolean isGroup) {
		// https://www.keycloak.org/docs-api/21.1.1/rest-api
		final RealmResource realm = keycloak.realm(keycloakRealm);

		// add client scope to "cryptomatorvaults" client
		// -> requires role_manage-clients
		final List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
		if (byClientId.size() != 1) {
			throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
		}
		final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.getFirst();
		final ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());
		cryptomatorVaultsClientResource.addOptionalClientScope(vaultId);

		// remove client role from user/group
		// -> requires role_manage-users
		if (!isGroup) {
			realm.users().get(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).remove(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
		} else {
			realm.groups().group(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).remove(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
		}
	}

	protected Keycloak getKeycloak() {
		return keycloak;
	}
}
