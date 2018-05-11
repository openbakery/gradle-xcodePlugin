package org.openbakery

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.util.PathHelper

import java.util.function.Consumer

@CompileStatic
class PrepareXcodeArchivingTask extends AbstractXcodeBuildTask {

	private ProvisioningProfileReader reader
	private final File outputFile

	public static final String DESCRIPTION = "Prepare the archive configuration file"
	public static final String NAME = "prepareArchiving"

	private static final String KEY_BUNDLE_IDENTIFIER = "PRODUCT_BUNDLE_IDENTIFIER"
	private static final String KEY_CODE_SIGN_IDENTITY = "CODE_SIGN_IDENTITY"
	private static final String KEY_CODE_SIGN_ENTITLEMENTS = "CODE_SIGN_ENTITLEMENTS"
	private static final String KEY_DEVELOPMENT_TEAM = "DEVELOPMENT_TEAM"
	private static final String KEY_PROVISIONING_PROFILE_ID = "PROVISIONING_PROFILE"
	private static final String KEY_PROVISIONING_PROFILE_SPEC = "PROVISIONING_PROFILE_SPECIFIER"

	PrepareXcodeArchivingTask() {
		super()

		dependsOn(KeychainCreateTask.TASK_NAME)
		dependsOn(ProvisioningInstallTask.TASK_NAME)
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		dependsOn(XcodePlugin.INFOPLIST_MODIFY_TASK_NAME)

		this.description = DESCRIPTION
		this.outputFile = PathHelper.resolveXcConfigFile(project)
	}

	@Input
	@Override
	List<String> getProvisioningUriList() {
		return super.getProvisioningUriList()
	}

	@OutputFile
	File getXcConfigFile() {
		return outputFile
	}

	@TaskAction
	void generate() {
		getXcConfigFile().text = ""
		computeProvisioningFile()
	}

	private void computeProvisioningFile() {
		reader = new ProvisioningProfileReader(getProvisioningFile(), commandRunner)
		append(KEY_BUNDLE_IDENTIFIER, reader.getApplicationIdentifier())
		append(KEY_CODE_SIGN_IDENTITY, getSignatureFriendlyName())
		append(KEY_DEVELOPMENT_TEAM, reader.getTeamIdentifierPrefix())
		append(KEY_PROVISIONING_PROFILE_ID, reader.getUUID())
		append(KEY_PROVISIONING_PROFILE_SPEC, reader.getName())

		Optional.ofNullable(getXcodeExtension().signing.entitlementsFile)
				.filter { File file -> file.exists() }
				.ifPresent(new Consumer<File>() {
			@Override
			void accept(File file) {
				append(KEY_CODE_SIGN_ENTITLEMENTS, file.absolutePath)
			}
		})
	}

	private void append(String key, String value) {
		getXcConfigFile()
				.append(System.getProperty("line.separator") + key + " = " + value)
	}
}
