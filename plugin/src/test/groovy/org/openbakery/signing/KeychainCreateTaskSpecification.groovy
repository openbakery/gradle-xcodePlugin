package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.codesign.Security
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import spock.lang.Specification

class KeychainCreateTaskSpecification extends Specification {

	@Rule
	final TemporaryFolder tmpDirectory = new TemporaryFolder()

	Project project
	KeychainCreateTask keychainCreateTask

	CommandRunner commandRunner = Mock(CommandRunner)
	File keychainDestinationFile
	File certificateFile

	File loginKeychain

	def setup() {
		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		keychainCreateTask = project.tasks.findByName('keychainCreate')
		keychainCreateTask.commandRunner = commandRunner
		keychainCreateTask.security.commandRunner = commandRunner


		certificateFile = File.createTempFile("test", ".cert")
		keychainDestinationFile = new File(project.xcodebuild.signing.signingDestinationRoot, certificateFile.getName())

		loginKeychain = new File(tmpDirectory, "login.keychain")
		FileUtils.writeStringToFile(loginKeychain, "dummy")

		project.xcodebuild.type = Type.macOS
		project.xcodebuild.signing.certificateURI = certificateFile.toURL()
		project.xcodebuild.signing.certificatePassword = "password"
		project.xcodebuild.signing.timeout = null


	}

	def cleanup() {
		certificateFile.delete()
		new File(project.xcodebuild.signing.signingDestinationRoot, certificateFile.getName()).delete()
		project.xcodebuild.signing.keychainPathInternal.delete()
		FileUtils.deleteDirectory(tmpDirectory)
	}


	def "OSVersion"() {
		System.setProperty("os.version", "10.9.0");

		when:
		Version version = keychainCreateTask.getOSVersion()

		then:
		version != null;
		version.major == 10
		version.minor == 9
		version.maintenance == 0
	}


	def "create with OS X 10.8"() {
		given:
		System.setProperty("os.version", "10.8.0")

		Security security = Mock(Security)
		keychainCreateTask.security = security
		security.getKeychainList() >> []

		when:
		keychainCreateTask.create()

		then:
		1 * security.createKeychain(project.xcodebuild.signing.keychainPathInternal, "This_is_the_default_keychain_password")
		1 * security.importCertificate(keychainDestinationFile, "password",  project.xcodebuild.signing.keychainPathInternal)
		0 * security.setKeychainList([project.xcodebuild.signing.keychainPathInternal])
	}


	def mockListKeychains() {
		String result = "    \""+ loginKeychain.absolutePath + "\"";
		commandRunner.runWithResult( ["security", "list-keychains"]) >> result
	}

	def "create with OS X 10.9 adds keychain to list"() {
		given:
		System.setProperty("os.version", "10.9.0")

		Security security = Mock(Security)
		keychainCreateTask.security = security
		security.getKeychainList() >> []

		when:
		keychainCreateTask.create()

		then:
		1 * security.createKeychain(project.xcodebuild.signing.keychainPathInternal, "This_is_the_default_keychain_password")
		1 * security.importCertificate(keychainDestinationFile, "password",  project.xcodebuild.signing.keychainPathInternal)
		1 * security.setKeychainList([project.xcodebuild.signing.keychainPathInternal])
	}


	def "cleanup first"() {
		given:
		Security security = Mock(Security)
		keychainCreateTask.security = security
		security.getKeychainList() >> []
		File toBeDeleted = new File(project.xcodebuild.signing.signingDestinationRoot, "my.keychain")
		FileUtils.writeStringToFile(toBeDeleted, "dummy")
		mockListKeychains()

		when:
		keychainCreateTask.create()

		then:
		!toBeDeleted.exists()

	}


	def "depends on"() {
		when:
		def dependsOn = keychainCreateTask.getDependsOn()
		then:
		!dependsOn.contains(XcodePlugin.KEYCHAIN_CLEAN_TASK_NAME)
	}


	def "create with macOS 10.12.0 set-key-partition-list"() {
		given:
		System.setProperty("os.version", "10.12.0")

		Security security = Mock(Security)
		keychainCreateTask.security = security
		security.getKeychainList() >> []

		when:
		keychainCreateTask.create()

		then:
		1 * security.createKeychain(project.xcodebuild.signing.keychainPathInternal, "This_is_the_default_keychain_password")
		1 * security.importCertificate(keychainDestinationFile, "password",  project.xcodebuild.signing.keychainPathInternal)
		1 * security.setPartitionList(project.xcodebuild.signing.keychainPathInternal, "This_is_the_default_keychain_password")
	}


	def "create with macOS 10.11.0 NOT set-key-partition-list"() {
		given:
		System.setProperty("os.version", "10.11.0")

		Security security = Mock(Security)
		keychainCreateTask.security = security
		security.getKeychainList() >> []
		when:
		keychainCreateTask.create()

		then:
		1 * security.createKeychain(project.xcodebuild.signing.keychainPathInternal, "This_is_the_default_keychain_password")
		1 * security.importCertificate(keychainDestinationFile, "password",  project.xcodebuild.signing.keychainPathInternal)
		0 * security.setPartitionList(project.xcodebuild.signing.keychainPathInternal, "This_is_the_default_keychain_password")
	}

}
