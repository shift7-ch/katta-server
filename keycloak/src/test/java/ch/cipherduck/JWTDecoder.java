package ch.cipherduck;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

public class JWTDecoder {
	static JSONObject deocdeJWT(String token) throws JSONException {
		String[] chunks = token.split("\\.");
		Base64.Decoder decoder = Base64.getUrlDecoder();

		String payload = new String(decoder.decode(chunks[1]));
		return new JSONObject(payload);
	}
}
