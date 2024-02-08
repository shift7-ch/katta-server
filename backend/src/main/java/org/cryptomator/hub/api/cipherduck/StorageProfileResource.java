package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;

import java.net.URI;
import java.util.List;
import java.util.UUID;
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
	public Response uploadStorageProfile(final StorageProfileDto c) {
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
		return StorageProfileDto.findAll().<StorageProfileDto>stream().collect(Collectors.toList());
	}

	@GET
	@Path("/{profileId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "gets a storage profile")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "not an admin")
	public StorageProfileDto get(@PathParam("profileId") UUID profileId) {
		return StorageProfileDto.<StorageProfileDto>findByIdOptional(profileId).orElseThrow(NotFoundException::new);
	}

	@DELETE
	@Path("/{profileId}")
	@RolesAllowed("admin")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "archive a storage profile")
	@APIResponse(responseCode = "204", description = "storage profile archived")
	@APIResponse(responseCode = "403", description = "not an admin")
	public Response archive(@PathParam("profileId") UUID profileId) {
		// TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 (R3) archive flag instead - we must not delete storage profiles!
		if (StorageProfileDto.deleteById(profileId)) {
			return Response.status(Response.Status.NO_CONTENT).build();
		} else {
			throw new NotFoundException();
		}
	}

	// TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 (R3) refactor into meta service for getting which values are required for which protocol?
	@GET
	@Path("/meta")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	public VaultJWEPayloadDto getVaultJWEBackendDto(final StorageProfileDto.Protocol protocol) {
		// N.B. temporary workaround to have VaultJWEBackendDto exposed in openapi.json for now....
		return null;
	}
}
