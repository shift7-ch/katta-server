package org.cryptomator.hub.api.cipherduck.storage;


import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.api.cipherduck.CreateS3STSBucketDto;
import org.cryptomator.hub.api.cipherduck.StorageProfileS3STSDto;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AccelerateConfiguration;
import software.amazon.awssdk.services.s3.model.BucketAccelerateStatus;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionByDefault;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionConfiguration;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionRule;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

import java.net.URI;
import java.util.Collections;

public class S3StorageHelper {
	private static final Logger log = Logger.getLogger(S3StorageHelper.class);

	public static void makeS3Bucket(
			final StorageProfileS3STSDto storageConfig,
			final CreateS3STSBucketDto dto
	) {

		if (log.isInfoEnabled()) {
			log.info(String.format("Make S3 bucket %s for profile %s", dto, storageConfig));
		}

		final String bucketName = storageConfig.bucketPrefix() + dto.vaultId();
		// https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/java/example_code/s3/src/main/java/aws/example/s3/CreateBucket.java
		final String region = dto.region();

		S3ClientBuilder s3Builder = S3Client.builder()
				.credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(dto.awsAccessKey(), dto.awsSecretKey(), dto.sessionToken())));
		if (storageConfig.stsEndpoint() != null) {
			s3Builder = s3Builder
					.endpointOverride(URI.create(storageConfig.stsEndpoint()))
					.serviceConfiguration(S3Configuration.builder()
							.pathStyleAccessEnabled(storageConfig.withPathStyleAccessEnabled() != null ? storageConfig.withPathStyleAccessEnabled() : false)
							.build());
		} else if (region != null) {
			s3Builder = s3Builder.region(Region.of(region));
		}
		try (final S3Client s3 = s3Builder.build()) {
			boolean okToCreate = true;
			try {
				s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
			} catch (final NoSuchBucketException e) {
				okToCreate = true;
			} catch (final S3Exception e) {
				if (e.statusCode() == 403) {
					// ignore
					log.info("Ignoring 403 on bucket head", e);
					okToCreate = true;
				}
			}
			if (!okToCreate) {
				throw new ClientErrorException(String.format("Bucket %s already exists or no permission to list.", bucketName), Response.Status.CONFLICT);
			}

			s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
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
			final PutObjectRequest request2 = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(String.format("d/%s/%s/", dto.rootDirHash().substring(0, 2), dto.rootDirHash().substring(2)))
					.contentLength(0L)
					.build();
			s3.putObject(request2, RequestBody.empty());

			// enable versioning on the bucket.
			{
				if (log.isInfoEnabled()) {
					log.info(String.format("Enable/disable bucket versioning on %s (%s, %s)", bucketName, dto, storageConfig));
				}
				s3.putBucketVersioning(PutBucketVersioningRequest.builder()
						.bucket(bucketName)
						.versioningConfiguration(VersioningConfiguration.builder().status(storageConfig.bucketVersioning() ? BucketVersioningStatus.ENABLED : BucketVersioningStatus.SUSPENDED).build())
						.build());
				final GetBucketVersioningResponse conf = s3.getBucketVersioning(GetBucketVersioningRequest.builder().bucket(bucketName).build());
				if (log.isInfoEnabled()) {
					log.info(String.format("Enabled/disabled bucket versioning on %s (%s, %s) with status %s", bucketName, dto, storageConfig, conf.statusAsString()));
				}
			}

			// enable/disable bucket acceleration on the bucket. Skip if not set (e.g. MinIO which has no bucket acceleration API)
			if (storageConfig.bucketAcceleration() != null) {
				if (log.isInfoEnabled()) {
					log.info(String.format("Enable/disable bucket acceleration on %s (%s, %s)", bucketName, dto, storageConfig));
				}
				s3.putBucketAccelerateConfiguration(PutBucketAccelerateConfigurationRequest.builder()
						.bucket(bucketName)
						.accelerateConfiguration(AccelerateConfiguration.builder()
								.status(storageConfig.bucketAcceleration() ? BucketAccelerateStatus.ENABLED : BucketAccelerateStatus.SUSPENDED)
								.build())
						.build());
				final GetBucketAccelerateConfigurationResponse conf = s3.getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest.builder().bucket(bucketName).build());
				if (log.isInfoEnabled()) {
					log.info(String.format("Enabled/disabled bucket acceleration on %s (%s, %s) with status %s", bucketName, dto, storageConfig, conf.status()));
				}
			}

			// enable/disable bucket encryption on the bucket
			{
				if (log.isInfoEnabled()) {
					log.info(String.format("Enable/disable bucket encryption on %s (%s, %s)", bucketName, dto, storageConfig));
				}
				switch (storageConfig.bucketEncryption()) {
					case NONE -> {
					}
					case SSE_AES256 -> s3.putBucketEncryption(
							PutBucketEncryptionRequest.builder()
									.bucket(bucketName)
									.serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder()
											.rules(Collections.singleton(
													ServerSideEncryptionRule.builder()
															.applyServerSideEncryptionByDefault(ServerSideEncryptionByDefault.builder()
																	.sseAlgorithm(ServerSideEncryption.AES256).build())
															.build()))

											.build())
									.build());
					case SSE_KMS_DEFAULT -> s3.putBucketEncryption(
							PutBucketEncryptionRequest.builder()
									.bucket(bucketName)
									.serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder()
											.rules(Collections.singleton(
													ServerSideEncryptionRule.builder()
															.applyServerSideEncryptionByDefault(ServerSideEncryptionByDefault.builder()
																	.sseAlgorithm(ServerSideEncryption.AWS_KMS).build())
															.build()))

											.build())
									.build());
				}
				switch (storageConfig.bucketEncryption()) {
					case NONE:
						// MinIO does not support bucket acceleration nor encryption
						// https://min.io/docs/minio/linux/administration/identity-access-management/policy-based-access-control.html
						// https://github.com/minio/minio/blob/master/cmd/api-router.go
						// https://github.com/minio/minio/issues/14586
						break;
					case SSE_AES256:
					case SSE_KMS_DEFAULT:
						final GetBucketEncryptionResponse conf = s3.getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(bucketName).build());
						if (log.isInfoEnabled()) {
							log.info(String.format("Enabled/disabled bucket encryption on %s (%s, %s) with configuration %s", bucketName, dto, storageConfig, conf.serverSideEncryptionConfiguration()));
						}
				}
			}
		}
	}
}
