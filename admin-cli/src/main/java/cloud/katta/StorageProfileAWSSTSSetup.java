package cloud.katta;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import picocli.CommandLine;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.Callable;

import static io.restassured.RestAssured.given;

@CommandLine.Command(name = "storageProfileAWSSTS",
		description = "Upload storage profile for AWS STS.",
		mixinStandardHelpOptions = true)
public class StorageProfileAWSSTSSetup implements Callable<Void> {

//	@CommandLine.Option(names = {"--tokenUrl"}, description = "Keycloak realm URL with scheme. Example: \"https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token\"", required = true)
//	String tokenUrl;

	@CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://testing.katta.cloud/tamarind\"", required = true)
	String hubUrl;

	@CommandLine.Option(names = {"--rolePrefix"}, description = "ARN Role Prefix. Example: \"arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind\"", required = true)
	String rolePrefix;

	@CommandLine.Option(names = {"--accessToken"}, description = "The access token. Requires admin role in the hub.", required = true)
	String accessToken;

	@CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket prefix.", required = false, defaultValue = "katta")
	String bucketPrefix;

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

		final JSONObject awsSTSTemplate = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/aws_sts/aws_sts_profile.json"), Charset.defaultCharset()));
		System.out.println(awsSTSTemplate);
		// arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-createbucket
		awsSTSTemplate.put("stsRoleArnHub", String.format("%s-createbucket", rolePrefix));
		awsSTSTemplate.put("stsRoleArnClient", String.format("%s-createbucket", rolePrefix));

		// arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-sts-chain-01
		awsSTSTemplate.put("stsRoleArn", String.format("%s-sts-chain-01", rolePrefix));

		// arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-sts-chain-02
		awsSTSTemplate.put("stsRoleArn2", String.format("%s-sts-chain-02", rolePrefix));
		awsSTSTemplate.put("id", uuid);
		awsSTSTemplate.put("bucketPrefix", bucketPrefix);

		System.out.println(awsSTSTemplate);
		final String response = given().header("Content-Type", "application/json")
				.header("Authorization", String.format("Bearer %s", accessToken))
				.when()
				.body(awsSTSTemplate.toString())
				.put(String.format("%s/api/storageprofile/s3sts", hubUrl))
				.then()
				.statusCode(201).extract().body().toString();
		System.out.println(response);

		// TODO other profiles
		//curl -v --fail -X PUT http://localhost:${HUB_PORT}/api/storageprofile/s3 -d @setup/aws_static/aws_static_profile.json -H "Content-Type: application/json" -H "Authorization: Bearer $$ACCESS_TOKEN"
		//curl -v --fail http://localhost:${HUB_PORT}/api/storageprofile/ -H "Authorization: Bearer $$ACCESS_TOKEN"

		return null;
	}

}
