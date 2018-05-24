package org.openbakery.signing

import de.undercouch.gradle.tasks.download.Download
import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.openbakery.CommandRunner
import org.openbakery.codesign.Security
import org.openbakery.util.FileUtil
import org.openbakery.util.SystemUtil
import org.openbakery.xcode.Version

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class KeychainCreateTask extends Download {

	@InputFile
	@Optional
	final RegularFileProperty certificateFile = newInputFile()

	@Input
	@Optional
	final Property<String> certificateUri = project.objects.property(String)

	@Input
	final Property<String> certificatePassword = project.objects.property(String)

	@Input
	final Property<Integer> keychainTimeout = project.objects.property(Integer)

	final Property<String> certificateFriendlyName = project.objects.property(String)

	final Property<CommandRunner> commandRunnerProperty = project.objects.property(CommandRunner)

	@OutputFile
	final RegularFileProperty keyChainFile = newOutputFile()

	final Property<Security> security = project.objects.property(Security)
	final DirectoryProperty outputDirectory = newOutputDirectory()

	private static final Pattern PATTERN = ~/^\s{4}friendlyName:\s(?<friendlyName>[^\n]+)/

	static final String TASK_NAME = "keychainCreate"
	static final String TASK_DESCRIPTION = "Create a keychain that is used for signing the app"
	static final String KEYCHAIN_DEFAULT_PASSWORD = "This_is_the_default_keychain_password"

	private File temporaryCertificateFile

	KeychainCreateTask() {
		super()
		this.description = TASK_DESCRIPTION
		onlyIf {
			if (!certificateFile.present && !certificateUri.present) {
				logger.warn("No signing certificate defined, will skip the keychain creation")
			}

			if (!certificatePassword.present) {
				logger.warn("No signing certificate password defined, will skip the keychain creation")
			}

			if (keyChainFile.present && keyChainFile.asFile.get().exists()) {
				logger.debug("Using keychain : " + keyChainFile.get())
			}

			return ((certificateFile.present || certificateUri.present) &&
					certificatePassword.present) ||
					(keyChainFile.present && keyChainFile.asFile.get().exists())
		}
	}

	@TaskAction
	void download() {
		if (certificateUri.present) {
			outputDirectory.get().asFile.mkdirs()
			configureDownload()
			super.download()
			resolveCertificateFile()
		} else {
			File file = certificateFile.asFile.get()
			temporaryCertificateFile = new File(outputDirectory.asFile.get(), file.name)
			FileUtils.copyFile(file, temporaryCertificateFile)
		}

		parseCertificateFile()
		createTemporaryCertificateFile()
		createKeyChainAndImportCertificate()
		addKeyChainToThePartitionList()
		setupOptionalTimeout()

		project.gradle.buildFinished {
			removeGradleKeychainsFromSearchList()
			deleteTemporaryKeyChainFile()
		}
	}

	private void configureDownload() {
		this.src(certificateUri.get())
		this.dest(outputDirectory.get().asFile)
		this.acceptAnyCertificate(true)
	}

	private void resolveCertificateFile() {
		temporaryCertificateFile = getOutputFiles().first()
	}

	private void parseCertificateFile() {
		certificateFriendlyName.set(getSignatureFriendlyName())

		// Delete on exit the downloaded files
		project.gradle.buildFinished {
			if (temporaryCertificateFile.exists()) {
				temporaryCertificateFile.delete()
			}
		}
	}

	private void createTemporaryCertificateFile() {
		temporaryCertificateFile = FileUtil.download(project,
				outputDirectory.asFile.get(),
				certificateUri.present
						? certificateUri.get()
						: certificateFile.asFile.get().toURI().toString())

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
	}

	private void addKeyChainToThePartitionList() {
		Version systemVersion = SystemUtil.getOsVersion()
		if (systemVersion.minor >= 9) {
			List<File> keychainList = getKeychainList()
			keychainList.add(keyChainFile.asFile.get())
			populateKeyChain(keychainList)
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

	List<File> getKeychainList() {
		return security.get().getKeychainList()
	}

	void populateKeyChain(List<File> keychainList) {
		security.get()
				.setKeychainList(keychainList)
	}

	private void deleteTemporaryKeyChainFile() {
		if (keyChainFile.asFile.get()) {
			keyChainFile.asFile.get().delete()

			logger.info("The temporary keychain file has been deleted")
		}
	}

	private void removeGradleKeychainsFromSearchList() {
		if (keyChainFile.present) {
			List<File> list = getKeychainList()
			list.removeIf { File file ->
				return file.absolutePath == keyChainFile.get().asFile.absolutePath
			}
			populateKeyChain(list)

			logger.info("The temporary keychain has been removed from the search list")
		}
	}

	String getSignatureFriendlyName() {
		return java.util.Optional.ofNullable(getKeyContent(temporaryCertificateFile)
				.split(System.getProperty("line.separator"))
				.find { PATTERN.matcher(it).matches() })
				.map { PATTERN.matcher(it) }
				.filter { Matcher it -> it.matches() }
				.map { Matcher it ->
			return it.group("friendlyName")
		}
		.orElseThrow {
			new IllegalArgumentException("Failed to resolve the code signing identity from the certificate ")
		}
	}

	private String getKeyContent(File file) {
		return commandRunnerProperty.get()
				.runWithResult(["openssl",
								"pkcs12",
								"-nokeys",
								"-in",
								file.absolutePath,
								"-passin",
								"pass:" + certificatePassword.get()])
	}
}
