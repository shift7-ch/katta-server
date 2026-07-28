package org.cryptomator.hub.api.katta;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
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
import org.cryptomator.hub.entities.katta.StorageProfileS3STS;
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
	JsonWebToken jwt;

	@RestClient
	KeycloakTokenExchangeApi tokenExchangeApi;

	@Inject
	S3StorageHelper s3StorageHelper;

	@Inject
	StorageProfile.Repository storageProfileRepo;

	@PUT
	@Path("/{vaultId}")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it for call from Web Client (CORS).")
	@APIResponse(responseCode = "200", description = "Bucket and Keycloak config created")
	@APIResponse(responseCode = "400", description = "Could not create bucket")
	@APIResponse(responseCode = "409", description = "Bucket with this name already exists")
	@APIResponse(responseCode = "410", description = "Storage profile is archived")
	public Response createBucket(@PathParam("vaultId") UUID vaultId, final CreateS3STSBucketDto storage) {
		var storageProfileId = storage.storageConfigId();
		var storageProfile = storageProfileRepo.findById(storageProfileId);
		return switch (storageProfile) {
			case null -> throw new BadRequestException(String.format("Storage profile %s not found on this server", storageProfileId));
			case StorageProfileS3STS stsProfile when stsProfile.isArchived() -> throw new GoneException("Storage profile is archived.");
			case StorageProfileS3STS stsProfile when !stsProfile.isArchived() -> {
				// N.B. if the bucket already exists, this will fail, so we do not prevent calling this method several times.
				s3StorageHelper.makeS3Bucket(StorageProfileS3STSDto.fromEntity(stsProfile), storage);
				yield Response.created(URI.create(".")).build();
			}
			default -> throw new BadRequestException("Storage profile must be StorageProfileS3STSDto. Found" + storageProfile.getClass());
		};
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
