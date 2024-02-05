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
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
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
	private static final Logger LOG = Logger.getLogger(StorageResource.class);

	@Inject
	SyncerConfig syncerConfig;

	@Inject
	CipherduckConfig cipherduckConfig;

	@Inject
	JsonWebToken jwt;


	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it.")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	@APIResponse(responseCode = "400", description = "Could not create bucket")
	@APIResponse(responseCode = "409", description = "Vault with this ID already exists")
	public Response createBucket(@PathParam("vaultId") UUID vaultId, StorageDto storage) {
		Optional<Vault> vault = Vault.<Vault>findByIdOptional(vaultId);
		if (vault.isPresent()) {
			throw new ClientErrorException(String.format("Vault with ID %s already exists", vaultId), Response.Status.CONFLICT);
		}

		final Map<UUID, StorageProfileDto> storageConfigs = StorageProfileDto.findAll().<StorageProfileDto>stream().collect(Collectors.toMap(StorageProfileDto::id, Function.identity()));
		final StorageProfileDto storageProfile = storageConfigs.get(storage.storageConfigId());

		// N.B. if the bucket already exists, this will fail, so we do not prevent calling this method several times.
		makeS3Bucket(storageProfile, storage);

		final User currentUser = User.findById(jwt.getSubject());
		keycloakGrantAccessToVault(syncerConfig, vaultId.toString(), currentUser.id, cipherduckConfig.keycloakClientIdCryptomatorVaults());
		keycloakPrepareVault(syncerConfig, vaultId.toString(), storageProfile, jwt.getSubject(), cipherduckConfig.keycloakClientIdCryptomatorVaults());

		return Response.created(URI.create(".")).build();
	}

}
