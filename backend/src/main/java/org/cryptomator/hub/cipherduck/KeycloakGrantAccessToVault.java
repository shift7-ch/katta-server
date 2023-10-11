package org.cryptomator.hub.cipherduck;

import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.SyncerConfig;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakGrantAccessToVault {
	private static final Logger LOG = Logger.getLogger(KeycloakGrantAccessToVault.class);


	public static void keycloakGrantAccessToVault(final SyncerConfig syncerConfig, final String vaultId, final String userId, final List<String> clientIds) {
		LOG.info("keycloakURL=" + syncerConfig.getKeycloakUrl());
		// N.B. quarkus has no means to provide empty string as value, interpreted as no value, see https://github.com/quarkusio/quarkus/issues/2765
		// TODO better solution than using sentinel string "empty"?
		if ("empty".equals(syncerConfig.getKeycloakUrl())) {
			LOG.error(String.format("Could not grant access to vault %s for user %s as keycloak URL is not defined.", vaultId, userId));
			return;
		}
		try (final Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {

			// https://www.keycloak.org/docs-api/21.1.1/rest-api
			final RealmResource realm = keycloak.realm(syncerConfig.getKeycloakRealm());


			// create client scope <vaultId> (if necessary)
			if (realm.clientScopes().findAll().stream().filter(clientScopeRepresentation -> clientScopeRepresentation.getId().equals(vaultId)).collect(Collectors.toList()).isEmpty()) {

				ClientScopeRepresentation vaultClientScope = new ClientScopeRepresentation();
				vaultClientScope.setId(vaultId);
				vaultClientScope.setName(vaultId);
				vaultClientScope.setDescription(String.format("Client scope for vault %s", vaultId));
				vaultClientScope.setAttributes(new HashMap<>());
				vaultClientScope.setProtocol("openid-connect");


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


				// TODO https://github.com/chenkins/cipherduck-hub/issues/41 we would need only one of them... invoke as part of make bucket endpoint?
				vaultClientScope.setProtocolMappers(Arrays.asList(minioProtocolMapper, awsProtocolMapper));

				Response response = realm.clientScopes().create(vaultClientScope);
				if (response.getStatus() != 201) {
					throw new RuntimeException(String.format("Failed to create client for vault %s. %s", vaultId, response.getStatusInfo().getReasonPhrase()));
				}
			}

			// add client scope to "cryptomator" and cryptomatorhub" clients
			// -> requires role_manage-clients
			for (String clientId : clientIds) {
				List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
				if (byClientId.size() != 1) {
					throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
				}
				ClientRepresentation cryptomatorClient = byClientId.get(0);
				realm.clients().get(cryptomatorClient.getId()).addOptionalClientScope(vaultId);
			}


			// create role <vaultId> (if necessary)
			// -> requires role_manage-realm
			if (realm.roles().list().stream().filter(role -> role.getName().equals(vaultId)).collect(Collectors.toList()).isEmpty()) {
				RoleRepresentation vaultRole = new RoleRepresentation();
				vaultRole.setName(vaultId);
				vaultRole.setDescription(String.format("Role for vault %s", vaultId));
				realm.roles().create(vaultRole);
			}

			// scope the client scope to the vault role
			realm.clientScopes().get(vaultId).getScopeMappings().realmLevel().add(Collections.singletonList(realm.roles().get(vaultId).toRepresentation()));

			// add role to user
			// -> requires role_manage-users
			realm.users().get(userId).roles().realmLevel().add(Collections.singletonList(realm.roles().get(vaultId).toRepresentation()));


			// TODO https://github.com/chenkins/cipherduck-hub/issues/41 which permissions required for syncer -> minimal set
			// TODO https://github.com/chenkins/cipherduck-hub/issues/41 proof of access to vault in grantAccess()?
		}
	}
}
