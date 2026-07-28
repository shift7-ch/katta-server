package org.cryptomator.hub.api.katta.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.api.katta.CreateS3STSBucketDto;
import org.cryptomator.hub.api.katta.StorageProfileS3STSDto;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.Base64;

@ApplicationScoped
public class S3StorageHelper {
    private static final Logger log = Logger.getLogger(S3StorageHelper.class);

    public void makeS3Bucket(
            final StorageProfileS3STSDto storageConfig,
            final CreateS3STSBucketDto dto
    ) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Make S3 bucket %s for profile %s", dto, storageConfig));
        }

        final String bucketName = storageConfig.getBucketPrefix() + dto.vaultId();
        if (!BucketUtils.isValidDnsBucketName(bucketName, false)) {
            throw new ClientErrorException(String.format("Invalid bucket name %s ", bucketName), Response.Status.BAD_REQUEST);
        }
        // https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/java/example_code/s3/src/main/java/aws/example/s3/CreateBucket.java
        final String region = dto.region();

        S3ClientBuilder s3Builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(dto.awsAccessKey(), dto.awsSecretKey(), dto.sessionToken())));
        if (storageConfig.getStsEndpoint() != null) {
            s3Builder = s3Builder
                    .endpointOverride(URI.create(storageConfig.getStsEndpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(storageConfig.isPathStyleAccessEnabled())
                            .build());
        }
        if (region != null) {
            s3Builder = s3Builder.region(Region.of(region));
        }
        try (final S3Client s3 = s3Builder.build()) {
            try {
                s3.createBucket(CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .createBucketConfiguration(CreateBucketConfiguration.builder().locationConstraint(region).build())
                        .build());
            } catch (final BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
                throw new ClientErrorException(String.format("Bucket %s already exists", bucketName), Response.Status.CONFLICT, e);
            }

            if (log.isInfoEnabled()) {
                log.info(String.format("Upload vault template to %s (%s, %s)", bucketName, dto, storageConfig));
            }
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key("vault.uvf")
                            .build(),
                    RequestBody.fromString(dto.vaultUvf()));

            // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
            //        zip.file('vault.uvf', this.vaultUvf);
            //        zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
            // create meta-data for your folder and set content-length to 0
            final PutObjectRequest placeholderPutRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(String.format("d/%s/%s/", dto.rootDirHash().substring(0, 2), dto.rootDirHash().substring(2)))
                    .contentLength(0L)
                    .build();
            s3.putObject(placeholderPutRequest, RequestBody.empty());

            final PutObjectRequest dirUvfPutRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(String.format("d/%s/%s/dir.uvf", dto.rootDirHash().substring(0, 2), dto.rootDirHash().substring(2)))
                    .build();
            s3.putObject(dirUvfPutRequest, RequestBody.fromBytes(Base64.getUrlDecoder().decode(dto.dirUvf())));
        }
    }
}
