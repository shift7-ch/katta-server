package org.cryptomator.hub.api.cipherduck;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/storageconfig")
public class StorageConfigResource {
    private static final Logger LOG = Logger.getLogger(StorageConfigResource.class);


    @Inject
    BackendsConfig backendsConfig;


    @GET
    @Path("/")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(summary = "creates bucket and policy", description = "creates an S3 bucket and uploads policy for it.")
    @APIResponse(responseCode = "200", description = "uploaded storage configuration")
    public BackendsConfig getStorageConfig() {
        return backendsConfig;
    }


}
