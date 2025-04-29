package cloud.katta;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateOpenIdConnectProviderRequest;
import software.amazon.awssdk.services.iam.model.CreateOpenIdConnectProviderResponse;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleResponse;
import software.amazon.awssdk.services.iam.model.ListOpenIdConnectProvidersResponse;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.OpenIDConnectProviderListEntry;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.UpdateAssumeRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.UpdateOpenIdConnectProviderThumbprintRequest;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "awsSetup",
		description = "Setup/update OIDC provider and roles for STS.",
		mixinStandardHelpOptions = true)
public class AwsSTSSetup implements Callable<Void> {

	@CommandLine.Option(names = {"--realmUrl"}, description = "Keycloak realm URL with scheme.", required = true)
	String realmUrl;

	@CommandLine.Option(names = {"--profileName"}, description = "AWS profile to load AWS credentials from.", required = true)
	String profileName;

	@CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket Prefix for STS vaults.", required = false, defaultValue = "katta")
	String bucketPrefix;

	@CommandLine.Option(names = {"--maxSessionDuration"}, description = "Bucket Prefix for STS vaults.", required = false)
	Integer maxSessionDuration;

	@Override
	public Void call() throws Exception {
		final String arnPostfix = realmUrl.replace("https://", "");
		final String arnPostfixSanitized = arnPostfix.replace("/", "-");

		final URL url = new URI(realmUrl).toURL();

		final String sha = getThumbprint(url);
		System.out.println(sha);
		final Region region = Region.AWS_GLOBAL;

		try (final IamClient iam = IamClient.builder()
				.region(region)
				.credentialsProvider(ProfileCredentialsProvider.create(profileName))
				.build()) {
			final ListOpenIdConnectProvidersResponse existingOpenIdConnectProviders = iam.listOpenIDConnectProviders();
			System.out.println(existingOpenIdConnectProviders);


			final Optional<OpenIDConnectProviderListEntry> existingOIDCProvider = existingOpenIdConnectProviders.openIDConnectProviderList().stream().filter(idp -> idp.arn().endsWith(arnPostfix)).findFirst();

			//		aws iam create-open-id-connect-provider --url https://testing.hub.cryptomator.org/kc/realms/cipherduck --client-id-list cryptomator cryptomatorhub  --thumbprint-list BE21B29075BF9F3265353F8B85208A8981DAEC2A
			final String oidcProviderArn;
			if (existingOIDCProvider.isEmpty()) {
				final CreateOpenIdConnectProviderResponse openIDConnectProvider = iam.createOpenIDConnectProvider(CreateOpenIdConnectProviderRequest.builder()
						.url(realmUrl)
						.clientIDList("cryptomator", "cryptomatorhub")
						.thumbprintList(sha)
						.build());
				oidcProviderArn = openIDConnectProvider.openIDConnectProviderArn();
				System.out.println(oidcProviderArn);
			} else {
				oidcProviderArn = existingOIDCProvider.get().arn();
				iam.updateOpenIDConnectProviderThumbprint(UpdateOpenIdConnectProviderThumbprintRequest.builder()
						.openIDConnectProviderArn(oidcProviderArn)
						.thumbprintList(sha).build());

			}
			System.out.println(oidcProviderArn);
			final String arnPrefix = oidcProviderArn.replace(":oidc-provider" + "/" + arnPostfix, "");


			//		aws iam create-role --role-name cipherduck-createbucket --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws_stscreatebuckettrustpolicy.json
			//		aws iam put-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket --policy-document file://src/main/resources/cipherduck/setup/aws_stscreatebucketpermissionpolicy.json
			final String awsSTSCreateBucketRoleName = String.format("%s-createbucket", arnPostfixSanitized);
			final JSONObject awsSTSCreateBuckeTrustPolicyTemplate = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/aws_sts/createbuckettrustpolicy.json"), Charset.defaultCharset()));
			final JSONObject awsSTSCreateBuckePermissionPolicyTemplate = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/aws_sts/createbucketpermissionpolicy.json"), Charset.defaultCharset()));
			injectFederated(awsSTSCreateBuckeTrustPolicyTemplate, oidcProviderArn);
			injectBucketPrefixIntoResources(awsSTSCreateBuckePermissionPolicyTemplate, bucketPrefix);
			uploadAssumeRolePolicyAndPermissionPolicy(iam, awsSTSCreateBucketRoleName, awsSTSCreateBuckeTrustPolicyTemplate, awsSTSCreateBuckePermissionPolicyTemplate, maxSessionDuration);

			// TODO inject MaxSessionDuration


			//		aws iam create-role --role-name cipherduck_chain_01 --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws_stscipherduck_chain_01_trustpolicy.json
			//		aws iam put-role-policy --role-name cipherduck_chain_01 --policy-name cipherduck_chain_01 --policy-document file://src/main/resources/cipherduck/setup/aws_stscipherduck_chain_01_permissionpolicy.json
			//
			final String awsSTSChain01RoleName = String.format("%s-sts-chain-01", arnPostfixSanitized);
			final String awsSTSChain02RoleName = String.format("%s-sts-chain-02", arnPostfixSanitized);
			final JSONObject awsSTSChain01RoleNameTrustPolicyTemplate = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/aws_sts/cipherduck_chain_01_trustpolicy.json"), Charset.defaultCharset()));
			final JSONObject awsSTSChain01RoleNamePermissionPolicyTemplate = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/aws_sts/cipherduck_chain_01_permissionpolicy.json"), Charset.defaultCharset()));
			injectFederated(awsSTSChain01RoleNameTrustPolicyTemplate, oidcProviderArn);
			awsSTSChain01RoleNamePermissionPolicyTemplate.getJSONArray("Statement").getJSONObject(0).put("Resource", arnPrefix + ":role/" + awsSTSChain02RoleName);
			uploadAssumeRolePolicyAndPermissionPolicy(iam, awsSTSChain01RoleName, awsSTSChain01RoleNameTrustPolicyTemplate, awsSTSChain01RoleNamePermissionPolicyTemplate, maxSessionDuration);

			//		sleep 10;
			//
			//		aws iam create-role --role-name cipherduck_chain_02 --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws_stscipherduck_chain_02_trustpolicy.json
			//		aws iam put-role-policy --role-name cipherduck_chain_02 --policy-name cipherduck_chain_02 --policy-document file://src/main/resources/cipherduck/setup/aws_stscipherduck_chain_02_permissionpolicy.json
			//
			final JSONObject awsSTSChain02RoleNameTrustPolicyTemplate = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/aws_sts/cipherduck_chain_02_trustpolicy.json"), Charset.defaultCharset()));
			final JSONObject awsSTSChain02RoleNamePermissionPolicyTemplate = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/aws_sts/cipherduck_chain_02_permissionpolicy.json"), Charset.defaultCharset()));
			final GetRoleResponse role = iam.getRole(GetRoleRequest.builder().roleName(awsSTSChain01RoleName).build());
			awsSTSChain02RoleNameTrustPolicyTemplate.getJSONArray("Statement").getJSONObject(0).getJSONObject("Principal").put("AWS", Collections.singletonList(role.role().arn()));
			injectBucketPrefixIntoResources(awsSTSChain02RoleNamePermissionPolicyTemplate, bucketPrefix);
			uploadAssumeRolePolicyAndPermissionPolicy(iam, awsSTSChain02RoleName, awsSTSChain02RoleNameTrustPolicyTemplate, awsSTSChain02RoleNamePermissionPolicyTemplate, maxSessionDuration);
		}
		return null;
	}

