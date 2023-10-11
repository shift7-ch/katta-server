package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.cryptomator.hub.api.cipherduck.storage.S3Storage.makeS3Bucket;

@Path("/storage")
public class StorageResource {
	private static final Logger LOG = Logger.getLogger(StorageResource.class);

	@Inject
	BackendsConfig backendsConfig;


	@PUT
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it.")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	@APIResponse(responseCode = "400", description = "Could not create bucket")
	public Response createBucket(StorageDto dto) {

		final Map<String, StorageConfig> storageConfigs = backendsConfig.backends().stream().collect(Collectors.toMap(StorageConfig::id, Function.identity()));
		final StorageConfig storageConfig = storageConfigs.get(dto.storageConfigId());

		makeS3Bucket(storageConfig, dto);
		return Response.created(URI.create(".")).build();
	}

}
