package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.SyncerConfig;
import org.cryptomator.hub.entities.VaultAccess;
import org.cryptomator.hub.filters.VaultRole;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.cryptomator.hub.api.cipherduck.storage.S3Storage.makeS3Bucket;
import static org.cryptomator.hub.cipherduck.KeycloakGrantAccessToVault.keycloakPrepareVault;

@Path("/storage")
public class StorageResource {
	private static final Logger LOG = Logger.getLogger(StorageResource.class);

	@Inject
	BackendsConfig backendsConfig;

	@Inject
	SyncerConfig syncerConfig;

	@Inject
	JsonWebToken jwt;


	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@VaultRole(VaultAccess.Role.OWNER) // may throw 403
	@Transactional
	@Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it.")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	@APIResponse(responseCode = "400", description = "Could not create bucket")
	public Response createBucket(@PathParam("vaultId") UUID vaultId, StorageDto dto) {

		// TODO https://github.com/chenkins/cipherduck-hub/issues/41 prevent overwriting?

		final Map<String, StorageConfig> storageConfigs = backendsConfig.backends().stream().collect(Collectors.toMap(StorageConfig::id, Function.identity()));
		final StorageConfig storageConfig = storageConfigs.get(dto.storageConfigId());

		makeS3Bucket(storageConfig, dto);

		// TODO https://github.com/chenkins/cipherduck-hub/issues/41 hard-coded cryptomatorvaults
		keycloakPrepareVault(syncerConfig, vaultId.toString(), storageConfig, jwt.getSubject(), "cryptomatorvaults");

		return Response.created(URI.create(".")).build();
	}

}
