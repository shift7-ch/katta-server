package org.cryptomator.hub.api.katta;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.cryptomator.hub.api.katta.storage.S3StorageHelper;
import org.cryptomator.hub.entities.katta.S3StorageClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
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

		// ids are assigned by the server on creation and captured here for the subsequent ordered tests.
		static UUID staticProfileId;
		static UUID stsProfileId;

		@Test
		@Order(1)
		@DisplayName("POST /storageprofile/ returns 201 for S3STATIC body and assigns an id")
		public void testPostS3StorageProfile() {
			var vaultDto = new StorageProfileS3StaticDto(
					null,
					"AWS S3 static",
					StorageProfileDto.Protocol.S3_STATIC,
					false,
					null,
					false,
					S3StorageClass.STANDARD,
					"eu-west-1",
					Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"),
					"katta-test-"
			);
			staticProfileId = UUID.fromString(given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/")
					.then().statusCode(201)
					.body("id", notNullValue())
					.body("name", equalToIgnoringCase("AWS S3 static"))
					.extract().path("id"));
		}

		@Test
		@Order(1)
		@DisplayName("POST /storageprofile/ returns 201 for S3STS body and assigns an id")
		public void testPostS3STSStorageProfile() {
			var vaultDto = new StorageProfileS3STSDto(
					null,
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
					"arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01",
					"JsonNullable[arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-02]",
					null,
					"Vault"
			);

			stsProfileId = UUID.fromString(given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/")
					.then().statusCode(201)
					.body("id", notNullValue())
					.body("name", equalToIgnoringCase("AWS S3 STS"))
					.extract().path("id"));
		}

		@ParameterizedTest
		@Order(1)
		@DisplayName("POST /storageprofile/ returns 400 for an invalid endpoint URL")
		@ValueSource(strings = {
				"not a url",
				"://no-scheme.example",
				"http:// space-in-host",
				"htp:bad"
		})
		public void testPostS3StorageProfileWithInvalidEndpoint(String invalidEndpoint) {
			var vaultDto = new StorageProfileS3StaticDto(
					null,
					"Invalid endpoint test",
					StorageProfileDto.Protocol.S3_STATIC,
					false,
					invalidEndpoint,
					false,
					S3StorageClass.STANDARD,
					"eu-west-1",
					Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"),
					"katta-test-"
			);
			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/")
					.then().statusCode(400);
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
			assertEquals(1, dtos.stream().filter(dto -> dto.getProtocol().equals(StorageProfileDto.Protocol.S3_STATIC)).count());
			assertEquals(1, dtos.stream().filter(dto -> dto.getProtocol().equals(StorageProfileDto.Protocol.S3_STS)).count());
			final StorageProfileS3STSDto s3STSDto = dtos.stream().filter(StorageProfileS3STSDto.class::isInstance).map(StorageProfileS3STSDto.class::cast).findFirst().orElseThrow();
			assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01", s3STSDto.getStsRoleAccessBucketAssumeRoleWithWebIdentity());
			assertFalse(s3STSDto.isArchived());
		}

		@Test
		@Order(2)
		@DisplayName("GET /storageprofile/{profileId} returns 200 for S3STATIC")
		public void testGetS3StorageProfile() {
			final StorageProfileDto dto = given()
					.when().get("/storageprofile/{profileId}", staticProfileId)
					.then().statusCode(200)
					.extract()
					.as(StorageProfileDto.class);

			assertEquals(staticProfileId, dto.getId());
			assertEquals(StorageProfileDto.Protocol.S3_STATIC, dto.getProtocol());
			assertInstanceOf(StorageProfileS3StaticDto.class, dto);
			assertFalse(dto.isArchived());
		}

		@Test
		@Order(2)
		@DisplayName("GET /storageprofile/{profileId} returns 200 for S3STS")
		public void testGetS3STSStorageProfile() {
			final StorageProfileDto dto = given()
					.when().get("/storageprofile/{profileId}", stsProfileId)
					.then().statusCode(200)
					.extract()
					.as(StorageProfileDto.class);

			assertEquals(stsProfileId, dto.getId());
			assertEquals(StorageProfileDto.Protocol.S3_STS, dto.getProtocol());
			final StorageProfileS3STSDto stsDto = assertInstanceOf(StorageProfileS3STSDto.class, dto);
			assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01", stsDto.getStsRoleAccessBucketAssumeRoleWithWebIdentity());
			assertFalse(stsDto.isArchived());
		}

		@Test
		@Order(4)
		@DisplayName("PUT /storage/{vaultId} returns 201")
		public void testCreateStorage() {
			final String vaultId = UUID.randomUUID().toString();
			var createS3STSBucketDto = new CreateS3STSBucketDto(
					vaultId,
					stsProfileId,
					"",
					"",
					"",
					"",
					"",
					"",
					""
			);
			var vaultDto = new StorageProfileS3STSDto(
					stsProfileId,
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
					.when().put("/storageprofile/{profileId}", staticProfileId)
					.then().statusCode(204);

		}

		@Test
		@Order(4)
		@DisplayName("PUT /storageprofile/{profileId} archiving returns 204")
		public void testArchiveS3STSStorageProfile() {
			given().formParam("archived", true)
					.when().put("/storageprofile/{profileId}", stsProfileId)
					.then().statusCode(204);
		}

		@Test
		@Order(5)
		@DisplayName("GET /storageprofile/{profileId} returns 200 for archived S3STATIC")
		public void testGetArchivedS3StorageProfile() {
			final StorageProfileDto dto = given()
					.when().get("/storageprofile/{profileId}", staticProfileId)
					.then().statusCode(200)
					.extract()
					.as(StorageProfileDto.class);
			assertInstanceOf(StorageProfileS3StaticDto.class, dto);
			assertTrue(dto.isArchived());
		}

		@Test
		@Order(5)
		@DisplayName("GET /storageprofile/{profileId} returns 200 for archived S3STS")
		public void testGetArchivedS3STSStorageProfile() {
			final StorageProfileDto dto = given()
					.when().get("/storageprofile/{profileId}", stsProfileId)
					.then().statusCode(200)
					.extract()
					.as(StorageProfileDto.class);
			assertInstanceOf(StorageProfileS3STSDto.class, dto);
			assertTrue(dto.isArchived());
		}

		@Test
		@Order(4)
		@DisplayName("PUT /storage/{vaultId} returns 410")
		public void testCreateStorageArchived() {
			final String vaultId = UUID.randomUUID().toString();
			var createS3STSBucketDto = new CreateS3STSBucketDto(
					vaultId,
					stsProfileId,
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
	@Nested
	@DisplayName("As normal user2")
	@TestSecurity(user = "User Name 2", roles = {"user"})
	@OidcSecurity(claims = {
			@Claim(key = "sub", value = "user2")
	})
	public class CreateStorageProfileAsNonAdmin {

		@Test
		@DisplayName("POST /storageprofile/ returns 403 for a user without the admin role")
		public void testPostS3StorageProfileForbidden() {
			var vaultDto = new StorageProfileS3StaticDto(
					null,
					"AWS S3 static",
					StorageProfileDto.Protocol.S3_STATIC,
					false,
					null,
					false,
					S3StorageClass.STANDARD,
					"eu-west-1",
					Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"),
					"katta-test-"
			);
			given().contentType(ContentType.JSON).body(vaultDto)
					.when().post("/storageprofile/")
					.then().statusCode(403);
		}

		@Test
		@DisplayName("PUT /storageprofile/{profileId} returns 403 for a user without the admin role")
		public void testArchiveS3StorageProfileForbidden() {
			given().formParam("archived", true)
					.when().put("/storageprofile/{profileId}", UUID.randomUUID())
					.then().statusCode(403);
		}
	}
}
