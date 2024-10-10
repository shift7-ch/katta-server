package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.SyncerConfig;
import org.cryptomator.hub.api.GoneException;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.cipherduck.StorageProfile;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.cryptomator.hub.api.cipherduck.storage.S3StorageHelper.makeS3Bucket;
import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakGrantAccessToVault;
import static org.cryptomator.hub.cipherduck.KeycloakCryptomatorVaultsHelper.keycloakPrepareVault;

@Path("/storage")
public class StorageResource {
	@Inject
	SyncerConfig syncerConfig;

	@Inject
	CipherduckConfig cipherduckConfig;

	@Inject
	JsonWebToken jwt;

	@Inject
	Vault.Repository vaultRepo;

	@Inject
	User.Repository userRepo;

	@Inject
	Group.Repository groupRepo;


	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it.")
	@APIResponse(responseCode = "200", description = "Bucket and Keycloak config created")
	@APIResponse(responseCode = "400", description = "Could not create bucket")
	@APIResponse(responseCode = "409", description = "Vault with this ID or bucket with this name already exists")
	@APIResponse(responseCode = "410", description = "Storage profile is archived")
	public Response createBucket(@PathParam("vaultId") UUID vaultId, final CreateS3STSBucketDto storage) {
		Optional<Vault> vault = vaultRepo.findByIdOptional(vaultId);
		if (vault.isPresent()) {
			throw new ClientErrorException(String.format("Vault with ID %s already exists", vaultId), Response.Status.CONFLICT);
		}

		final Map<UUID, StorageProfileDto> storageConfigs = StorageProfile.findAll().<StorageProfile>stream().map(StorageProfileDto::fromEntity).collect(Collectors.toMap(StorageProfileDto::id, Function.identity()));
		if (!storageConfigs.containsKey(storage.storageConfigId())) {
			return Response.status(Response.Status.BAD_REQUEST).entity(String.format("Storage profile %s not found on this server", storage.storageConfigId())).build();
		}
		final StorageProfileDto storageProfileDto = storageConfigs.get(storage.storageConfigId());
		if (storageProfileDto.archived) {
			throw new GoneException("Storage profile is archived.");
		}
		if (!(storageProfileDto instanceof StorageProfileS3STSDto)) {
			return Response.status(Response.Status.BAD_REQUEST).entity(String.format("Storage profile must be StorageProfileS3STSDto. Found %s", storageProfileDto.getClass().getName())).build();
		}

		// N.B. if the bucket already exists, this will fail, so we do not prevent calling this method several times.
		makeS3Bucket((StorageProfileS3STSDto) storageProfileDto, storage);

		final User currentUser = userRepo.findById(jwt.getSubject());
		keycloakGrantAccessToVault(syncerConfig, vaultId.toString(), currentUser.getId(), cipherduckConfig.keycloakClientIdCryptomatorVaults(), groupRepo);
		keycloakPrepareVault(syncerConfig, vaultId.toString(), (StorageProfileS3STSDto) storageProfileDto, jwt.getSubject());

		return Response.created(URI.create(".")).build();
	}
}
