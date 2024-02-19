package org.cryptomator.hub.api.cipherduck.storage;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketAccelerateConfiguration;
import com.amazonaws.services.s3.model.BucketAccelerateStatus;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.GetBucketEncryptionResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketAccelerateConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.cryptomator.hub.api.cipherduck.StorageDto;
import org.cryptomator.hub.api.cipherduck.StorageProfileS3STSDto;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;

public class S3StorageHelper {
	private static final Logger log = Logger.getLogger(S3StorageHelper.class);

	public static void makeS3Bucket(
			final StorageProfileS3STSDto storageConfig,
			final StorageDto dto
	) {

		if (log.isInfoEnabled()) {
			log.info(String.format("Make S3 bucket %s for profile %s", dto, storageConfig));
		}

		final String bucketName = storageConfig.bucketPrefix() + dto.vaultId();
		// https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/java/example_code/s3/src/main/java/aws/example/s3/CreateBucket.java
		final String region = dto.region();
		AmazonS3ClientBuilder s3Builder = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(dto.awsAccessKey(), dto.awsSecretKey(), dto.sessionToken())));
		if (storageConfig.stsEndpoint() != null) {
			s3Builder = s3Builder
					.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(storageConfig.stsEndpoint(), region))
					.withPathStyleAccessEnabled(storageConfig.withPathStyleAccessEnabled() != null ? storageConfig.withPathStyleAccessEnabled() : false);
		} else if (region != null) {
			s3Builder = s3Builder.withRegion(region);
		}
		final AmazonS3 s3 = s3Builder
				.build();

		if (s3.doesBucketExistV2(bucketName)) {
			throw new ClientErrorException(String.format("Bucket %s already exists or no permission to list.", bucketName), Response.Status.CONFLICT);
		}
		s3.createBucket(bucketName);
		if (log.isInfoEnabled()) {
			log.info(String.format("Upload vault template to %s (%s, %s)", bucketName, dto, storageConfig));
		}
		s3.putObject(bucketName, "vault.cryptomator", IOUtils.toInputStream(dto.vaultConfigToken()), new ObjectMetadata());

		// See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
		//        zip.file('vault.cryptomator', this.vaultConfigToken);
		//        zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
		// create meta-data for your folder and set content-length to 0
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);

		// create empty content
		InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
		com.amazonaws.services.s3.model.PutObjectRequest request2 = new PutObjectRequest(bucketName, String.format("d/%s/%s/", dto.rootDirHash().substring(0, 2), dto.rootDirHash().substring(2)), emptyContent, metadata);
		s3.putObject(request2);

		// TODO review - should we allow for not setting (because of permissions)?
		// enable versioning on the bucket.
		{
			if (log.isInfoEnabled()) {
				log.info(String.format("Enable/disable bucket versioning on %s (%s, %s)", bucketName, dto, storageConfig));
			}
			s3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(bucketName, new BucketVersioningConfiguration().withStatus(storageConfig.bucketVersioning() ? BucketVersioningConfiguration.ENABLED : BucketVersioningConfiguration.OFF)));
			final BucketVersioningConfiguration conf = s3.getBucketVersioningConfiguration(bucketName);
			if (log.isInfoEnabled()) {
				log.info(String.format("Enabled/disabled bucket versioning on %s (%s, %s) with status %s", bucketName, dto, storageConfig, conf.getStatus()));
			}
		}

		// enable/disable bucket acceleration on the bucket
		{
			if (log.isInfoEnabled()) {
				log.info(String.format("Enable/disable bucket acceleration on %s (%s, %s)", bucketName, dto, storageConfig));
			}
			try {
			s3.setBucketAccelerateConfiguration(new SetBucketAccelerateConfigurationRequest(bucketName, new BucketAccelerateConfiguration(storageConfig.bucketAcceleration() ? BucketAccelerateStatus.Enabled : BucketAccelerateStatus.Suspended)));
			}
			catch (AmazonS3Exception e){
				if(!storageConfig.bucketAcceleration() && e.getMessage().contains("MalformedXML")){
					// MinIO does not support bucket acceleration nor encryption -> TODO https://github.com/shift7-ch/cipherduck-hub/issues/44 should we make bucketAcceleration attribute in storage profile nullable instead?
					// https://min.io/docs/minio/linux/administration/identity-access-management/policy-based-access-control.html
					// https://github.com/minio/minio/blob/master/cmd/api-router.go
					// https://github.com/minio/minio/issues/14586
					log.warn(String.format("Ignoring failed SetBucketAccelerateConfiguration call - MinIO does not support it. Details: %s", e));
				}
			}

			final BucketAccelerateConfiguration conf = s3.getBucketAccelerateConfiguration(bucketName);
			if (log.isInfoEnabled()) {
				log.info(String.format("Enabled/disabled bucket acceleration on %s (%s, %s) with status %s", bucketName, dto, storageConfig, conf.getStatus()));
			}
		}

		// enable/disable bucket encryption on the bucket
		{
			if (log.isInfoEnabled()) {
				log.info(String.format("Enable/disable bucket encryption on %s (%s, %s)", bucketName, dto, storageConfig));
			}
			switch (storageConfig.bucketEncryption()) {
				case NONE -> {}
				case SSE_AES256 -> s3.setBucketEncryption(
						new SetBucketEncryptionRequest()
								.withBucketName(bucketName)
								.withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
								.withRules(Collections.singleton(
										new ServerSideEncryptionRule()
												.withApplyServerSideEncryptionByDefault(new ServerSideEncryptionByDefault().withSSEAlgorithm(SSEAlgorithm.AES256))))
						));
				case SSE_KMS_DEFAULT -> s3.setBucketEncryption(
						new SetBucketEncryptionRequest()
								.withBucketName(bucketName)
								.withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
										.withRules(Collections.singleton(
												new ServerSideEncryptionRule()
														.withApplyServerSideEncryptionByDefault(new ServerSideEncryptionByDefault().withSSEAlgorithm(SSEAlgorithm.KMS))))
								));
			}
			switch (storageConfig.bucketEncryption()) {
				case  NONE:
					// MinIO does not support bucket acceleration nor encryption -> TODO https://github.com/shift7-ch/cipherduck-hub/issues/44 should we make bucketEncryption attribute in storage profile nullable instead?
					// https://min.io/docs/minio/linux/administration/identity-access-management/policy-based-access-control.html
					// https://github.com/minio/minio/blob/master/cmd/api-router.go
					// https://github.com/minio/minio/issues/14586
					break;
				case SSE_AES256:
				case SSE_KMS_DEFAULT:
					final GetBucketEncryptionResult conf = s3.getBucketEncryption(bucketName);
					if (log.isInfoEnabled()) {
						log.info(String.format("Enabled/disabled bucket encryption on %s (%s, %s) with configuration %s", bucketName, dto, storageConfig, conf.getServerSideEncryptionConfiguration()));
					}
			}
		}
		// TODO https://github.com/shift7-ch/cipherduck-hub/issues/44 CORS?
		//s3.setBucketCrossOriginConfiguration();

	}
}
