package org.openbakery

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.util.PathHelper

import java.util.regex.Pattern

//@CompileStatic
class PrepareXcodeArchivingTask extends AbstractXcodeBuildTask {

	private ProvisioningProfileReader reader

	public static final String NAME = "prepareArchiving"
	public static final String FILE_NAME = "archive.xcconfig"

	private static final String KEY_BUNDLE_IDENTIFIER = "PRODUCT_BUNDLE_IDENTIFIER"
	private static final String KEY_CODE_SIGN_IDENTITY = "CODE_SIGN_IDENTITY"
	private static final String KEY_DEVELOPMENT_TEAM = "DEVELOPMENT_TEAM"
	private static final String KEY_PROVISIONING_PROFILE_ID = "PROVISIONING_PROFILE"
	private static final String KEY_PROVISIONING_PROFILE_SPEC = "PROVISIONING_PROFILE_SPECIFIER"
	private static final Pattern PATTERN = ~/^\s{4}friendlyName:\s(?<friendlyName>[^\n]+)/

	PrepareXcodeArchivingTask() {
		super()
		dependsOn(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME)
		dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		dependsOn(XcodePlugin.INFOPLIST_MODIFY_TASK_NAME)

		this.description = "Prepare the archive configuration file"
	}

	@Input
	List<String> getProvisioningUriList() {
		return getXcodeExtension().signing.mobileProvisionURI
	}

	@Input
	String getBundleIdentifier() {
		return getXcodeExtension().buildConfiguration.bundleIdentifier
	}

	Optional<File> getProvisioningFile() {
		List<File> provisioningList = getProvisioningUriList()
				.collect { it -> new File(new URI(it)) }

		return Optional.ofNullable(ProvisioningProfileReader.getProvisionFileForIdentifier(bundleIdentifier,
				provisioningList,
				commandRunner,
				plistHelper))
	}

	@OutputFile
	File getXcConfigFile() {
		return new File(PathHelper.resolveArchiveFolder(project), FILE_NAME)
	}

	@TaskAction
	void generate() {
		computeXcConfigFile()
	}

	private void computeXcConfigFile() {
		getXcConfigFile().text = ""

		computeProvisioningFile(getProvisioningFile()
				.orElseThrow {
			new IllegalArgumentException("Cannot resolve a valid provisioning " +
					"profile for bundle identifier : " +
					getBundleIdentifier())
		})
	}

	private void computeProvisioningFile(File file) {
		reader = new ProvisioningProfileReader(file, commandRunner)
		append(KEY_BUNDLE_IDENTIFIER, reader.getApplicationIdentifier())
		append(KEY_CODE_SIGN_IDENTITY, getCodeSignIdentity().orElseThrow {
			new IllegalArgumentException("Failed to resolve the code signing identity from the certificate ")
		})
		append(KEY_DEVELOPMENT_TEAM, reader.getTeamIdentifierPrefix())
		append(KEY_PROVISIONING_PROFILE_ID, reader.getUUID())
		append(KEY_PROVISIONING_PROFILE_SPEC, reader.getName())
	}

	private Optional<String> getCodeSignIdentity() {
		return Optional.ofNullable(getKeyContent()
				.split(System.getProperty("line.separator"))
				.find { PATTERN.matcher(it).matches() })
				.map { PATTERN.matcher(it) }
				.filter { it.matches() }
				.map { it.group("friendlyName") }
	}

	private void append(String key, String value) {
		getXcConfigFile()
				.append(System.getProperty("line.separator") + key + " = " + value)
	}

	private String getKeyContent() {
		File file = new File(URI.create(getXcodeExtension().signing.certificateURI))
		assert file.exists()
		return commandRunner.runWithResult(["openssl",
											"pkcs12",
											"-nokeys",
											"-in",
											file.absolutePath,
											"-passin",
											"pass:" + getXcodeExtension().signing.certificatePassword])
	}
}
