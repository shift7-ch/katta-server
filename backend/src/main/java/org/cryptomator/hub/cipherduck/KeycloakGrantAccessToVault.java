package org.cryptomator.hub.cipherduck;

import org.cryptomator.hub.SyncerConfig;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class KeycloakGrantAccessToVault {
    private static final Logger LOG = Logger.getLogger(KeycloakGrantAccessToVault.class);

    public static void keycloakGrantAccessToVault(final SyncerConfig syncerConfig, final String vaultId, final String userId) {
        LOG.info("keycloakURL=" + syncerConfig.getKeycloakUrl());
        // N.B. quarkus has no means to provide empty string as value, interpreted as no value, see https://github.com/quarkusio/quarkus/issues/2765
        // TODO better solution than using sentinel string "empty"?
        if ("empty".equals(syncerConfig.getKeycloakUrl())) {
            LOG.error(String.format("Could not grant access to vault %s for user %s as keycloak URL is not defined.", vaultId, userId));
            return;
        }
        try (final Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {

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
        }
    }
}
