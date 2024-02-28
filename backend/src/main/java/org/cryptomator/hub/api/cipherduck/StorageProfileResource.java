package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.cipherduck.StorageProfile;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3STS;
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
	@Path("/s3")
	@RolesAllowed("admin")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	@APIResponse(responseCode = "400", description = "Constraint violation")
	@APIResponse(responseCode = "403", description = "not an admin")
	@APIResponse(responseCode = "409", description = "Storage profile with ID already exists")
	public Response uploadStorageProfile(final StorageProfileS3Dto c) {
		try {
			final StorageProfileS3 entity = c.toEntity();
			if(StorageProfile.findByIdOptional(entity.id).isPresent()){
				throw new ClientErrorException(Response.Status.CONFLICT);
			}
			entity.persistAndFlush();
			return Response.created(URI.create(".")).build();
		} catch (ConstraintViolationException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
		}
	}

	@PUT
	@Path("/s3sts")
	@RolesAllowed("admin")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	@APIResponse(responseCode = "400", description = "Constraint violation")
	@APIResponse(responseCode = "403", description = "not an admin")
	@APIResponse(responseCode = "409", description = "Storage profile with ID already exists")
	public Response uploadStorageProfile(final StorageProfileS3STSDto c) {
		try {
			final StorageProfileS3STS entity = c.toEntity();
			if(StorageProfile.findByIdOptional(entity.id).isPresent()){
				throw new ClientErrorException(Response.Status.CONFLICT);
			}
			entity.persistAndFlush();
			return Response.created(URI.create(".")).build();
		} catch (ConstraintViolationException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
		}
	}

	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	@APIResponse(responseCode = "403", description = "not a user")
	public List<StorageProfileDto> getStorageProfiles(@Nullable @QueryParam("archived") Boolean archived) {
		return StorageProfile.findAll().<StorageProfile>stream().map(StorageProfileDto::fromEntity).filter(p -> (null == archived) || (archived.booleanValue() == p.archived)).collect(Collectors.toList());
	}

	@GET
	@Path("/s3")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	@APIResponse(responseCode = "403", description = "not a user")
	public List<StorageProfileS3Dto> getStorageProfilesS3() {
		return StorageProfile.findAll().<StorageProfile>stream().map(StorageProfileDto::fromEntity).filter(StorageProfileS3Dto.class::isInstance).map(StorageProfileS3Dto.class::cast).collect(Collectors.toList());
	}

	@GET
	@Path("/{profileId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "gets a storage profile")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "not a user")
	public StorageProfileDto get(@PathParam("profileId") UUID profileId) {
		return StorageProfileDto.fromEntity(StorageProfile.<StorageProfile>findByIdOptional(profileId).orElseThrow(NotFoundException::new));
	}

	@PUT
	@Path("/{profileId}")
	@RolesAllowed("admin")
	@Transactional
	@Produces(MediaType.APPLICATION_FORM_URLENCODED)
	@Operation(summary = "archive a storage profile")
	@APIResponse(responseCode = "204", description = "storage profile archived")
	@APIResponse(responseCode = "403", description = "not an admin")
	public Response archive(@PathParam("profileId") UUID profileId, @FormParam("archived") final boolean archived) {
		final StorageProfile storageProfile = StorageProfile.<StorageProfile>findByIdOptional(profileId).orElseThrow(NotFoundException::new);
		storageProfile.setArchived(archived).persistAndFlush();
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	// TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 refactor into uvf vault metadata
	@GET
	@Path("/meta")
	@RolesAllowed("admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	public VaultMasterkeyJWEDto getVaultJWEBackendDto(final StorageProfileDto.Protocol protocol) {
		// N.B. temporary workaround to have VaultJWEBackendDto exposed in openapi.json for now....
		return null;
	}
}
