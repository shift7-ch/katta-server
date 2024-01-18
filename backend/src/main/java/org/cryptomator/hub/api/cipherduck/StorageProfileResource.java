package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("/storageprofile")
public class StorageProfileResource {

	@Inject
	CipherduckConfig cipherduckConfig;

	@PUT
	@Path("/")
	@RolesAllowed("admin")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	public Response uploadStorageProfile(StorageProfileDto c) {
		// TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 (R3) protocol-specific validations?
		switch (c.protocol) {
			case s3:
				break;
			case s3sts:
				break;
		}
		try {
			c.persistAndFlush();
			return Response.created(URI.create(".")).build();
		} catch (ConstraintViolationException e) {
			throw new ClientErrorException(Response.Status.CONFLICT, e);
		}
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	public List<StorageProfileDto> getStorageProfiles() {
		List<StorageProfileDto> storageProfiles = StorageProfileDto.findAll().<StorageProfileDto>stream().collect(Collectors.toList());
		for (StorageProfileDto storageProfileDto : storageProfiles) {
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
		return storageProfiles;
	}

	// TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 (R3) refactor into meta service for getting which values are required for which protocol?
	@GET
	@Path("/meta")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	public VaultJWEBackendDto getVaultJWEBackendDto(final StorageProfileDto.Protocol protocol) {
		// N.B. temporary workaround to have VaultJWEBackendDto exposed in openapi.json for now....
		return null;
	}
}
