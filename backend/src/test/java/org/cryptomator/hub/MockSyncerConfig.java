package org.cryptomator.hub;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mockito;

import java.util.List;

// https://quarkus.io/guides/getting-started-testing#cdi-alternative-mechanism
@Mock
@ApplicationScoped
public class MockSyncerConfig extends SyncerConfig {

	@Override
	public Keycloak getKeycloak() {
		final Keycloak keycloakMock = Mockito.mock(Keycloak.class);
		final RealmResource realmResourceMock = Mockito.mock(RealmResource.class);
		final ClientsResource clientsResourceMock = Mockito.mock(ClientsResource.class);
		final ClientResource clientResourceMock = Mockito.mock(ClientResource.class);
		final ClientRepresentation clientRepresentationMock = Mockito.mock(ClientRepresentation.class);
		final ClientScopesResource clientScopesResourceMock = Mockito.mock(ClientScopesResource.class);
		final ClientScopeResource clientScopeResourceMock = Mockito.mock(ClientScopeResource.class);
		final RoleMappingResource roleMappingResourceMock = Mockito.mock(RoleMappingResource.class);
		final RoleScopeResource roleScopeResourceMock = Mockito.mock(RoleScopeResource.class);
		final Response responseMock = Mockito.mock(Response.class);
		final RolesResource rolesResourceMock = Mockito.mock(RolesResource.class);
		final RoleResource roleResourceMock = Mockito.mock(RoleResource.class);
		final RoleRepresentation roleRepresentationMock = Mockito.mock(RoleRepresentation.class);
		final UsersResource usersResourceMock = Mockito.mock(UsersResource.class);
		final UserResource userResourceMock = Mockito.mock(UserResource.class);
		final GroupsResource groupsResourceMock = Mockito.mock(GroupsResource.class);
		final GroupResource groupResourceMock = Mockito.mock(GroupResource.class);
		Mockito.when(keycloakMock.realm(Mockito.anyString())).thenReturn(realmResourceMock);
		Mockito.when(realmResourceMock.clients()).thenReturn(clientsResourceMock);
		Mockito.when(realmResourceMock.clientScopes()).thenReturn(clientScopesResourceMock);
		Mockito.when(clientScopesResourceMock.findAll()).thenReturn(List.of());
		Mockito.when(clientScopesResourceMock.create(Mockito.any())).thenReturn(responseMock);
		Mockito.when(clientScopesResourceMock.get(Mockito.anyString())).thenReturn(clientScopeResourceMock);
		Mockito.when(responseMock.getStatus()).thenReturn(201);
		Mockito.when(clientsResourceMock.findByClientId(Mockito.anyString())).thenReturn(List.of(clientRepresentationMock));
		Mockito.when(clientsResourceMock.get(Mockito.anyString())).thenReturn(clientResourceMock);
		Mockito.when(clientScopeResourceMock.getScopeMappings()).thenReturn(roleMappingResourceMock);
		Mockito.when(clientRepresentationMock.getId()).thenReturn("");
		Mockito.when(clientResourceMock.roles()).thenReturn(rolesResourceMock);
		Mockito.when(roleMappingResourceMock.clientLevel(Mockito.anyString())).thenReturn(roleScopeResourceMock);
		Mockito.when(rolesResourceMock.get(Mockito.anyString())).thenReturn(roleResourceMock);
		Mockito.when(roleResourceMock.toRepresentation()).thenReturn(roleRepresentationMock);
		Mockito.when(realmResourceMock.users()).thenReturn(usersResourceMock);
		Mockito.when(usersResourceMock.get(Mockito.anyString())).thenReturn(userResourceMock);
		Mockito.when(userResourceMock.roles()).thenReturn(roleMappingResourceMock);
		Mockito.when(realmResourceMock.groups()).thenReturn(groupsResourceMock);
		Mockito.when(groupsResourceMock.group(Mockito.anyString())).thenReturn(groupResourceMock);
		Mockito.when(groupResourceMock.roles()).thenReturn(roleMappingResourceMock);
		return keycloakMock;
	}
}