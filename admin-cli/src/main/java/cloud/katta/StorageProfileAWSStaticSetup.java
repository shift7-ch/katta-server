package cloud.katta;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import picocli.CommandLine;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.Callable;

import static io.restassured.RestAssured.given;

@CommandLine.Command(name = "storageProfileAWSStatic",
		description = "Upload storage profile for AWS Static.",
		mixinStandardHelpOptions = true)
public class StorageProfileAWSStaticSetup implements Callable<Void> {

//	@CommandLine.Option(names = {"--tokenUrl"}, description = "Keycloak realm URL with scheme. Example: \"https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token\"", required = true)
//	String tokenUrl;

	@CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://testing.katta.cloud/tamarind\"", required = true)
	String hubUrl;

	@CommandLine.Option(names = {"--accessToken"}, description = "The access token. Requires admin role in the hub.", required = true)
	String accessToken;

	@Override
	public Void call() throws Exception {


// TODO extract
//		final String accessToken = given()
//				.header("Content-Type", "application/x-www-form-urlencoded")
//				.formParam("client_id", "cryptomator")
//				.formParam("grant_type", "password")
//				.formParam("username", "")
//				.formParam("password", "")
//				.when()
//				.post(tokenUrl)
//				.then()
//				.statusCode(200)
//				.extract().path("access_token");


		final String uuid = UUID.randomUUID().toString();
		final JSONObject awsSTSTemplate = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/aws_static/aws_static_profile.json"), Charset.defaultCharset()));
		awsSTSTemplate.put("id", uuid);
		final String response = given().header("Content-Type", "application/json")
				.header("Authorization", String.format("Bearer %s", accessToken))
				.when()
				.body(awsSTSTemplate.toString())
				.put(String.format("%s/api/storageprofile/s3", hubUrl))
				.then()
				.statusCode(201).extract().body().toString();
		System.out.println(response);

		// TODO other profiles
		//curl -v --fail -X PUT http://localhost:${HUB_PORT}/api/storageprofile/s3 -d @setup/aws_static/aws_static_profile.json -H "Content-Type: application/json" -H "Authorization: Bearer $$ACCESS_TOKEN"
		//curl -v --fail http://localhost:${HUB_PORT}/api/storageprofile/ -H "Authorization: Bearer $$ACCESS_TOKEN"

		return null;
	}
}
