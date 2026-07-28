package org.cryptomator.hub.api.katta;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import org.cryptomator.hub.entities.katta.S3StorageClass;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Exercises {@code PUT /storage/{vaultId}} against a real MinIO instance (Quarkus dev service, see
 * https://docs.quarkiverse.io/quarkus-minio/dev/dev-services.html) instead of the mocked {@code S3StorageHelper}
 * used in {@link StorageProfileResourceIT}. Since {@code makeS3Bucket} signs requests with
 * {@code AwsSessionCredentials}, plain MinIO root credentials are not enough: we first mint real temporary
 * credentials via MinIO's STS {@code AssumeRole} action (no OIDC/Keycloak setup required, unlike the
 * {@code AssumeRoleWithWebIdentity} flow used by real clients).
 */
@QuarkusTest
@DisplayName("Resource /storage against a real MinIO instance")
@TestSecurity(user = "Admin User", roles = {"admin", "user"})
@OidcSecurity(claims = {
        @Claim(key = "sub", value = "minio-test-admin")
})
public class StorageResourceIT {

    @ConfigProperty(name = "quarkus.minio.host")
    String minioHost;

    @ConfigProperty(name = "quarkus.minio.port")
    int minioPort;

    @ConfigProperty(name = "quarkus.minio.access-key")
    String minioAccessKey;

    @ConfigProperty(name = "quarkus.minio.secret-key")
    String minioSecretKey;

    @Test
    @DisplayName("PUT /storage/{vaultId} creates a real bucket and uploads the vault template to MinIO")
    public void testCreateStorageAgainstRealMinio() {
        final String minioEndpoint = "http://%s:%d".formatted(minioHost, minioPort);
        final UUID profileId = createLocalMinioStorageProfile(minioEndpoint);
        final Credentials tempCredentials = assumeMinioRole(minioEndpoint, "katta-storageprofile-minio-it");

        final String vaultId = UUID.randomUUID().toString();
        var createBucketDto = new CreateS3STSBucketDto(
                vaultId,
                profileId,
                "vault.uvf test content",
                Base64.getUrlEncoder().withoutPadding().encodeToString("dir.uvf test content".getBytes()),
                "deadbeefcafe",
                tempCredentials.accessKeyId(),
                tempCredentials.secretAccessKey(),
                tempCredentials.sessionToken(),
                "us-east-1"
        );

        given().contentType(ContentType.JSON).body(createBucketDto)
                .when().put("/storage/{vaultId}", vaultId)
                .then().statusCode(201);
    }

    @Test
    @DisplayName("PUT /storage/{vaultId} returns 409 when the same vault's bucket is created twice")
    public void testCreateStorageTwiceReturnsConflict() {
        final String minioEndpoint = "http://%s:%d".formatted(minioHost, minioPort);
        final UUID profileId = createLocalMinioStorageProfile(minioEndpoint);
        final Credentials tempCredentials = assumeMinioRole(minioEndpoint, "katta-storageprofile-minio-it-conflict");

        final String vaultId = UUID.randomUUID().toString();
        var createBucketDto = new CreateS3STSBucketDto(
                vaultId,
                profileId,
                "vault.uvf test content",
                Base64.getUrlEncoder().withoutPadding().encodeToString("dir.uvf test content".getBytes()),
                "deadbeefcafe",
                tempCredentials.accessKeyId(),
                tempCredentials.secretAccessKey(),
                tempCredentials.sessionToken(),
                "us-east-1"
        );

        given().contentType(ContentType.JSON).body(createBucketDto)
                .when().put("/storage/{vaultId}", vaultId)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(createBucketDto)
                .when().put("/storage/{vaultId}", vaultId)
                .then().statusCode(409);
    }

    @Test
    @DisplayName("PUT /storage/{vaultId} returns 400 when the bucket name is invalid")
    public void testCreateStorageWithInvalidBucketNameReturnsBadRequest() {
        final String minioEndpoint = "http://%s:%d".formatted(minioHost, minioPort);
        final UUID profileId = createLocalMinioStorageProfile(minioEndpoint);
        final Credentials tempCredentials = assumeMinioRole(minioEndpoint, "katta-storageprofile-minio-it-badrequest");

        // the bucket name is bucketPrefix + body.vaultId() (not the path param): uppercase letters and
        // underscores are not allowed in S3/MinIO bucket names, so bucket creation fails server-side.
        var createBucketDto = new CreateS3STSBucketDto(
                "Invalid_Bucket_Id!",
                profileId,
                "vault.uvf test content",
                Base64.getUrlEncoder().withoutPadding().encodeToString("dir.uvf test content".getBytes()),
                "deadbeefcafe",
                tempCredentials.accessKeyId(),
                tempCredentials.secretAccessKey(),
                tempCredentials.sessionToken(),
                "us-east-1"
        );

        given().contentType(ContentType.JSON).body(createBucketDto)
                .when().put("/storage/{vaultId}", UUID.randomUUID())
                .then().statusCode(400);
    }

    // persists a S3STS storage profile pointing at the local MinIO dev service
    private UUID createLocalMinioStorageProfile(String minioEndpoint) {
        var profileDto = new StorageProfileS3STSDto(
                null,
                "Local MinIO",
                StorageProfileDto.Protocol.S3_STS,
                false,
                minioEndpoint,
                true, // pathStyleAccessEnabled: required for a single-host MinIO instance
                S3StorageClass.STANDARD,
                "us-east-1",
                List.of("us-east-1"),
                "katta-minio-test-",
                "n/a", // stsRoleCreateBucketClient: only relevant for the desktop client, not exercised here
                "n/a", // stsRoleCreateBucketHub
                minioEndpoint, // stsEndpoint
                "n/a", // stsRoleAccessBucketAssumeRoleWithWebIdentity: not exercised, we mint creds via plain AssumeRole below
                null,
                null,
                "Vault"
        );
        return UUID.fromString(given().contentType(ContentType.JSON).body(profileDto)
                .when().post("/storageprofile/")
                .then().statusCode(201)
                .extract().path("id"));
    }

    // mints real, MinIO-issued temporary credentials (no external IdP/OIDC needed for plain AssumeRole)
    private Credentials assumeMinioRole(String minioEndpoint, String roleSessionName) {
        try (StsClient sts = StsClient.builder()
                .endpointOverride(URI.create(minioEndpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(minioAccessKey, minioSecretKey)))
                .build()) {
            return sts.assumeRole(AssumeRoleRequest.builder()
                    .roleSessionName(roleSessionName)
                    .durationSeconds(900)
                    .build()).credentials();
        }
    }
}
