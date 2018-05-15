package org.openbakery.signing

import groovy.transform.CompileStatic
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.extension.Signing
import org.openbakery.util.FileUtil
import org.openbakery.util.SystemUtil
import org.openbakery.xcode.Version

@CompileStatic
class KeychainCreateTask extends AbstractKeychainTask {

	@InputFile
	final RegularFileProperty certificateFile = newInputFile()

	@Input
	final Property<String> certificatePassword = project.objects.property(String)

	@Input
	final Property<Integer> keychainTimeout = project.objects.property(Integer)

	static final String TASK_NAME = "keychainCreate"
	static final String TASK_DESCRIPTION = "Create a keychain that is used for signing the app"
	static final String KEYCHAIN_DEFAULT_PASSWORD = "This_is_the_default_keychain_password"

	private File temporaryCertificateFile

	KeychainCreateTask() {
		super()
		this.description = TASK_DESCRIPTION
		onlyIf {
			XcodeBuildPluginExtension extension = project
					.getExtensions()
					.findByType(XcodeBuildPluginExtension)

			Signing signing = extension.signing

			if (!signing.certificate.present) {
				logger.warn("No signing certificate defined, will skip the keychain creation")
			}

			if (!signing.certificatePassword.present) {
				logger.warn("No signing certificate password defined, will skip the keychain creation")
			}

			if (keyChainFile.present && keyChainFile.asFile.get().exists()) {
				logger.debug("Using keychain : " + keyChainFile.get())
			}

			return (signing.certificate.present &&
					signing.certificatePassword.present) ||
					(keyChainFile.present && keyChainFile.asFile.get().exists())
		}
	}

	@TaskAction
	void create() {
		createTemporaryCertificateFile()
		createKeyChainAndImportCertificate()
		addKeyChainToThePartitionList()
		setupOptionalTimeout()
	}

	private void createTemporaryCertificateFile() {
		temporaryCertificateFile = FileUtil.download(project,
				outputDirectory.asFile.get(),
				certificateFile.asFile.get().toURI().toString())

		// Delete the temporary file on completion
		project.gradle.buildFinished {
			if (temporaryCertificateFile.exists()) {
				temporaryCertificateFile.delete()
			}
		}
	}

	private void createKeyChainAndImportCertificate() {
		security.get()
				.createKeychain(keyChainFile.asFile.getOrNull(),
				KEYCHAIN_DEFAULT_PASSWORD)

		security.get()
				.importCertificate(temporaryCertificateFile,
				certificatePassword.get(),
				keyChainFile.asFile.get())

		// Delete the temporary keychain on completion
		project.gradle.buildFinished {
			if (keyChainFile.asFile.get()) {
				keyChainFile.asFile.get().delete()
			}
		}
	}

	private void addKeyChainToThePartitionList() {
		Version systemVersion = SystemUtil.getOsVersion()
		if (systemVersion.minor >= 9) {
			List<File> keychainList = getKeychainList()
			keychainList.add(keyChainFile.asFile.get())
			setKeychainList(keychainList)
		}

		if (systemVersion.minor >= 12) {
			security.get().setPartitionList(keyChainFile.asFile.get(),
					KEYCHAIN_DEFAULT_PASSWORD)
		}
	}

	private void setupOptionalTimeout() {
		if (keychainTimeout.present) {
			security.get()
					.setTimeout(keychainTimeout.get(),
					keyChainFile.asFile.get())
		}
	}
}
