package org.cryptomator.hub.api.katta;

import io.quarkus.rest.client.reactive.ClientBasicAuth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.cryptomator.hub.entities.katta.AccessTokenResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "keycloak") // quarkus.rest-client.keycloak.url
@ClientBasicAuth(username = "${hub.keycloak.oidc.cryptomator-vaults-client-id}", password = "${hub.keycloak.oidc.cryptomator-vaults-client-secret}")
public interface KeycloakTokenExchangeApi {

	@POST
	@Path("protocol/openid-connect/token")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	AccessTokenResponse exchange(@FormParam("grant_type") String grantType,
								 @FormParam("subject_token") String subjectToken,
								 @FormParam("subject_token_type") String subjectTokenType,
								 @FormParam("requested_token_type") String requestedTokenType,
								 @FormParam("scope") String scope);
}
