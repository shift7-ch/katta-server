package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

@Path("/storageprofile")
public class StorageProfileResource {


	@Inject
	CipherduckConfig cipherduckConfig;


	@PUT
	@Path("/")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public void uploadStorageProfile(StorageProfileDto c) {
		cipherduckConfig.inMemoryStorageConfigs.add(c);
	}


	@GET
	@Path("/")
//	@RolesAllowed("user")
	@PermitAll
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	public List<StorageProfileDto> getStorageProfiles() {
		for (StorageProfileDto storageProfileDto : cipherduckConfig.inMemoryStorageConfigs) {
			// inject OAuth endpoints if STS
			if (storageProfileDto.stsRoleArn() != null) {
				storageProfileDto
						.withOauthAuthorizationUrl(cipherduckConfig.authEndpoint())
						.withOauthTokenUrl(cipherduckConfig.tokenEndpoint())
						.withOauthClientId(cipherduckConfig.keycloakClientIdCryptomator())
						.withoAuthTokenExchangeAudience(cipherduckConfig.keycloakClientIdCryptomatorVaults())
				;
			}
		}

		return cipherduckConfig.inMemoryStorageConfigs;
	}


}
