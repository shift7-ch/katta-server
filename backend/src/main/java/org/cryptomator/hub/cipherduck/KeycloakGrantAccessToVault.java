package org.cryptomator.hub.cipherduck;

import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.SyncerConfig;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

			final UserResource userResource = realm.users().get(userId);
			final UserRepresentation ur = userResource.toRepresentation();

			// TODO https://github.com/chenkins/cipherduck-hub/issues/4 do we want to use user attributes? Or should we use groups/....? What happens in federation setting - do attributes come from AD etc. and will there be no conflict?
			Map<String, List<String>> attributes = ur.getAttributes();
			if (attributes == null) {
				attributes = new HashMap<>();
			}
			attributes.put("vault", Stream.concat(attributes.getOrDefault("vault", Collections.EMPTY_LIST).stream(), Stream.of(vaultId)).toList());
			ur.setAttributes(attributes);
			userResource.update(ur);


			// create client scope <vaultId> (if necessary)
			if (realm.clientScopes().findAll().stream().filter(clientScopeRepresentation -> clientScopeRepresentation.getId().equals(vaultId)).collect(Collectors.toList()).isEmpty()) {

				ClientScopeRepresentation vaultClientScope = new ClientScopeRepresentation();
				vaultClientScope.setId(vaultId);
				vaultClientScope.setName(vaultId);
				vaultClientScope.setDescription(String.format("Client scope for vault %s", vaultId));
				vaultClientScope.setAttributes(new HashMap<>());
				vaultClientScope.setProtocol("openid-connect");


				ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
				protocolMapper.setName(String.format("Hard-coded mapper for vault %s", vaultId));
				protocolMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
				protocolMapper.setProtocol("openid-connect");

				Map<String, String> config = new HashMap<>();
				config.put("jsonType.label", "String");

				// TODO https://github.com/chenkins/cipherduck-hub/issues/41 do we need only access token?
				config.put("userinfo.token.claim", "true");
				config.put("id.token.claim", "true");
				config.put("access.token.claim", "true");
				config.put("access.tokenResponse.claim", "false");

				// TODO https://github.com/chenkins/cipherduck-hub/issues/41 support for AWS etc. - we need storage type info, however, we currently do not have it in access grant - should we make it a separate REST endpoint in StorageResource?
				config.put("claim.name", "aud");
				config.put("claim.value", vaultId);

				protocolMapper.setConfig(config);
				vaultClientScope.setProtocolMappers(Collections.singletonList(protocolMapper));

				Response response = realm.clientScopes().create(vaultClientScope);
				if (response.getStatus() != 201) {
					throw new RuntimeException(String.format("Failed to create client for vault %s. %s", vaultId, response.getStatusInfo().getReasonPhrase()));
				}
			}

			// add client scope to "cryptomator" and cryptomatorhub" clients
			for (String clientId : clientIds) {
				List<ClientRepresentation> byClientId = realm.clients().findByClientId(clientId);
				if (byClientId.size() != 1) {
					throw new RuntimeException(String.format("There are %s clients with clientId %s, expected to found exactly one.", byClientId.size(), clientId));
				}
				ClientRepresentation cryptomatorClient = byClientId.get(0);
				realm.clients().get(cryptomatorClient.getId()).addOptionalClientScope(vaultId);
			}


			// create role <vaultId> (if necessary)
			if (realm.roles().list().stream().filter(role -> role.getName().equals(vaultId)).collect(Collectors.toList()).isEmpty()) {
				RoleRepresentation vaultRole = new RoleRepresentation();
				vaultRole.setName(vaultId);
				vaultRole.setDescription(String.format("Role for vault %s", vaultId));
				realm.roles().create(vaultRole);
			}

			// scope the client scope to the vault role
			realm.clientScopes().get(vaultId).getScopeMappings().realmLevel().add(Collections.singletonList(realm.roles().get(vaultId).toRepresentation()));

			// add role to user
			realm.users().get(userId).roles().realmLevel().add(Collections.singletonList(realm.roles().get(vaultId).toRepresentation()));


			// TODO https://github.com/chenkins/cipherduck-hub/issues/41 which permissions required for syncer -> minimal set
			// TODO https://github.com/chenkins/cipherduck-hub/issues/41 proof of access to vault in grantAccess()?
		}
	}
}
