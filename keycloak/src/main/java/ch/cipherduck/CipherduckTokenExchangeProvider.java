package ch.cipherduck;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.DefaultTokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.representations.AccessToken;

public class CipherduckTokenExchangeProvider extends DefaultTokenExchangeProvider {

	private MultivaluedMap<String, String> formParams;

	private RealmModel realm;

	@Override
	public Response exchange(TokenExchangeContext context) {
		this.formParams = context.getFormParams();
		this.realm = context.getRealm();
		return super.exchange(context);
	}


	@Override
	protected Response exchangeClientToClient(UserModel targetUser, UserSessionModel targetUserSession,
											  AccessToken token, boolean disallowOnHolderOfTokenMismatch) {


		if (formParams.containsKey(OAuth2Constants.SCOPE) &&
				formParams.get(OAuth2Constants.SCOPE).size() == 1 &&
				formParams.containsKey(OAuth2Constants.AUDIENCE) &&
				formParams.get(OAuth2Constants.AUDIENCE).size() == 1
		) {
			final String scope = formParams.getFirst(OAuth2Constants.SCOPE);
			final String audience = formParams.getFirst(OAuth2Constants.AUDIENCE);
			final ClientModel targetClient = realm.getClientByClientId(audience);
			if (targetClient != null) {
				return exchangeClientToOIDCClient(targetUser, targetUserSession, OAuth2Constants.ACCESS_TOKEN_TYPE, targetClient, audience, scope);
			}
		}
		return super.exchangeClientToClient(targetUser, targetUserSession, token, disallowOnHolderOfTokenMismatch);
	}
}
