package org.openbakery

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.util.PathHelper

@CompileStatic
class PrepareXcodeArchivingTask extends AbstractXcodeBuildTask {

	private ProvisioningProfileReader reader

	public static final String NAME = "prepareArchiving"
	
	private static final String KEY_BUNDLE_IDENTIFIER = "PRODUCT_BUNDLE_IDENTIFIER"
	private static final String KEY_CODE_SIGN_IDENTITY = "CODE_SIGN_IDENTITY"
	private static final String KEY_DEVELOPMENT_TEAM = "DEVELOPMENT_TEAM"
	private static final String KEY_PROVISIONING_PROFILE_ID = "PROVISIONING_PROFILE"
	private static final String KEY_PROVISIONING_PROFILE_SPEC = "PROVISIONING_PROFILE_SPECIFIER"

	PrepareXcodeArchivingTask() {
		super()
		dependsOn(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME)
		dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		dependsOn(XcodePlugin.INFOPLIST_MODIFY_TASK_NAME)

		this.description = "Prepare the archive configuration file"
	}

	@Input
	@Override
	List<String> getProvisioningUriList() {
		return super.getProvisioningUriList()
	}

	@OutputFile
	File getXcConfigFile() {
		return PathHelper.resolveXcConfigFile(project)
	}

	@TaskAction
	void generate() {
		getXcConfigFile().text = ""

		computeProvisioningFile(getProvisioningFile()
				.orElseThrow { cannotResolveValidProvisioning() })
	}

	private IllegalArgumentException cannotResolveValidProvisioning() {
		return new IllegalArgumentException("Cannot resolve a valid provisioning " +
				"profile for bundle identifier : " +
				getBundleIdentifier())
	}

	private void computeProvisioningFile(File file) {
		reader = new ProvisioningProfileReader(file, commandRunner)
		append(KEY_BUNDLE_IDENTIFIER, reader.getApplicationIdentifier())
		append(KEY_CODE_SIGN_IDENTITY, getSignatureFriendlyName())
		append(KEY_DEVELOPMENT_TEAM, reader.getTeamIdentifierPrefix())
		append(KEY_PROVISIONING_PROFILE_ID, reader.getUUID())
		append(KEY_PROVISIONING_PROFILE_SPEC, reader.getName())
	}

	private void append(String key, String value) {
		getXcConfigFile()
				.append(System.getProperty("line.separator") + key + " = " + value)
	}
}
