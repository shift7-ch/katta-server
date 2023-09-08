package org.cryptomator.hub.api.cipherduck.storage;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.admin.MinioAdminClient;
import io.minio.errors.*;
import org.apache.commons.io.IOUtils;
import org.cryptomator.hub.api.cipherduck.StorageConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.cryptomator.hub.api.cipherduck.storage.RolePolicyHelper.rolePolicy;

public class MinIOStorage {
    public static void makeMinioBucket(
            final String vaultId,
            final String bucketName,
            final StorageConfig storageConfig,
            final String vaultConfigToken,
            final String rootDirHash
    ) throws URISyntaxException, ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {

        final String rolePolicy = rolePolicy(bucketName, storageConfig.s3Type());
        final String minioEndpoint = new URI(
                storageConfig.jwe().scheme().get(),
                null,
                storageConfig.jwe().hostname().get(),
                storageConfig.jwe().port().get(),
                null,
                null,
                null
        ).toURL().toString();
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(minioEndpoint)
                        .credentials(storageConfig.adminAccessKeyId(), storageConfig.adminSecretKey())
                        .build();
        final MinioAdminClient minioAdminClient = MinioAdminClient.builder()
                .endpoint(minioEndpoint)
                .credentials(storageConfig.adminAccessKeyId(), storageConfig.adminSecretKey())
                .build();
        minioClient.makeBucket(
                MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());

        minioAdminClient.addCannedPolicy(vaultId, rolePolicy);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object("vault.cryptomator")
                        .stream(IOUtils.toInputStream(vaultConfigToken), vaultConfigToken.length(), -1)
                        .build());


        // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
        //        zip.file('vault.cryptomator', this.vaultConfigToken);
        //        zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(String.format("d/%s/%s/", rootDirHash.substring(0, 2), rootDirHash.substring(2)))
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build());
    }
}
