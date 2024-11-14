package cloud.katta;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.DefaultTokenExchangeProviderFactory;
import org.keycloak.protocol.oidc.TokenExchangeProvider;

public class KattaTokenExchangeProviderFactory extends DefaultTokenExchangeProviderFactory {
    @Override
    public TokenExchangeProvider create(KeycloakSession session) {
        return new KattaTokenExchangeProvider();
    }

    @Override
    public String getId() {
        return "cipherduck-oauth2-token-exchange";
    }

    @Override
    public int order() {
        return 100;
    }
}
