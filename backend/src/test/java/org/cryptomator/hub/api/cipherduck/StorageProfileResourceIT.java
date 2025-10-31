package org.cryptomator.hub.api.cipherduck;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("Resource /storageprofile")
public class StorageProfileResourceIT {
	@Nested
	@DisplayName("As admin user1")
	@TestSecurity(user = "User Name 1", roles = {"admin", "user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user1")
	})
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	public class CreateVaults {
		@Test
		@Order(1)
		@DisplayName("POST /storageprofile/s3 returns 201")
		public void testPostS3StorageProfile() {
			var vaultDto = new StorageProfileS3Dto(
					UUID.fromString("72736c19-283c-49d3-80a5-ab74b5202543"),
					"AWS S3 static",
					StorageProfileDto.Protocol.s3,
					false,
					"https",
					null,
					443,
					false,
					StorageProfileS3Dto.S3_STORAGE_CLASSES.STANDARD,
					"eu-central-1",
					Arrays.asList("eu-central-1"),
					"katta-test-",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					null,
					true,
					null,
					StorageProfileS3Dto.S3_SERVERSIDE_ENCRYPTION.NONE
			);
			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/s3")
					.then().statusCode(201)
					.body("id", equalToIgnoringCase("72736c19-283c-49d3-80a5-ab74b5202543"))
					.body("name", equalToIgnoringCase("AWS S3 static"));
		}

		@Test
		@Order(1)
		@DisplayName("POST /storageprofile/s3sts returns 201")
		public void testPostS3STSStorageProfile() {
			var vaultDto = new StorageProfileS3STSDto(
					UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"),
					"AWS S3 STS",
					StorageProfileDto.Protocol.valueOf("s3"),
					false,
					null,
					null,
					null,
					false,
					StorageProfileS3Dto.S3_STORAGE_CLASSES.STANDARD,
					"eu-west-1",
					Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"),
					"katta-test-",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					null,
					true,
					null,
					StorageProfileS3Dto.S3_SERVERSIDE_ENCRYPTION.NONE,
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01",
					"JsonNullable[arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-02]",
					null,
					"Vault"
			);

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/s3sts")
					.then().statusCode(201)
					.body("id", equalToIgnoringCase("844bd517-96d4-4787-bcfa-238e103149f6"))
					.body("name", equalToIgnoringCase("AWS S3 STS"));
		}

		@Test
		@Order(2)
		@DisplayName("GET /storageprofile returns 200")
		public void testGetStorageProfiles() {
			final List<StorageProfileS3STSDto> dtos = given()
					.when().get("/storageprofile/")
					.then().statusCode(200)
					.extract()
					.as(new TypeRef<List<StorageProfileS3STSDto>>() {
					});
			assertEquals(2, dtos.size());
			assertEquals(1, dtos.stream().filter(dto -> dto.protocol.equals(StorageProfileS3Dto.Protocol.s3)).count());
			assertEquals(1, dtos.stream().filter(dto -> dto.protocol.equals(StorageProfileS3Dto.Protocol.s3sts)).count());
			final StorageProfileS3STSDto s3STSDto = dtos.stream().filter(dto -> dto.protocol.equals(StorageProfileS3Dto.Protocol.s3sts)).map(StorageProfileS3STSDto.class::cast).findFirst().get();
			assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01", s3STSDto.stsRoleAccessBucketAssumeRoleWithWebIdentity);
			assertFalse(s3STSDto.archived);
		}

		@Test
		@Order(2)
		@DisplayName("GET /storageprofile/{profileId} returns 200")
		public void testGetS3StorageProfile() {
			final StorageProfileS3STSDto dto = given()
					.when().get("/storageprofile/{profileId}", "72736c19-283c-49d3-80a5-ab74b5202543")
					.then().statusCode(200)
					.extract()
					.as(StorageProfileS3STSDto.class);

			assertEquals(UUID.fromString("72736c19-283c-49d3-80a5-ab74b5202543"), dto.id);
			assertNull(dto.stsRoleAccessBucketAssumeRoleWithWebIdentity);
			assertFalse(dto.archived);
		}

		@Test
		@Order(2)
		@DisplayName("GET /storageprofile/{profileId} returns 200")
		public void testGetS3STSStorageProfile() {
			final StorageProfileS3STSDto dto = given()
					.when().get("/storageprofile/{profileId}", "844bd517-96d4-4787-bcfa-238e103149f6")
					.then().statusCode(200)
					.extract()
					.as(StorageProfileS3STSDto.class);

			assertEquals(UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"), dto.id);
			assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01", dto.stsRoleAccessBucketAssumeRoleWithWebIdentity);
			assertFalse(dto.archived);
		}

		@Test
		@Order(3)
		@DisplayName("POST /storageprofile/s3 again returns 409")
		public void testPostS3StorageProfileAgain() {
			var vaultDto = new StorageProfileS3Dto(
					UUID.fromString("72736c19-283c-49d3-80a5-ab74b5202543"),
					"AWS S3 static",
					StorageProfileDto.Protocol.s3,
					false,
					"https",
					null,
					443,
					false,
					StorageProfileS3Dto.S3_STORAGE_CLASSES.STANDARD,
					"eu-central-1",
					Arrays.asList("eu-central-1"),
					"katta-test-",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					null,
					true,
					null,
					StorageProfileS3Dto.S3_SERVERSIDE_ENCRYPTION.NONE
			);
			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/s3")
					.then().statusCode(409);
		}

		@Test
		@Order(3)
		@DisplayName("POST /storageprofile/s3sts again returns 409")
		public void testPostS3STSStorageProfileAgain() {
			var vaultDto = new StorageProfileS3STSDto(
					UUID.fromString("844bd517-96d4-4787-bcfa-238e103149f6"),
					"AWS S3 STS",
					StorageProfileDto.Protocol.valueOf("s3"),
					false,
					null,
					null,
					null,
					false,
					StorageProfileS3Dto.S3_STORAGE_CLASSES.STANDARD,
					"eu-west-1",
					Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"),
					"katta-test-",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket",
					null,
					true,
					null,
					StorageProfileS3Dto.S3_SERVERSIDE_ENCRYPTION.NONE,
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01",
					"JsonNullable[arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-02]",
					null,
					"Vault"
			);

			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/s3sts")
					.then().statusCode(409);
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
		@DisplayName("GET /storageprofile/{profileId} returns 200 with vault archived")
		public void testGetArchivedS3StorageProfile() {
			final StorageProfileS3STSDto dto = given()
					.when().get("/storageprofile/{profileId}", "72736c19-283c-49d3-80a5-ab74b5202543")
					.then().statusCode(200)
					.extract()
					.as(StorageProfileS3STSDto.class);
			assertTrue(dto.archived);
		}

		@Test
		@Order(5)
		@DisplayName("GET /storageprofile/{profileId} returns 200 with vault archived")
		public void testGetArchivedS3STSStorageProfile() {
			final StorageProfileS3STSDto dto = given()
					.when().get("/storageprofile/{profileId}", "844bd517-96d4-4787-bcfa-238e103149f6")
					.then().statusCode(200)
					.extract()
					.as(StorageProfileS3STSDto.class);
			assertTrue(dto.archived);
		}

		@Test
		@Order(6)
		@DisplayName("GET /storageprofile returns 200")
		public void testGetArchivedStorageProfiles() {
			final List<StorageProfileS3STSDto> dtos = given()
					.queryParam("archived", true)
					.when().get("/storageprofile/")
					.then().statusCode(200)
					.extract()
					.as(new TypeRef<List<StorageProfileS3STSDto>>() {
					});
			assertEquals(2, dtos.size());
		}

		@Test
		@Order(6)
		@DisplayName("GET /storageprofile returns 200")
		public void testGetNonArchivedStorageProfiles() {
			final List<StorageProfileS3STSDto> dtos = given()
					.queryParam("archived", false)
					.when().get("/storageprofile/")
					.then().statusCode(200)
					.extract()
					.as(new TypeRef<List<StorageProfileS3STSDto>>() {
					});
			assertEquals(0, dtos.size());
		}
	}
}