	private static void injectBucketPrefixIntoResources(final JSONObject policy, final String bucketPrefix) {
		final JSONArray statements = policy.getJSONArray("Statement");
		for (int i = 0; i < statements.length(); i++) {
			final JSONArray resources = statements.getJSONObject(i).getJSONArray("Resource");
			for (int j = 0; j < resources.length(); j++) {
				resources.put(j, resources.getString(j).replace("cipherduck", bucketPrefix));
			}
		}
	}

	private static void injectFederated(final JSONObject policy, final String oidcProviderArn) {
		policy.getJSONArray("Statement").getJSONObject(0).getJSONObject("Principal").put("Federated", Collections.singletonList(oidcProviderArn));
	}

	private static void uploadAssumeRolePolicyAndPermissionPolicy(final IamClient iam, final String roleName, final JSONObject trustPolicyDocument, final JSONObject permissionPolicyDocument, final Integer maxSessionDuration) {
		System.out.println(trustPolicyDocument);
		System.out.println(permissionPolicyDocument);
		try {
			final GetRoleResponse role = iam.getRole(GetRoleRequest.builder().roleName(roleName).build());
			System.out.println(role);
			iam.updateAssumeRolePolicy(UpdateAssumeRolePolicyRequest.builder()
					.roleName(roleName)
					.policyDocument(trustPolicyDocument.toString())
					.build());

		} catch (NoSuchEntityException e) {
			iam.createRole(CreateRoleRequest.builder()
					.roleName(roleName)
					.assumeRolePolicyDocument(trustPolicyDocument.toString())
					.maxSessionDuration(maxSessionDuration)
					.build()
			);
		}
		iam.putRolePolicy(PutRolePolicyRequest.builder()
				.roleName(roleName)
				.policyName(roleName)
				.policyDocument(permissionPolicyDocument.toString())
				.build());
	}

