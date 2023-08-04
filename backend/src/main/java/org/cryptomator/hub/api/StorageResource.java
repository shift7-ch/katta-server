package org.cryptomator.hub.api;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.admin.MinioAdminClient;
import io.minio.errors.*;
import io.quarkus.qute.Qute;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/storage")
public class StorageResource {

    @Inject
    JsonWebToken jwt;

    @PUT
    @Path("/")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it.")
    @APIResponse(responseCode = "200", description = "uploaded storage configuration")
    public void createBucketAndPolicy(StorageDto dto
    ) throws ServerException, InsufficientDataException, ErrorResponseException, URISyntaxException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {


        final String rolePolicy = rolePolicy(dto.vaultId, dto.s3type);

        makeMinioBucket(
                dto.vaultId,
                rolePolicy,
                dto.scheme,
                dto.hostname,
                dto.port,
                dto.accessKeyId,
                dto.secretKey,
                dto.vaultConfigToken,
                dto.rootDirHash

        );
    }

    public static String rolePolicy(final String bucketName, String prefix) throws IOException {

        // TODO add read/write permissions GetObject etc.
        String templateF = String.format("/cipherduck/s3_templates/%sPolicyTemplate.json", prefix);
        String template = IOUtils.toString(StorageResource.class.getResourceAsStream(templateF), StandardCharsets.UTF_8);
        // TODO do we want to use Qute as suggested by Quarkus?
        return Qute.fmt(template, Stream.of(
                        new AbstractMap.SimpleEntry<>("bucketName", bucketName)
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private static void makeMinioBucket(final String vaultId, final String rolePolicy, final String scheme, final String hostname, final int port, final String minioAdminUsername, final String minioAdminPassword,
                                        final String vaultConfigToken,
                                        final String rootDirHash) throws URISyntaxException, ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        final String minioEndpoint = new URI(
                scheme,
                null,
                hostname,
                port,
                null,
                null,
                null
        ).toURL().toString();
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(minioEndpoint)
                        .credentials(minioAdminUsername, minioAdminPassword)
                        .build();
        final MinioAdminClient minioAdminClient = MinioAdminClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAdminUsername, minioAdminPassword)
                .build();
        minioClient.makeBucket(
                MakeBucketArgs.builder()
                        .bucket(vaultId)
                        .build());

        minioAdminClient.addCannedPolicy(vaultId, rolePolicy);

        //        zip.file('vault.cryptomator', this.vaultConfigToken);
        //        zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));


        final File tmpFile = Files.createTempFile("tempfiles", ".tmp").toFile();
        tmpFile.deleteOnExit();
        FileUtils.writeStringToFile(tmpFile, vaultConfigToken, StandardCharsets.UTF_8);
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(vaultId)
                        .object("vault.cryptomator")
                        .filename(tmpFile.toString())
                        .build());


        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(vaultId)
                        .object(String.format("d/%s/%s/", rootDirHash.substring(0, 2), rootDirHash.substring(2)))
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build());
    }
}
