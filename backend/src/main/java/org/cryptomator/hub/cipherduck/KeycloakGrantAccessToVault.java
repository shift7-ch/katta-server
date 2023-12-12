package org.cryptomator.hub.cipherduck;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.SyncerConfig;
import org.cryptomator.hub.api.VaultResource;
import org.cryptomator.hub.api.cipherduck.StorageProfileDto;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.Vault;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeycloakGrantAccessToVault {

	private static final Logger LOG = Logger.getLogger(KeycloakGrantAccessToVault.class);

	public static void keycloakPrepareVault(final SyncerConfig syncerConfig, final String vaultId, final StorageProfileDto storageConfig, final String userOrGroupId, final String clientId) {

		// N.B. quarkus has no means to provide empty string as value, interpreted as no value, see https://github.com/quarkusio/quarkus/issues/2765
		// TODO review better solution than using sentinel string "empty"?
		if ("empty".equals(syncerConfig.getKeycloakUrl())) {
			LOG.error(String.format("Could not grant access to vault %s for user %s as keycloak URL is not defined.", vaultId, userOrGroupId));
			return;
		}

		try (final Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {

			// https://www.keycloak.org/docs-api/21.1.1/rest-api
			final RealmResource realm = keycloak.realm(syncerConfig.getKeycloakRealm());

			final boolean minio = storageConfig.stsRoleArn()!=null && storageConfig.stsRoleArn2() ==null;
			final boolean aws = storageConfig.stsRoleArn() !=null && storageConfig.stsRoleArn2() != null;

			ClientScopeResource clientScopeResource = realm.clientScopes().get(vaultId);

			if (minio) {
				ProtocolMapperRepresentation minioProtocolMapper = new ProtocolMapperRepresentation();
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

				clientScopeResource.getProtocolMappers().createMapper(Arrays.asList(minioProtocolMapper));
			}
			if (aws) {
				ProtocolMapperRepresentation awsProtocolMapper = new ProtocolMapperRepresentation();
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

				clientScopeResource.getProtocolMappers().createMapper(Arrays.asList(awsProtocolMapper));
			}
		}
	}

	public static void keycloakGrantAccessToVault(final SyncerConfig syncerConfig, final String vaultId, final String userOrGroupId, final String clientId) {
		// N.B. quarkus has no means to provide empty string as value, interpreted as no value, see https://github.com/quarkusio/quarkus/issues/2765
		// TODO review better solution than using sentinel string "empty"?
		if ("empty".equals(syncerConfig.getKeycloakUrl())) {
			LOG.error(String.format("Could not grant access to vault %s for user %s as keycloak URL is not defined.", vaultId, userOrGroupId));
			return;
		}

		var group = Group.<Group>findByIdOptional(userOrGroupId);
		final boolean isGroup = group.isPresent();

		try (final Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {

			// https://www.keycloak.org/docs-api/21.1.1/rest-api
			final RealmResource realm = keycloak.realm(syncerConfig.getKeycloakRealm());

			List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
			if (byClientId.size() != 1) {
				throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
			}
			final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.get(0);
			ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());

			// create client scope <vaultId> (if necessary)
			if (realm.clientScopes().findAll().stream().map(clientScopeRepresentation -> clientScopeRepresentation.getId()).noneMatch(vaultId::equals)) {
				ClientScopeRepresentation vaultClientScope = new ClientScopeRepresentation();
				vaultClientScope.setId(vaultId);
				vaultClientScope.setName(vaultId);
				vaultClientScope.setDescription(String.format("Client scope for vault %s", vaultId));
				vaultClientScope.setAttributes(new HashMap<>());
				vaultClientScope.setProtocol("openid-connect");

				Response response = realm.clientScopes().create(vaultClientScope);
				if (response.getStatus() != 201) {
					throw new RuntimeException(String.format("Failed to create client for vault %s. %s", vaultId, response.getStatusInfo().getReasonPhrase()));
				}
			}

			// add client scope to "cryptomatorvaults" client
			// -> requires role_manage-clients
			cryptomatorVaultsClientResource.addOptionalClientScope(vaultId);


			// create client role <vaultId> (if necessary)
			// -> requires role_manage-clients
			if (cryptomatorVaultsClientResource.roles().list().stream().map(role -> role.getName()).noneMatch(vaultId::equals)) {
				RoleRepresentation vaultRole = new RoleRepresentation();
				vaultRole.setName(vaultId);
				vaultRole.setDescription(String.format("Role for vault %s", vaultId));
				vaultRole.setClientRole(true);

				cryptomatorVaultsClientResource.roles().create(vaultRole);
			}

			// scope the client scope to the client role for the vault
			realm.clientScopes().get(vaultId).getScopeMappings().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));


			// add client role to user/group
			// -> requires role_manage-users
			if (!isGroup) {
				realm.users().get(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
			} else {
				realm.groups().group(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).add(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
			}
		}
	}


	public static void keycloakRemoveAccessToVault(final SyncerConfig syncerConfig, final String vaultId, final String userOrGroupId, final String clientId) {
		// N.B. quarkus has no means to provide empty string as value, interpreted as no value, see https://github.com/quarkusio/quarkus/issues/2765
		// TODO review better solution than using sentinel string "empty"?
		if ("empty".equals(syncerConfig.getKeycloakUrl())) {
			LOG.error(String.format("Could not grant access to vault %s for user %s as keycloak URL is not defined.", vaultId, userOrGroupId));
			return;
		}

		final boolean isGroup = Group.findByIdOptional(userOrGroupId).isPresent();

		try (final Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {

			// https://www.keycloak.org/docs-api/21.1.1/rest-api
			final RealmResource realm = keycloak.realm(syncerConfig.getKeycloakRealm());

			// add client scope to "cryptomatorvaults" client
			// -> requires role_manage-clients
			List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
			if (byClientId.size() != 1) {
				throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
			}
			final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.get(0);
			ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());
			cryptomatorVaultsClientResource.addOptionalClientScope(vaultId);

			// remove client role from user/group
			// -> requires role_manage-users
			if (!isGroup) {
				realm.users().get(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).remove(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
			} else {
				realm.groups().group(userOrGroupId).roles().clientLevel(cryptomatorVaultsClientRepresentation.getId()).remove(List.of(cryptomatorVaultsClientResource.roles().get(vaultId).toRepresentation()));
			}
		}
	}

	// TODO review: this loop might not be safe enough to run in production - should we just disable this feature or remove from code entirely?
	// Deleting the cryptomatorvaults client also deletes the client roles under the client, however, the client scopes are at the realm level and will not be removed by this procedure.
	// Although safe, this can quickly become a mess in developing scenarios.
	public static void keycloakCleanupDanglingCryptomatorVaultsRoles(final SyncerConfig syncerConfig, final String clientId) {
		Set<String> existingVaultIds = Vault.findAll().<Vault>stream().map(VaultResource.VaultDto::fromEntity).map(vdto -> vdto.id().toString()).collect(Collectors.toSet());
		try (final Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {

			// https://www.keycloak.org/docs-api/21.1.1/rest-api
			final RealmResource realm = keycloak.realm(syncerConfig.getKeycloakRealm());

			List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
			if (byClientId.size() != 1) {
				throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
			}
			final ClientRepresentation cryptomatorVaultsClientRepresentation = byClientId.get(0);
			ClientResource cryptomatorVaultsClientResource = realm.clients().get(cryptomatorVaultsClientRepresentation.getId());

			for (RoleRepresentation roleRepresentation : cryptomatorVaultsClientResource.roles().list()) {
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
	}
}
