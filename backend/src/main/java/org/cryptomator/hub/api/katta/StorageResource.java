package org.cryptomator.hub.api.katta;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cryptomator.hub.api.GoneException;
import org.cryptomator.hub.api.katta.storage.S3StorageHelper;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.cryptomator.hub.entities.Vault;
import org.cryptomator.hub.entities.katta.AccessTokenResponse;
import org.cryptomator.hub.entities.katta.StorageProfile;
import org.cryptomator.hub.entities.katta.StorageProfileS3Static;
import org.cryptomator.hub.katta.KeycloakCryptomatorVaultsHelper;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


@Path("/storage")
public class StorageResource {

	private static final Logger log = Logger.getLogger(StorageResource.class);

	@Inject
	KattaConfig kattaConfig;

	@Inject
	KeycloakCryptomatorVaultsHelper keycloakCryptomatorVaultsHelper;

	@Inject
	JsonWebToken jwt;

	@Inject
	Vault.Repository vaultRepo;

	@Inject
	User.Repository userRepo;

	@Inject
	Group.Repository groupRepo;

	@RestClient
	KeycloakTokenExchangeApi tokenExchangeApi;

	@Inject
	S3StorageHelper s3StorageHelper;

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates bucket and uploads vault template", description = "Creates the S3 bucket for the vault and uploads the (client-side encrypted) vault template on behalf of the Web Client, using the temporary STS credentials it supplies. This offloads bucket creation from the browser, which is subject to CORS restrictions. No CORS configuration or bucket policy is set here.")
	@APIResponse(responseCode = "200", description = "Bucket and Keycloak config created")
	@APIResponse(responseCode = "400", description = "Could not create bucket")
	@APIResponse(responseCode = "409", description = "Bucket with this name already exists")
	@APIResponse(responseCode = "410", description = "Storage profile is archived")
	public Response createBucket(@PathParam("vaultId") UUID vaultId, final CreateS3STSBucketDto storage) {
		final Map<UUID, StorageProfileDto> storageConfigs = StorageProfileS3Static.findAll().<StorageProfile>stream().map(StorageProfileDto::fromEntity).collect(Collectors.toMap(StorageProfileDto::id, Function.identity()));
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
		s3StorageHelper.makeS3Bucket((StorageProfileS3STSDto) storageProfileDto, storage);

		return Response.created(URI.create(".")).build();
	}

	@POST
	@Path("/s3-token")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "token exchange", description = "retrieves a downscoped access token for S3.")
	@APIResponse(responseCode = "200", description = "success")
	@APIResponse(responseCode = "400", description = "bad request")
	public AccessTokenResponse exchangeS3Token(@QueryParam("vault") String vault) {
		try {
			return tokenExchangeApi.exchange("urn:ietf:params:oauth:grant-type:token-exchange",
					jwt.getRawToken(),
					"urn:ietf:params:oauth:token-type:access_token",
					"urn:ietf:params:oauth:token-type:access_token",
					vault);
		} catch (WebApplicationException e) {
			log.error(e);
			throw new WebApplicationException(Response.status(e.getResponse().getStatus()).entity(String.format("Received: '%s', status code %s from Keycloak.", e.getResponse().getStatusInfo().getReasonPhrase(), e.getResponse().getStatus())).build());
		}
	}
}
