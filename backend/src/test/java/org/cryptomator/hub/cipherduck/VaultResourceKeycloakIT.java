package org.cryptomator.hub.cipherduck;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import org.cryptomator.hub.api.VaultResource;
import org.cryptomator.hub.license.HubLicenseEntitlements;
import org.cryptomator.hub.license.LicenseHolder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;



@QuarkusTest
@DisplayName("Resource /vaults")
@QuarkusTestResource(value = KeycloakTestResourceLifecycleManager.class, restrictToAnnotatedClass = true)
public class VaultResourceKeycloakIT {
    @ConfigProperty(name = "hub.keycloak.realm")
    String keycloakRealm;

    @KeycloakTestResourceLifecycleManager.InjectKeycloakContainer
    KeycloakContainer container;

    @InjectMock
    LicenseHolder licenseHolder;

    @BeforeEach
    public void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        // https://quarkus.io/guides/getting-started-testing#quarkus_mock
        KeycloakCryptomatorVaultsHelperFromContainer mock = new KeycloakCryptomatorVaultsHelperFromContainer();

        QuarkusMock.installMockForType(mock, KeycloakCryptomatorVaultsHelper.class);

        var entitlements = HubLicenseEntitlements.create().withSeats(5L);
        Mockito.doReturn(entitlements).when(licenseHolder).getEntitlements();
        Mockito.doReturn(false).when(licenseHolder).isExpired();
    }

    public class KeycloakCryptomatorVaultsHelperFromContainer extends KeycloakCryptomatorVaultsHelper {
        KeycloakCryptomatorVaultsHelperFromContainer() {
            super.keycloakRealm = VaultResourceKeycloakIT.this.keycloakRealm;
        }

        @Override
        public Keycloak getKeycloak() {
            return container.getKeycloakAdminClient();
        }
    }

    @Nested
    @DisplayName("As vault admin user1")
    @TestSecurity(user = "alice", roles = {"user", "create-vaults"})
    @OidcSecurity(claims = {
            // needs to be user ID and not the user name
            @Claim(key = "sub", value = "alice")
    })
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class CreateVaults {

        @Test
        @Order(1)
        @DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100007777 returns 201")
        public void testCreateVault1() {
            var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100007777");
            var vaultDto = new VaultResource.VaultDto(uuid, "My Vault", Instant.parse("2112-12-21T21:12:21Z"), "Test vault 3", false, 0, Map.of(), "uvfMetadata3", "uvfKeySet3", "masterkey3", 42, "NaCl", "authPubKey3", "authPrvKey3");
            given().contentType(ContentType.JSON).body(vaultDto)
                    .queryParam("minio", true)
                    .queryParam("aws", true)
                    .when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-000100007777")
                    .then().statusCode(201)
                    .body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100007777"))
                    .body("name", equalTo("My Vault"))
                    .body("description", equalTo("Test vault 3"))
                    .body("archived", equalTo(false));
        }

        @Test
        @Order(2)
        @DisplayName("PUT /vaults/7E57C0DE-0000-4000-8000-000100007777 returns 200, updating only name, description and archive flag")
        public void testUpdateVault() {
            var uuid = UUID.fromString("7E57C0DE-0000-4000-8000-000100007777");
            var vaultDto = new VaultResource.VaultDto(uuid, "VaultUpdated", Instant.parse("2222-11-11T11:11:11Z"), "Vault updated.", true, 0, Map.of(), "doNotUpdateEither", "doNotUpdateEither", "doNotUpdateEither", 27, "doNotUpdateEither", "doNotUpdateEither", "doNotUpdateEither");
            given().contentType(ContentType.JSON)
                    .body(vaultDto)
                    .when().put("/vaults/{vaultId}", "7E57C0DE-0000-4000-8000-000100007777")
                    .then().statusCode(200)
                    .body("id", equalToIgnoringCase("7E57C0DE-0000-4000-8000-000100007777"))
                    .body("name", equalTo("VaultUpdated"))
                    .body("description", equalTo("Vault updated."))
                    .body("archived", equalTo(true))
                    .body("creationTime", not("2222-11-11T11:11:11Z"));
        }
    }
}
