package org.cryptomator.hub.api.cipherduck;

import io.minio.errors.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.cryptomator.hub.api.cipherduck.storage.AWSStorage.makeAWSBucket;
import static org.cryptomator.hub.api.cipherduck.storage.MinIOStorage.makeMinioBucket;

@Path("/storage")
public class StorageResource {
    private static final Logger LOG = Logger.getLogger(StorageResource.class);


    @Inject
    BackendsConfig backendsConfig;


    @PUT
    @Path("/")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it.")
    @APIResponse(responseCode = "200", description = "uploaded storage configuration")
    public void createBucketAndPolicy(StorageDto dto) throws ServerException, InsufficientDataException, ErrorResponseException, URISyntaxException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        Map<String, StorageConfig> storageConfigs = backendsConfig.backends().stream().collect(Collectors.toMap(StorageConfig::id, Function.identity()));
        StorageConfig storageConfig = storageConfigs.get(dto.storageConfigId());

        // TODO https://github.com/chenkins/cipherduck-hub/issues/15 configurable bucket prefix
        String bucketName = "cipherduck" + dto.vaultId();


        switch (storageConfig.s3Type()) {
            // TODO https://github.com/chenkins/cipherduck-hub/issues/3 enum for s3type?
            case "minio":
                // TODO https://github.com/chenkins/cipherduck-hub/issues/3 interface for makeBucket?
                makeMinioBucket(
                        dto.vaultId(),
                        bucketName,
                        storageConfig,
                        dto.vaultConfigToken(),
                        dto.rootDirHash()
                );
                break;
            case "aws":
                makeAWSBucket(
                        dto.vaultId(),
                        bucketName,
                        storageConfig,
                        dto.vaultConfigToken(),
                        dto.rootDirHash()
                );
                break;
            default:
                throw new IllegalStateException(String.format("Unknown S3 type {}", storageConfig.s3Type()));
        }
    }


}
