package org.cryptomator.hub.cipherduck;

import io.quarkus.qute.Qute;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateBucketAndPolicy {

    // TODO extract and inject configuration
    private final static String oidcProvider = "arn:aws:iam::930717317329:oidc-provider/login1.staging.cryptomator.cloud/realms/cipherduck";

    private static final Logger LOG = Logger.getLogger(CreateBucketAndPolicy.class);

    public static String bucketPolicy(final String bucketName) throws IOException {

        // TODO add read/write permissions GetObject etc.
        String template = IOUtils.toString(CreateBucketAndPolicy.class.getResourceAsStream("/cipherduck/s3_templates/bucketPolicyTemplate.json"), "UTF-8");
        // TODO do we want to use Qute as suggested by Quarkus?
        String policy = Qute.fmt(template, Stream.of(
                        new AbstractMap.SimpleEntry<>("bucketName", bucketName)
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return policy;
    }

    public static String trustPolicy(final String vaultId, final String oidcProvider) throws IOException {

        // TODO add read/write permissions GetObject etc.
        String template = IOUtils.toString(CreateBucketAndPolicy.class.getResourceAsStream("/cipherduck/s3_templates/trustPolicyTemplate.json"), "UTF-8");
        // TODO do we want to use Qute as suggested by Quarkus?

        String policy = Qute.fmt(template, Stream.of(
                        new AbstractMap.SimpleEntry<>("vaultId", vaultId),
                        new AbstractMap.SimpleEntry<>("oidcProvider", oidcProvider)
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return policy;
    }

    // TODO use API instead of CLI - good enough for PoC here as we want to implement it in the hub using Javascript client library
    public static void execute(final String vaultId, final String policy, final PolicyUploadOperator op, final boolean failOnError) throws IOException {
        // TODO use tempdir
        String policeFileName = String.format("%s.json", vaultId);
        FileUtils.writeStringToFile(new File(policeFileName), policy, Charset.forName("UTF-8"));
        String[] cmd = op.op(vaultId, policeFileName);
        LOG.info(Arrays.toString(cmd));
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream()));
        String s;
        while ((s = reader.readLine()) != null) {
            LOG.info("Script output: " + s);
        }
        // TODO fail...
        reader = new BufferedReader(new InputStreamReader(
                process.getErrorStream()));
        boolean failed = false;
        while ((s = reader.readLine()) != null) {
            LOG.info("Script error: " + s);
            failed = true;
        }
        if (failed && failOnError) {
            throw new RuntimeException("Failed");
        }
    }

    public interface PolicyUploadOperator {
        public String[] op(final String vaultId, final String policeFileName);
    }

    public static String[] minioCreatePolicyCmd(final String vaultId, final String policeFileName) {
        String[] command = {"mc", "admin", "policy", "create", "myminio", vaultId, policeFileName};
        return command;
    }

    // aws iam create-role --role-name login1.staging.cryptomator.cloud-Test-Role --assume-role-policy-document file://login1.staging.cryptomator.cloud.trustpolicy.json
    public static String[] awsCreateRoleCmd(final String vaultId, final String policeFileName) {
        String[] command = {"aws", "iam", "create-role", "--role-name", vaultId, "--assume-role-policy-document", "file://" + policeFileName};
        return command;
    }

    public static String[] awsDeleteRoleCmd(final String vaultId, final String policeFileName) {
        String[] command = {"aws", "iam", "delete-role", "--role-name", vaultId};
        return command;
    }

    // aws iam put-role-policy --role-name google-Test-Role --policy-name ExamplePolicy --policy-document file://ListBucketsPolicy.json
    public static String[] awsPutRolePolicyCmd(final String vaultId, final String policeFileName) {
        String[] command = {"aws", "iam", "put-role-policy", "--role-name", vaultId, "--policy-name", vaultId, "--policy-document", "file://" + policeFileName};
        return command;
    }

    public static String[] awsDeleteRolePolicyCmd(final String vaultId, final String policeFileName) {
        String[] command = {"aws", "iam", "delete-role-policy", "--role-name", vaultId, "--policy-name", vaultId};
        return command;
    }

    private static class BucketSpec {
        private final String bucketName;
        private final String vaultId;

        private BucketSpec(final String bucketName, final String vaultId) {
            this.bucketName = bucketName;
            this.vaultId = vaultId;
        }
    }

    // TODO full Durchstich: bucket creation
    public static void createBucketAndPolicy(final String bucketName, final String vaultId) {
        try {
            final String bucketPolicy = bucketPolicy(bucketName);
            LOG.info(bucketPolicy);
            final String trustPolicy = trustPolicy(vaultId, oidcProvider);
            LOG.info(trustPolicy);

            execute(vaultId, bucketPolicy, CreateBucketAndPolicy::awsDeleteRolePolicyCmd, false);
            execute(vaultId, trustPolicy, CreateBucketAndPolicy::awsDeleteRoleCmd, true);
            execute(vaultId, trustPolicy, CreateBucketAndPolicy::awsCreateRoleCmd, true);
            execute(vaultId, bucketPolicy, CreateBucketAndPolicy::awsPutRolePolicyCmd, false);
        }
        catch (IOException e){
            throw new RuntimeException("Something went wrong while creating bucket and uploading",e);
        }
    }

    // TODO integration tests
    // https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_iam-condition-keys.html -> Available keys for AWS web identity federation
    // if the azp field is set, then aud in the condition is populated by the azp field....

    public static void main(String[] args) throws IOException {

        String bucketPolicy;
        String trustPolicy;

        final BucketSpec bucketSpec1 = new BucketSpec("sample-vault-008", "7975aa47-c1cd-4cee-b20f-6d5690e209a4");
        final BucketSpec bucketSpec2 = new BucketSpec("sample-vault-009", "7975aa47-c1cd-4cee-b20f-6d5690e209a5");

        final BucketSpec[] bucketSpecs = {bucketSpec1, bucketSpec2};

        for (BucketSpec bucketSpec : bucketSpecs) {
            // TODO add read/write objects etc. to bucket policy
            // TODO naming conventions for buckets: use vaultId? Use vault name? Something else? Can vaults be re-named?
            final String bucketName = bucketSpec.bucketName;
            bucketPolicy = bucketPolicy(bucketName);
            LOG.info(bucketPolicy);
            final String vaultId = bucketSpec.vaultId;
            trustPolicy = trustPolicy(vaultId, oidcProvider);
            LOG.info(trustPolicy);

            // MinIO has no trust policies, only one OIDC provider can be claim-based
//            execute(vaultId, bucketPolicy, GeneratePolicy::minioCreatePolicyCmd);

            execute(vaultId, bucketPolicy, CreateBucketAndPolicy::awsDeleteRolePolicyCmd, false);
            execute(vaultId, trustPolicy, CreateBucketAndPolicy::awsDeleteRoleCmd, true);
            execute(vaultId, trustPolicy, CreateBucketAndPolicy::awsCreateRoleCmd, true);
            execute(vaultId, bucketPolicy, CreateBucketAndPolicy::awsPutRolePolicyCmd, false);
        }

    }


}
