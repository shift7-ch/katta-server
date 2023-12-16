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
import java.util.UUID;

@Path("/storageprofile")
public class StorageProfileResource {


	@Inject
	CipherduckConfig cipherduckConfig;


	@PUT
	@Path("/")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public void uploadStorageProfile(StorageProfileDto c) {
		c.withId(UUID.randomUUID());
		// TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 (R3) StorageProfileDto is not very transparent from the admin perspective - which fields are required for S3 static and which for S3 STS - we need some kind of meta-model (which could also be used for configuring which fields to show/hide in the ui).
		switch (c.protocol) {
			case s3:
				break;
			case s3sts:
				break;
		}
		cipherduckConfig.inMemoryStorageConfigs.add(c);

	}


	@GET
	@Path("/")
	// TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 (R3) restrict to admin after refctoring is done
//	@RolesAllowed("admin")
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
