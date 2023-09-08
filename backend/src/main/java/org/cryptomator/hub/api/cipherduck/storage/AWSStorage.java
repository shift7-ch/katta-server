package org.cryptomator.hub.api.cipherduck.storage;

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
import org.apache.commons.io.IOUtils;
import org.cryptomator.hub.api.cipherduck.StorageConfig;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.cryptomator.hub.api.cipherduck.storage.RolePolicyHelper.rolePolicy;
import static org.cryptomator.hub.api.cipherduck.storage.RolePolicyHelper.trustPolicy;

public class AWSStorage {
    private static final Logger LOG = Logger.getLogger(AWSStorage.class);


    public static void makeAWSBucket(
            final String vaultId,
            final String bucketName,
            final StorageConfig storageConfig,
            final String vaultConfigToken,
            final String rootDirHash
    ) throws IOException {

        final String trustPolicy = trustPolicy(vaultId, storageConfig.oidcProvider().get(), "aws");
        LOG.info(String.format("Creating bucket with trustPolicy=%s", trustPolicy));

        final String rolePolicy = rolePolicy(bucketName, storageConfig.s3Type());


        // https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/java/example_code/s3/src/main/java/aws/example/s3/CreateBucket.java
        final String region = storageConfig.region().orElse(null);
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(storageConfig.adminAccessKeyId(), storageConfig.adminSecretKey())))
                .build();

        if (s3.doesBucketExistV2(bucketName)) {
            throw new RuntimeException(String.format("Bucket %s already exists.", vaultId));
        } else {
            s3.createBucket(bucketName);
        }
        s3.putObject(bucketName, "vault.cryptomator", IOUtils.toInputStream(vaultConfigToken), new ObjectMetadata());

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
