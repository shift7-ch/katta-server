package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.stream.Collectors;

@Path("/backendsconfig")
public class BackendsConfigResource {


	@Inject
	BackendsConfig backendsConfig;

	@Inject
	CipherduckConfig cipherduckConfig;


	@GET
	@Path("/")
	@RolesAllowed("user")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "get configs for storage backends", description = "get list of configs for storage backends")
	@APIResponse(responseCode = "200", description = "uploaded storage configuration")
	public BackendsConfigDto getBackendsConfig() {
		return new BackendsConfigDto(Settings.get().hubId, backendsConfig.backends().stream()
				// TODO https://github.com/chenkins/cipherduck-hub/issues/41 hard-coded cryptomatorvaults
				.map(b -> new StorageConfigDto(b, new VaultJWEBackendDto(b.jwe(), cipherduckConfig.authEndpoint(), cipherduckConfig.tokenEndpoint(), cipherduckConfig.keycloakClientIdCryptomator(), "cryptomatorvaults")))
				.collect(Collectors.toList()));
	}


}
