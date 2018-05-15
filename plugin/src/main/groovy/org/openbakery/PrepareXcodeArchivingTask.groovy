package org.openbakery

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.util.PlistHelper

@CompileStatic
class PrepareXcodeArchivingTask extends DefaultTask {

	@InputFile
	@Optional
	final Provider<RegularFile> entitlementsFile = newInputFile()

	@OutputFile
	final Provider<RegularFile> outputFile = newOutputFile()

	final ListProperty<File> registeredProvisioningFiles = project.objects.listProperty(File)
	final Property<CommandRunner> commandRunnerProperty = project.objects.property(CommandRunner)
	final Property<File> provisioningForConfiguration = project.objects.property(File)
	final Property<PlistHelper> plistHelperProperty = project.objects.property(PlistHelper)
	final Property<ProvisioningProfileReader> provisioningReader = project.objects.property(ProvisioningProfileReader)
	final Property<String> certificateFriendlyName = project.objects.property(String)
	final Property<String> configurationBundleIdentifier = project.objects.property(String)
	final Property<String> entitlementsFilePath = project.objects.property(String)

	public static final String DESCRIPTION = "Prepare the archive configuration file"
	public static final String NAME = "prepareArchiving"

	static final String KEY_BUNDLE_IDENTIFIER = "PRODUCT_BUNDLE_IDENTIFIER"
	static final String KEY_CODE_SIGN_IDENTITY = "CODE_SIGN_IDENTITY"
	static final String KEY_CODE_SIGN_ENTITLEMENTS = "CODE_SIGN_ENTITLEMENTS"
	static final String KEY_DEVELOPMENT_TEAM = "DEVELOPMENT_TEAM"
	static final String KEY_PROVISIONING_PROFILE_ID = "PROVISIONING_PROFILE"
	static final String KEY_PROVISIONING_PROFILE_SPEC = "PROVISIONING_PROFILE_SPECIFIER"

	PrepareXcodeArchivingTask() {
		super()

		dependsOn(KeychainCreateTask.TASK_NAME)
		dependsOn(ProvisioningInstallTask.TASK_NAME)
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		dependsOn(XcodePlugin.INFOPLIST_MODIFY_TASK_NAME)

		this.description = DESCRIPTION

		this.entitlementsFilePath.set(entitlementsFile.map(new Transformer<String, RegularFile>() {
			@Override
			String transform(RegularFile regularFile) {
				return regularFile.asFile.absolutePath
			}
		}))

		this.provisioningForConfiguration.set(configurationBundleIdentifier.map(new Transformer<File, String>() {
			@Override
			File transform(String bundleIdentifier) {
				return ProvisioningProfileReader.getProvisionFileForIdentifier(bundleIdentifier,
						registeredProvisioningFiles.get().asList() as List<File>,
						commandRunnerProperty.get(),
						plistHelperProperty.get())
			}
		}))

		this.provisioningReader.set(provisioningForConfiguration.map(new Transformer<ProvisioningProfileReader, File>() {
			@Override
			ProvisioningProfileReader transform(File file) {
				return new ProvisioningProfileReader(file,
						commandRunnerProperty.get())
			}
		}))

		this.onlyIf {
			return certificateFriendlyName.present &&
					configurationBundleIdentifier.present &&
					provisioningForConfiguration.present &&
					outputFile.present &&
					provisioningForConfiguration.present
		}
	}

	@TaskAction
	void generate() {
		logger.debug("Preparing archiving")

		outputFile.get().asFile.text = ""

		append(KEY_CODE_SIGN_IDENTITY, certificateFriendlyName.get())
		append(KEY_BUNDLE_IDENTIFIER, configurationBundleIdentifier.get())

		if (provisioningReader.present) {
			ProvisioningProfileReader reader = provisioningReader.get()
			append(KEY_DEVELOPMENT_TEAM, reader.getTeamIdentifierPrefix())
			append(KEY_PROVISIONING_PROFILE_ID, reader.getUUID())
			append(KEY_PROVISIONING_PROFILE_SPEC, reader.getName())
		}

		if (entitlementsFilePath.present) {
			append(KEY_CODE_SIGN_ENTITLEMENTS, entitlementsFilePath.get())
		}
	}

	private void append(String key, String value) {
		outputFile.get()
				.asFile
				.append(System.getProperty("line.separator") + key + " = " + value)
	}

}
