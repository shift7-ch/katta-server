package org.cryptomator.hub.api.katta;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.cryptomator.hub.api.katta.storage.S3StorageHelper;
import org.cryptomator.hub.entities.katta.S3ServersideEncryption;
import org.cryptomator.hub.entities.katta.S3StorageClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
@QuarkusTest
@DisplayName("Resource /storageprofile")
public class StorageProfileResourceIT {
	@InjectMock
	S3StorageHelper s3StorageHelper;

	@Nested
	@DisplayName("As admin user1")
	@TestSecurity(user = "User Name 1", roles = {"admin", "user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class CreateStorageProfile {
		@Test
		@Order(1)
		@DisplayName("POST /storageprofile/ returns 201 for S3STATIC body")
		public void testPostS3StorageProfile() {
			var vaultDto = new StorageProfileS3StaticDto(
					UUID.fromString("72736c19-283c-49d3-80a5-ab74b5202543"),
					"AWS S3 static",
					StorageProfileDto.Protocol.S3_STATIC,
					false,
					null,
					false,
					S3StorageClass.STANDARD
			);
			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/")
					.then().statusCode(201)
					.body("id", equalToIgnoringCase("72736c19-283c-49d3-80a5-ab74b5202543"))
					.body("name", equalToIgnoringCase("AWS S3 static"));
		}

		@Test
		@Order(1)
		@DisplayName("POST /storageprofile/ returns 201 for S3STS body")
		public void testPostS3STSStorageProfile() {
			var vaultDto = new StorageProfileS3STSDto(
					UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"),
					"AWS S3 STS",
					StorageProfileDto.Protocol.S3_STS,
					false,
					null,
					false,
					S3StorageClass.STANDARD,
					"eu-west-1",
					Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"),
					"katta-test-",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					null,
					true,
					null,
					S3ServersideEncryption.NONE,
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01",
					"JsonNullable[arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-02]",
					null,
					"Vault"
			);

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/")
					.then().statusCode(201)
					.body("id", equalToIgnoringCase("844bd517-96d4-4787-bcfa-238e103149f6"))
					.body("name", equalToIgnoringCase("AWS S3 STS"));
		}

		@Test
		@Order(2)
		@DisplayName("GET /storageprofile returns 200 with both subtypes deserialized polymorphically")
		public void testGetStorageProfiles() {
			final List<StorageProfileDto> dtos = given()
					.when().get("/storageprofile/")
					.then().statusCode(200)
					.extract()
					.as(new TypeRef<List<StorageProfileDto>>() {
					});
			assertEquals(2, dtos.size());
			assertEquals(1, dtos.stream().filter(dto -> dto.protocol.equals(StorageProfileDto.Protocol.S3_STATIC)).count());
			assertEquals(1, dtos.stream().filter(dto -> dto.protocol.equals(StorageProfileDto.Protocol.S3_STS)).count());
			final StorageProfileS3STSDto s3STSDto = dtos.stream().filter(StorageProfileS3STSDto.class::isInstance).map(StorageProfileS3STSDto.class::cast).findFirst().orElseThrow();
			assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01", s3STSDto.stsRoleAccessBucketAssumeRoleWithWebIdentity);
			assertFalse(s3STSDto.archived);
		}

		@Test
		@Order(2)
		@DisplayName("GET /storageprofile/{profileId} returns 200 for S3STATIC")
		public void testGetS3StorageProfile() {
			final StorageProfileDto dto = given()
					.when().get("/storageprofile/{profileId}", "72736c19-283c-49d3-80a5-ab74b5202543")
					.then().statusCode(200)
					.extract()
					.as(StorageProfileDto.class);

			assertEquals(UUID.fromString("72736c19-283c-49d3-80a5-ab74b5202543"), dto.id);
			assertEquals(StorageProfileDto.Protocol.S3_STATIC, dto.protocol);
			assertInstanceOf(StorageProfileS3StaticDto.class, dto);
			assertFalse(dto.archived);
		}

		@Test
		@Order(2)
		@DisplayName("GET /storageprofile/{profileId} returns 200 for S3STS")
		public void testGetS3STSStorageProfile() {
			final StorageProfileDto dto = given()
					.when().get("/storageprofile/{profileId}", "844bd517-96d4-4787-bcfa-238e103149f6")
					.then().statusCode(200)
					.extract()
					.as(StorageProfileDto.class);

			assertEquals(UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"), dto.id);
			assertEquals(StorageProfileDto.Protocol.S3_STS, dto.protocol);
			final StorageProfileS3STSDto stsDto = assertInstanceOf(StorageProfileS3STSDto.class, dto);
			assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01", stsDto.stsRoleAccessBucketAssumeRoleWithWebIdentity);
			assertFalse(stsDto.archived);
		}

		@Test
		@Order(3)
		@DisplayName("POST /storageprofile/ S3STATIC again returns 409")
		public void testPostS3StorageProfileAgain() {
			var vaultDto = new StorageProfileS3StaticDto(
					UUID.fromString("72736c19-283c-49d3-80a5-ab74b5202543"),
					"AWS S3 static",
					StorageProfileDto.Protocol.S3_STATIC,
					false,
					null,
					false,
					S3StorageClass.STANDARD
			);
			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/")
					.then().statusCode(409);
		}

		@Test
		@Order(3)
		@DisplayName("POST /storageprofile/ S3STS again returns 409")
		public void testPostS3STSStorageProfileAgain() {
			var vaultDto = new StorageProfileS3STSDto(
					UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"),
					"AWS S3 STS",
					StorageProfileDto.Protocol.S3_STS,
					false,
					null,
					false,
					S3StorageClass.STANDARD,
					"eu-west-1",
					Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"),
					"katta-test-",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					null,
					true,
					null,
					S3ServersideEncryption.NONE,
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01",
					"JsonNullable[arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-02]",
					null,
					"Vault"
			);

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/")
					.then().statusCode(409);
		}

		@Test
		@Order(4)
		@DisplayName("PUT /storage/{vaultId} returns 201")
		public void testCreateStorage() {
			final String vaultId = UUID.randomUUID().toString();
			var createS3STSBucketDto = new CreateS3STSBucketDto(
					vaultId,
					UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"),
					"",
					"",
					"",
					"",
					"",
					"",
					""
			);
			var vaultDto = new StorageProfileS3STSDto(
					UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"),
					"AWS S3 STS",
					StorageProfileDto.Protocol.S3_STS,
					false,
					null,
					false,
					S3StorageClass.STANDARD,
					"eu-west-1",
					Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"),
					"katta-test-",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					null,
					true,
					null,
					S3ServersideEncryption.NONE,
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01",
					"JsonNullable[arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-02]",
					null,
					"Vault"
			);
			given().contentType(ContentType.JSON).body(createS3STSBucketDto)
					.when().put("/storage/{vaultId}", vaultId)
					.then().statusCode(201);
			Mockito.verify(s3StorageHelper, times(1)).makeS3Bucket(vaultDto, createS3STSBucketDto);
		}

		@Test
		@Order(4)
		@DisplayName("PUT /storageprofile/{profileId} archiving returns 204")
		public void testArchiveS3StorageProfile() {
			given().formParam("archived", true)
					.when().put("/storageprofile/{profileId}", "72736c19-283c-49d3-80a5-ab74b5202543")
					.then().statusCode(204);

		}

		@Test
		@Order(4)
		@DisplayName("PUT /storageprofile/{profileId} archiving returns 204")
		public void testArchiveS3STSStorageProfile() {
			given().formParam("archived", true)
					.when().put("/storageprofile/{profileId}", "844bd517-96d4-4787-bcfa-238e103149f6")
					.then().statusCode(204);
		}

		@Test
		@Order(5)
		@DisplayName("GET /storageprofile/{profileId} returns 200 for archived S3STATIC")
		public void testGetArchivedS3StorageProfile() {
			final StorageProfileDto dto = given()
					.when().get("/storageprofile/{profileId}", "72736c19-283c-49d3-80a5-ab74b5202543")
					.then().statusCode(200)
					.extract()
					.as(StorageProfileDto.class);
			assertInstanceOf(StorageProfileS3StaticDto.class, dto);
			assertTrue(dto.archived);
		}

		@Test
		@Order(5)
		@DisplayName("GET /storageprofile/{profileId} returns 200 for archived S3STS")
		public void testGetArchivedS3STSStorageProfile() {
			final StorageProfileDto dto = given()
					.when().get("/storageprofile/{profileId}", "844bd517-96d4-4787-bcfa-238e103149f6")
					.then().statusCode(200)
					.extract()
					.as(StorageProfileDto.class);
			assertInstanceOf(StorageProfileS3STSDto.class, dto);
			assertTrue(dto.archived);
		}

		@Test
		@Order(4)
		@DisplayName("PUT /storage/{vaultId} returns 410")
		public void testCreateStorageArchived() {
			final String vaultId = UUID.randomUUID().toString();
			var createS3STSBucketDto = new CreateS3STSBucketDto(
					vaultId,
					UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"),
					"",
					"",
					"",
					"",
					"",
					"",
					""
			);
			given().contentType(ContentType.JSON).body(createS3STSBucketDto)
					.when().put("/storage/{vaultId}", vaultId)
					.then().statusCode(410);
		}

		@Test
		@Order(6)
		@DisplayName("GET /storageprofile returns 200")
		public void testGetArchivedStorageProfiles() {
			final List<StorageProfileDto> dtos = given()
					.queryParam("archived", true)
					.when().get("/storageprofile/")
					.then().statusCode(200)
					.extract()
					.as(new TypeRef<List<StorageProfileDto>>() {
					});
			assertEquals(2, dtos.size());
		}

		@Test
		@Order(6)
		@DisplayName("GET /storageprofile returns 200")
		public void testGetNonArchivedStorageProfiles() {
			final List<StorageProfileDto> dtos = given()
					.queryParam("archived", false)
					.when().get("/storageprofile/")
					.then().statusCode(200)
					.extract()
					.as(new TypeRef<List<StorageProfileDto>>() {
					});
			assertEquals(0, dtos.size());
		}

	}
}
