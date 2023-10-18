package org.cryptomator.hub.api.cipherduck.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.cryptomator.hub.api.cipherduck.StorageConfig;
import org.cryptomator.hub.api.cipherduck.StorageDto;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class S3Storage {


	public static void makeS3Bucket(
			final StorageConfig storageConfig,
			final StorageDto dto
	) {

		final String bucketName = storageConfig.bucketPrefix() + dto.vaultId();
		// https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/java/example_code/s3/src/main/java/aws/example/s3/CreateBucket.java
		final String region = storageConfig.region().orElse("eu_east_1");
		AmazonS3ClientBuilder s3Builder = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(dto.awsAccessKey(), dto.awsSecretKey(), dto.sessionToken())));
		if (storageConfig.jwe().stsEndpoint().isPresent()) {
			s3Builder = s3Builder
					.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(storageConfig.jwe().stsEndpoint().get(), region))
					.withPathStyleAccessEnabled(storageConfig.withPathStyleAccessEnabled().orElse(false));
		} else {
			s3Builder = s3Builder.withRegion(region);
		}
		final AmazonS3 s3 = s3Builder
				.build();

		if (s3.doesBucketExistV2(bucketName)) {
			throw new ClientErrorException(String.format("Bucket %s already exists or no permission to list.", bucketName), Response.Status.CONFLICT);
		}
		s3.createBucket(bucketName);
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
	}
}
