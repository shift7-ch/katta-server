package cloud.katta;

import picocli.CommandLine;

@CommandLine.Command(name = "katta-admin-cli",
		mixinStandardHelpOptions = true,
		subcommands = {AwsSTSSetup.class, CommandLine.HelpCommand.class, StorageProfileAWSSTSSetup.class, StorageProfileAWSStaticSetup.class})
public class KattaSetupCli {

	public static void main(String... args) {
		var app = new KattaSetupCli();
		int exitCode = new CommandLine(app)
				.setPosixClusteredShortOptionsAllowed(false)
				.execute(args);
		System.exit(exitCode);
	}
}
