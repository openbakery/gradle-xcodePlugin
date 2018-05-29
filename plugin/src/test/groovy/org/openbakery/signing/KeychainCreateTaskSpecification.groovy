package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.XcodePlugin
import org.openbakery.codesign.Security
import org.openbakery.xcode.Type
import spock.lang.Specification
import spock.lang.Unroll

import static org.openbakery.signing.KeychainCreateTask.KEYCHAIN_DEFAULT_PASSWORD

class KeychainCreateTaskSpecification extends Specification {

	@Rule
	final TemporaryFolder tmpDirectory = new TemporaryFolder()

	KeychainCreateTask subject

	Project project

	CommandRunner commandRunner = Mock(CommandRunner)
	File keychainDestinationFile
	File certificateFile

	File loginKeychain
	File folder
	XcodeBuildPluginExtension xcodeBuildPluginExtension
	Security mockSecurity

	final static String CERTIFICATE_PASSWORD = "password"
	final static String FAKE_CERT_CONTENT = "Bag Attributes\n" +
			"    localKeyID: FE 93 19 AC CC D7 C1 AC 82 97 02 C2 35 97 B6 CE 37 33 CB 4F\n" +
			"    friendlyName: iPhone Distribution: Test Company Name (12345ABCDE)"

	def setup() {
		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: XcodePlugin

		mockSecurity = Mock(Security)

		subject = project.tasks.findByName('keychainCreate') as KeychainCreateTask
		subject.security.set(new Security(commandRunner))
		subject.commandRunnerProperty.set(commandRunner)

		xcodeBuildPluginExtension = project.extensions.getByType(XcodeBuildPluginExtension)
		folder = xcodeBuildPluginExtension.signing
				.signingDestinationRoot
				.get()
				.asFile

		certificateFile = tmpDirectory.newFile("test.cert")
		certificateFile.text = FAKE_CERT_CONTENT

		keychainDestinationFile = new File(folder,
				certificateFile.getName())

		loginKeychain = tmpDirectory.newFile("login.keychain")
		FileUtils.writeStringToFile(loginKeychain, "dummy")

		project.xcodebuild.type = Type.macOS
		project.xcodebuild.signing.certificatePassword = CERTIFICATE_PASSWORD
		project.xcodebuild.signing.timeout = null

		subject.security.set(mockSecurity)
		mockSecurity.getKeychainList() >> []

		commandRunner.runWithResult(_) >> FAKE_CERT_CONTENT
	}

	@Unroll
	def "Mac OS #version - Temporary keychain should be create and certificate URI imported"() {
		given:
		System.setProperty("os.version", version)
		project.xcodebuild.signing.certificate = certificateFile

		when:
		subject.download()

		then: "The `setPartitionList` method should be call only for OS version > 10.12"
		1 * mockSecurity.createKeychain(xcodeBuildPluginExtension.signing.keyChainFile.asFile.get(),
				KEYCHAIN_DEFAULT_PASSWORD)

		1 * mockSecurity.importCertificate(keychainDestinationFile,
				CERTIFICATE_PASSWORD,
				xcodeBuildPluginExtension.signing.keyChainFile.asFile.get())

		count * mockSecurity.setPartitionList(xcodeBuildPluginExtension.signing.keyChainFile.asFile.get(),
				KEYCHAIN_DEFAULT_PASSWORD)

		where:
		version   | count
		"10.8.0"  | 0
		"10.9.0"  | 0
		"10.11.0" | 0
		"10.12.0" | 1
		"11.12.0" | 1
		"11.12"   | 1
	}

	@Unroll
	def "Mac OS #version - Temporary keychain should be create and certificate File imported"() {
		given:
		System.setProperty("os.version", version)
		project.xcodebuild.signing.certificate = certificateFile

		when:
		subject.download()

		then: "The `setPartitionList` method should be call only for OS version > 10.12"
		1 * mockSecurity.createKeychain(xcodeBuildPluginExtension.signing.keyChainFile.asFile.get(),
				KEYCHAIN_DEFAULT_PASSWORD)

		1 * mockSecurity.importCertificate(keychainDestinationFile,
				CERTIFICATE_PASSWORD,
				xcodeBuildPluginExtension.signing.keyChainFile.asFile.get())

		count * mockSecurity.setPartitionList(xcodeBuildPluginExtension.signing.keyChainFile.asFile.get(),
				KEYCHAIN_DEFAULT_PASSWORD)

		where:
		version   | count
		"10.8.0"  | 0
		"10.9.0"  | 0
		"10.11.0" | 0
		"10.12.0" | 1
		"11.12.0" | 1
		"11.12"   | 1
	}

	@Unroll
	def "The keychain timeout should be called"() {
		given:
		project.xcodebuild.signing.certificate = certificateFile

		if (timeout)
			xcodeBuildPluginExtension.signing.timeout.set(timeout)

		when:
		subject.download()

		then:
		count * mockSecurity.setTimeout(timeout,
				xcodeBuildPluginExtension.signing.keyChainFile.asFile.get())

		where:
		timeout | count
		100     | 1
		400     | 1
		null    | 0
	}

	def "The temporary keychain file should be present post run"() {
		given:
		project.xcodebuild.signing.certificate = certificateFile

		when:
		subject.download()

		then:
		File file = xcodeBuildPluginExtension.signing
				.signingDestinationRoot
				.file(certificateFile.name)
				.get()
				.asFile
		file.exists()
		file.text == FAKE_CERT_CONTENT
	}
}