	private static String getThumbprint(final URL url) throws IOException, CertificateEncodingException {
		//		openssl s_client -servername testing.hub.cryptomator.org -showcerts -connect testing.hub.cryptomator.org:443 > testing.hub.cryptomator.org.crt
		//
		//		vi testing.hub.cryptomator.org.crt ...
		//		(remove the irrelevant parts from the chain)
		//
		//		cat testing.hub.cryptomator.org.crt
		//				-----BEGIN CERTIFICATE-----
		//				MIIGBDCCBOygAwIBAgISA1CGKN3OkGJihg/qGhz2fl3fMA0GCSqGSIb3DQEBCwUA
		//		MDIxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MQswCQYDVQQD
		//				EwJSMzAeFw0yMzExMTIxMzAyMTdaFw0yNDAyMTAxMzAyMTZaMCYxJDAiBgNVBAMT
		//		G3Rlc3RpbmcuaHViLmNyeXB0b21hdG9yLm9yZzCCAiIwDQYJKoZIhvcNAQEBBQAD
		//				ggIPADCCAgoCggIBALWWmJr7lckOPCysl8p8FywJ2BwfCfdqMqTeb7KdOa3Zd9kb
		//		rb0dYUAs6cs4XKIxSBzKTDJAZiE5d2/iXUgHIBS8hDjG8U40EFaKDTc/JugOSovs
		//		HB6FQTi4YCMNfm3oMBiREMXYQTEKErBFfECbtGw8mTua2suT6Uc7lwj91qbPO6BN
		//				TROk0Az1NcifYOz8lMZhelg0WXEa10YfalaKGtjh4srMBv0rT85PpXaJXaNp58Ls
		//		4Psf/YlPjGJOhevnyAuqZouUD9sz7gZX8WvQ87y9uTXpDoarySh/0nppYLPZTDty
		//		sI3LeVwwrf4ir5jObVgjkH1CdS8kj/ueKLLW0BBqSX/9oji9o1zFJlBeRcWbeW08
		//		SD3+7292cy+zpNo3Y7xEFxGs0SVlJjTRk4cf6edkVq5QzTPqIF9FSn6tgXC6OTJi
		//		ISHnLGvkuSOzCieADPwjlYJiix3duK+0rpeN3xH3/NnyvPnncbWr/KLwwGE/tsHx
		//		orv1XLXkV0nmD9MDvE1gqRd7m7n3PwXEojz2Ih37i4bowFx2jYy6acAyY0KJSWwE
		//		3Rl2BRvOqXY1AOZC2MKOp7mb3hbryr8pzUPb0j4p3iOmOG9MgUQydKLyE97W1Ucd
		//		PRQMHdoG+EKnDeaauKdZ/3Lj0jMJ1CKlmYOB5qShHv1XCR5uimouioQkoJTFAgMB
		//				AAGjggIeMIICGjAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEG
		//		CCsGAQUFBwMCMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFHkBSFhuApvRJvGqRHZg
		//		5t183UMCMB8GA1UdIwQYMBaAFBQusxe3WFbLrlAJQOYfr52LFMLGMFUGCCsGAQUF
		//				BwEBBEkwRzAhBggrBgEFBQcwAYYVaHR0cDovL3IzLm8ubGVuY3Iub3JnMCIGCCsG
		//		AQUFBzAChhZodHRwOi8vcjMuaS5sZW5jci5vcmcvMCYGA1UdEQQfMB2CG3Rlc3Rp
		//				bmcuaHViLmNyeXB0b21hdG9yLm9yZzATBgNVHSAEDDAKMAgGBmeBDAECATCCAQUG
		//		CisGAQQB1nkCBAIEgfYEgfMA8QB2ADtTd3U+LbmAToswWwb+QDtn2E/D9Me9AA0t
		//		cm/h+tQXAAABi8PXIB0AAAQDAEcwRQIhAPOlsQr63JOSMbTFWOM746oA7i4HQ+hl
		//		p7M3pRpG4HYQAiBKqLSDsx1FdI18Fax3k7zkCgsY8x96ZAQvVUfdch0xoAB3AO7N
		//		0GTV2xrOxVy3nbTNE6Iyh0Z8vOzew1FIWUZxH7WbAAABi8PXIBwAAAQDAEgwRgIh
		//		AOZskIE18A5sTthKz6w3wMvIocbaoj3UCTCIAXWVJJNzAiEAmMWS709vLq/WOPG0
		//		5hb6lBPn6NRnjizJaNEnj/ts71EwDQYJKoZIhvcNAQELBQADggEBADiSgsGpOKqZ
		//		0kzeIS9x7vJlc3I0lnScB9JjxJyLoZFs//T4SNWE18zFxnzVspWRnwu4NTmuGURv
		//		6RWJ8RAznYwjZCnVDdQREUSX7wahzGdz+3GalRaIYngkvwHOhT+aGLbrKRjz+Pfh
		//		13qMStwjlfA6iSofHqVeQFCf48itgeVjNbpdZKEOLwdiV+JMwpT4n/i0nfVwWkaG
		//		RcEWn8S4gfSq1iZ/LAhWdyB0QJ4EcCO6mx02wABxbQibPc5FM8Q64j37TizHniVu
		//		hs+X7qFNDF/jvbob3sL09e0BLjiZWxVasAHiAAaZONTRV0N5YYV56F5br/vnegic
		//		u3AvSS5HW70=
		//				-----END CERTIFICATE-----
		//
		//
		//				openssl x509 -in testing.hub.cryptomator.org.crt -fingerprint -sha1 -noout | sed -e 's/://g' | sed -e 's/[Ss][Hh][Aa]1 [Ff]ingerprint=//'
		//		BE21B29075BF9F3265353F8B85208A8981DAEC2A
		//
		//		aws iam create-open-id-connect-provider --url https://testing.hub.cryptomator.org/kc/realms/cipherduck --client-id-list cryptomator cryptomatorhub  --thumbprint-list BE21B29075BF9F3265353F8B85208A8981DAEC2A
		//		{
		//			"OpenIDConnectProviderArn": "arn:aws:iam::930717317329:oidc-provider/testing.hub.cryptomator.org/kc/realms/cipherduck1"
		//		}
		//
		//		aws iam list-open-id-connect-providers
		//
		//		aws iam get-open-id-connect-provider --open-id-connect-provider-arn arn:aws:iam::930717317329:oidc-provider/testing.hub.cryptomator.org/kc/realms/cipherduck
		//		{
		//			"Url": "testing.hub.cryptomator.org/kc/realms/cipherduck",
		//				"ClientIDList": [
		//			"cryptomatorhub",
		//					"cryptomator"
		//    ],
		//			"ThumbprintList": [
		//			"a053375bfe84e8b748782c7cee15827a6af5a405"
		//    ],
		//			"CreateDate": "2023-11-13T13:51:32.729000+00:00",
		//				"Tags": []
		//		}
		HttpURLConnection.setFollowRedirects(false);
		final Certificate[] chain = getCertificates(url);

		// TODO is it always first cert?
		return DigestUtils.sha1Hex(chain[0].getEncoded());
	}

	private static Certificate[] getCertificates(final URL url) throws IOException {
		final HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
		c.setRequestMethod("HEAD"); // GET
		c.setDoOutput(false);
		c.setAllowUserInteraction(false);
		c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0 URLInspect/1.0");
		c.setRequestProperty("Connection", "close");

		c.connect(); // throws SSL handshake exception

		// retrieve TLS info before reading response (which closes connection?)
		return c.getServerCertificates();
	}
}
