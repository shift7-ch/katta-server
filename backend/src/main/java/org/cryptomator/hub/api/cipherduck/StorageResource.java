package org.cryptomator.hub.api.cipherduck;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    private static final Logger LOG = Logger.getLogger(StorageResource.class);

    @Inject
    JsonWebToken jwt;


    // TODO https://github.com/chenkins/cipherduck-hub/issues/3 configuration
    @ConfigProperty(name = "hub.s3.minio.accessKeyId")
    String accessKeyId_minio;

    @ConfigProperty(name = "hub.s3.minio.secretKey")
    String secretKey_minio;


    @ConfigProperty(name = "hub.s3.aws.accessKeyId")
    String accessKeyId_aws;

    @ConfigProperty(name = "hub.s3.aws.secretKey")
    String secretKey_aws;


    @PUT
    @Path("/")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it.")
    @APIResponse(responseCode = "200", description = "uploaded storage configuration")
    public void createBucketAndPolicy(StorageDto dto
    ) throws ServerException, InsufficientDataException, ErrorResponseException, URISyntaxException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        final String rolePolicy = rolePolicy(dto.bucketName(), dto.s3type());
        // TODO test config?
        if (!dto.protocol().equals("s3")) {
            throw new IllegalStateException(String.format("Unknown protocol {}", dto.protocol()));
        }
        switch (dto.s3type()) {
            // TODO enum for s3type?
            case "minio":
                makeMinioBucket(
                        // TODO pass full dto instead?
                        dto.vaultId(),
                        dto.bucketName(),
                        rolePolicy,
                        dto.scheme(),
                        dto.hostname(),
                        dto.port(),

                        accessKeyId_minio,
                        secretKey_minio,

                        dto.vaultConfigToken(),
                        dto.rootDirHash()
                );
                break;
            case "aws":
                makeAWSBucket(
                        // TODO pass full dto instead?
                        dto.vaultId(),
                        dto.bucketName(),
                        dto.oidcProvider(),
                        dto.region(),
                        rolePolicy,
                        dto.scheme(),
                        dto.hostname(),
                        dto.port(),

                        accessKeyId_aws,
                        secretKey_aws,

                        dto.vaultConfigToken(),
                        dto.rootDirHash());
                break;
            default:
                throw new IllegalStateException(String.format("Unknown S3 type {}", dto.s3type()));
        }
    }

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

    private static void makeMinioBucket(
            final String vaultId,
            final String bucketName,
            final String rolePolicy,
            final String scheme,
            final String hostname,
            final int port,
            final String accessKeyId,
            final String secretKey,
            final String vaultConfigToken,
            final String rootDirHash
    ) throws URISyntaxException, ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
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
                        .credentials(accessKeyId,secretKey)
                        .build();
        final MinioAdminClient minioAdminClient = MinioAdminClient.builder()
                .endpoint(minioEndpoint)
                .credentials(accessKeyId,secretKey)
                .build();
        minioClient.makeBucket(
                MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());

        minioAdminClient.addCannedPolicy(vaultId, rolePolicy);


        final File tmpFile = Files.createTempFile("tempfiles", ".tmp").toFile();
        tmpFile.deleteOnExit();
        FileUtils.writeStringToFile(tmpFile, vaultConfigToken, StandardCharsets.UTF_8);
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .object("vault.cryptomator")
                        .filename(tmpFile.toString())
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

    private static void makeAWSBucket(
            final String vaultId,
            final String bucketName,
            final String oidcProvider,
            final String region,
            final String rolePolicy,
            // TODO configure endpoint using hostname, scheme, port
            final String scheme,
            final String hostname,
            final int port,
            final String accessKeyId,
            final String secretKey,
            final String vaultConfigToken,
            final String rootDirHash
    ) throws IOException {

        final String trustPolicy = trustPolicy(vaultId, oidcProvider, "aws");
        LOG.info(String.format("Creating bucket with trustPolicy=%s", trustPolicy));


        // https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/java/example_code/s3/src/main/java/aws/example/s3/CreateBucket.java
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(region)

                // TODO rename minio* - make possible to use a token, as well...
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey)))
                .build();

        if (s3.doesBucketExistV2(bucketName)) {
            throw new RuntimeException(String.format("Bucket %s already exists.", vaultId));
        } else {
            s3.createBucket(bucketName);
        }
        final File tmpFile = Files.createTempFile("tempfiles", ".tmp").toFile();
        tmpFile.deleteOnExit();
        FileUtils.writeStringToFile(tmpFile, vaultConfigToken, StandardCharsets.UTF_8);
        s3.putObject(bucketName, "vault.cryptomator", tmpFile);

        // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-iam-policies.html
        AmazonIdentityManagementClientBuilder amazonIdentityManagementClientBuilder =
                AmazonIdentityManagementClientBuilder.standard();
        amazonIdentityManagementClientBuilder.setRegion(region);
        final AmazonIdentityManagement iam = amazonIdentityManagementClientBuilder.build();

        CreateRoleRequest request = new CreateRoleRequest()
                .withRoleName(bucketName)
                .withAssumeRolePolicyDocument(trustPolicy);
        final CreateRoleResult response = iam.createRole(request);
        LOG.info(String.format("Created role with trust policy %s", response));

        final PutRolePolicyRequest putRolePolicyRequest = new PutRolePolicyRequest()
                .withRoleName(bucketName)
                .withPolicyName(bucketName)
                .withPolicyDocument(rolePolicy);

        final PutRolePolicyResult putRolePolicyResponse = iam.putRolePolicy(putRolePolicyRequest);
        LOG.info(String.format("Attached role policy %s", putRolePolicyResponse));


        // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
        //        zip.file('vault.cryptomator', this.vaultConfigToken);
        //        zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
        // create meta-data for your folder and set content-length to 0
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);

        // create empty content
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
        com.amazonaws.services.s3.model.PutObjectRequest request2 = new PutObjectRequest(bucketName, String.format("d/%s/%s/", rootDirHash.substring(0, 2), rootDirHash.substring(2)), emptyContent, metadata);
        s3.putObject(request2);

    }
}
