package org.cryptomator.hub.api.katta;

import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
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
	@Operation(summary = "create a storage profile", description = "Polymorphic by `protocol` discriminator: S3STATIC or S3STS. The server assigns the profile id; any client-supplied id is ignored.")
	@APIResponse(responseCode = "201", description = "uploaded storage configuration", content = @Content(schema = @Schema(implementation = StorageProfileDto.class)))
	@APIResponse(responseCode = "400", description = "Constraint violation")
	@APIResponse(responseCode = "403", description = "not an admin")
	public Response uploadStorageProfile(@Valid @NotNull final StorageProfileDto dto) {
		try {
			final StorageProfile entity = dto.toEntity();
			entity.id = UUID.randomUUID(); // server-assigned; any client-supplied id is ignored
			storageProfileRepo.persistAndFlush(entity);
			var result = StorageProfileDto.fromEntity(entity);
			return Response.created(URI.create(".")).contentLocation(URI.create(".")).entity(result).type(MediaType.APPLICATION_JSON).build();
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
	@Operation(summary = "set the archived state of a storage profile", description = "Archives (archived=true) or unarchives (archived=false) the storage profile. While archived, no new vaults can be created for this profile.")
	@APIResponse(responseCode = "204", description = "archived state updated")
	@APIResponse(responseCode = "403", description = "not an admin")
	public Response archive(@PathParam("profileId") UUID profileId, @NotNull @FormParam("archived") final boolean archived) {
		final StorageProfile entity = storageProfileRepo.findByIdOptional(profileId).orElseThrow(NotFoundException::new);
		storageProfileRepo.persistAndFlush(entity.setArchived(archived));
		return Response.status(Response.Status.NO_CONTENT).build();
	}
}
