package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.cryptomator.hub.entities.AccessToken;
import org.cryptomator.hub.entities.Device;
import org.cryptomator.hub.entities.Authority;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

	@Inject
	JsonWebToken jwt;

	@PUT
	@Path("/me")
	@RolesAllowed("user")
	@Operation(summary = "get the logged-in user")
	@APIResponse(responseCode = "201", description = "user created")
	public Response syncMe() {
		Authority.createOrUpdate(jwt.getSubject(), jwt.getName(), jwt.getClaim("picture"), jwt.getClaim("email"));
		return Response.created(URI.create(".")).build();
	}

	@GET
	@Path("/me")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@NoCache
	@Transactional
	@Operation(summary = "get the logged-in user")
	public UserDto getMe(@QueryParam("withDevices") boolean withDevices, @QueryParam("withAccessibleVaults") boolean withAccessibleVaults) {
		Authority user = Authority.findById(new Authority.AuthorityId(jwt.getSubject(), Authority.AuthorityType.USER));
		Function<AccessToken, VaultResource.VaultDto> mapAccessibleVaults =
				a -> new VaultResource.VaultDto(a.vault.id, a.vault.name, a.vault.description, a.vault.creationTime, UserDto.fromEntity(a.vault.owner), null, null, null);
		Function<Device, DeviceResource.DeviceDto> mapDevices = withAccessibleVaults //
				? d -> new DeviceResource.DeviceDto(d.id, d.name, d.publickey, d.owner.id.getId(), d.accessTokens.stream().map(mapAccessibleVaults).collect(Collectors.toSet())) //
				: d -> new DeviceResource.DeviceDto(d.id, d.name, d.publickey, d.owner.id.getId(), Set.of());
		return withDevices //
				? new UserDto(user.id.getId(), user.name, user.details.pictureUrl, user.details.email, user.devices.stream().map(mapDevices).collect(Collectors.toSet()))
				: new UserDto(user.id.getId(), user.name, user.details.pictureUrl, user.details.email, Set.of());
	}

	@GET
	@Path("/")
	@RolesAllowed("vault-owner")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "list all users")
	public List<UserDto> getAll() {
		PanacheQuery<Authority> query = Authority.findAll();
		return query.stream().map(UserDto::fromEntity).toList();
	}

	public record UserDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") String pictureUrl, @JsonProperty("email") String email,
								 @JsonProperty("devices") Set<DeviceResource.DeviceDto> devices) {

		public static UserDto fromEntity(Authority user) {
			return new UserDto(user.id.getId(), user.name, user.details.pictureUrl, user.details.email, Set.of());
		}
	}

}