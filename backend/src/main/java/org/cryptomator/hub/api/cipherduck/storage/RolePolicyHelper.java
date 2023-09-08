package org.cryptomator.hub.api.cipherduck.storage;

import io.quarkus.qute.Qute;
import org.apache.commons.io.IOUtils;
import org.cryptomator.hub.api.cipherduck.StorageResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RolePolicyHelper {
    public static String rolePolicy(final String bucketName, String prefix) throws IOException {

        // TODO add read/write permissions GetObject etc.
        String templateF = String.format("/cipherduck/s3_templates/%sPolicyTemplate.json", prefix);
        String template = IOUtils.toString(StorageResource.class.getResourceAsStream(templateF), StandardCharsets.UTF_8);
        // TODO do we want to use Qute as suggested by Quarkus?
        return Qute.fmt(template, Stream.of(
                        Map.entry("bucketName", bucketName)
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public static String trustPolicy(final String vaultId, final String oidcProvider, final String prefix) throws IOException {

        // TODO https://github.com/chenkins/cipherduck-hub/issues/15 naming convention: vault name or vault id?
        String templateF = String.format("/cipherduck/s3_templates/%sTrustPolicyTemplate.json", prefix);
        String template = IOUtils.toString(StorageResource.class.getResourceAsStream(templateF), StandardCharsets.UTF_8);
        // TODO do we want to use Qute as suggested by Quarkus?
        return Qute.fmt(template, Stream.of(
                        new AbstractMap.SimpleEntry<>("vaultId", vaultId),
                        new AbstractMap.SimpleEntry<>("oidcProvider", oidcProvider)
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
