package org.cryptomator.hub.api.katta;

import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.entities.katta.StorageProfile;
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
	KattaConfig kattaConfig;

	@Inject
	StorageProfile.Repository storageProfileRepo;


	@POST
	@Path("/")
	@RolesAllowed("admin")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "create a storage profile", description = "Polymorphic by `protocol` discriminator: S3STATIC or S3STS.")
	@APIResponse(responseCode = "201", description = "uploaded storage configuration")
	@APIResponse(responseCode = "400", description = "Constraint violation")
	@APIResponse(responseCode = "403", description = "not an admin")
	@APIResponse(responseCode = "409", description = "Storage profile with ID already exists")
	public Response uploadStorageProfile(@Valid @NotNull final StorageProfileDto dto) {
		try {
			final StorageProfile entity = dto.toEntity();
			if (storageProfileRepo.findByIdOptional(entity.id).isPresent()) {
				throw new ClientErrorException(Response.Status.CONFLICT);
			}
			storageProfileRepo.persistAndFlush(entity);
			return Response.created(URI.create(".")).contentLocation(URI.create(".")).entity(entity).type(MediaType.APPLICATION_JSON).build();
		} catch (ConstraintViolationException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
		}
	}

	@GET
	@Path("/")
	@RolesAllowed({"user", "admin"})
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "list of storage configuration")
	@APIResponse(responseCode = "403", description = "not a user")
	public List<StorageProfileDto> getStorageProfiles(@Nullable @QueryParam("archived") Boolean archived) {
		return storageProfileRepo.findAll().stream().map(StorageProfileDto::fromEntity).filter(dto -> (archived == null) || archived.equals(dto.isArchived())).collect(Collectors.toList());
	}

	@GET
	@Path("/{profileId}")
	@RolesAllowed({"user", "admin"})
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "gets a storage profile")
	@APIResponse(responseCode = "200")
	@APIResponse(responseCode = "403", description = "not a user")
	public StorageProfileDto get(@PathParam("profileId") UUID profileId) {
		return StorageProfileDto.fromEntity(storageProfileRepo.findByIdOptional(profileId).orElseThrow(NotFoundException::new));
	}

	@PUT
	@Path("/{profileId}")
	@RolesAllowed("admin")
	@Transactional
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "archive a storage profile")
	@APIResponse(responseCode = "204", description = "storage profile archived")
	@APIResponse(responseCode = "403", description = "not an admin")
	public Response archive(@PathParam("profileId") UUID profileId, @FormParam("archived") final Boolean archived) {
		if (archived == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		final StorageProfile entity = storageProfileRepo.findByIdOptional(profileId).orElseThrow(NotFoundException::new);
		storageProfileRepo.persistAndFlush(entity.setArchived(archived));
		return Response.status(Response.Status.NO_CONTENT).build();
	}
}
