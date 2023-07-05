package org.cryptomator.hub.cipherduck;

import org.cryptomator.hub.SyncerConfig;
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


    public static void keycloakGrantAccessToVault(final SyncerConfig syncerConfig, final String vaultId, final String userId) {
        try (final Keycloak keycloak = Keycloak.getInstance(syncerConfig.getKeycloakUrl(), syncerConfig.getKeycloakRealm(), syncerConfig.getUsername(), syncerConfig.getPassword(), syncerConfig.getKeycloakClientId())) {

            final RealmResource realm = keycloak.realm(syncerConfig.getKeycloakRealm());
            final UserResource userResource = realm.users().get(userId);
            final UserRepresentation ur = userResource.toRepresentation();

            // TODO do we want to use user attributes? Or should we use groups/....? What happens in federation setting - do attributes come from AD etc. and will there be no conflict?
            Map<String, List<String>> attributes = ur.getAttributes();
            if(attributes == null){
                attributes = new HashMap<>();
            }
            attributes.put("vaults", Stream.concat(attributes.getOrDefault("vaults", Collections.EMPTY_LIST).stream(), Stream.of(vaultId)).toList());
            ur.setAttributes(attributes);
            userResource.update(ur);
        }
    }
}
