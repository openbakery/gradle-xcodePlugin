package org.openbakery.signing

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import spock.lang.Specification

class KeychainCreateTaskSpecification extends Specification {

	Project project
	KeychainCreateTask keychainCreateTask

	CommandRunner commandRunner = Mock(CommandRunner)
	File keychainDestinationFile
	File certificateFile

	def setup() {
		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		keychainCreateTask = project.tasks.findByName('keychainCreate')
		keychainCreateTask.commandRunner = commandRunner

		certificateFile = File.createTempFile("test", ".cert")
		keychainDestinationFile = new File(project.xcodebuild.signing.signingDestinationRoot, certificateFile.getName())
	}

	def cleanup() {
		certificateFile.delete()
		new File(project.xcodebuild.signing.signingDestinationRoot, certificateFile.getName()).delete()
		project.xcodebuild.signing.keychainPathInternal.delete()
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
		System.setProperty("os.version", "10.8.0");

		project.xcodebuild.type = Type.OSX
		project.xcodebuild.signing.certificateURI = certificateFile.toURL()
		project.xcodebuild.signing.certificatePassword = "password"
		project.xcodebuild.signing.timeout = null

		when:
		keychainCreateTask.create()

		then:
		1 * commandRunner.run(["security", "create-keychain", "-p", "This_is_the_default_keychain_password", project.xcodebuild.signing.keychainPathInternal.toString()])
		1 * commandRunner.run(["security", "-v", "import",  keychainDestinationFile.toString(), "-k", project.xcodebuild.signing.keychainPathInternal.toString(), "-P", "password", "-T", "/usr/bin/codesign"])
	}

	def "create with OS X 10.9"() {
		given:
		System.setProperty("os.version", "10.9.0");
		project.xcodebuild.type = Type.OSX
		project.xcodebuild.signing.certificateURI = certificateFile.toURL()
		project.xcodebuild.signing.certificatePassword = "password"
		project.xcodebuild.signing.timeout = null

		project.xcodebuild.signing.keychainPathInternal.createNewFile()

		String result = "    \"" + AbstractKeychainTask.loginKeychainPath() + "\"";
		commandRunner.runWithResult( ["security", "list-keychains"]) >> result

		when:
		keychainCreateTask.create()

		then:
		1 * commandRunner.run(["security", "-v", "import",  keychainDestinationFile.toString(), "-k", project.xcodebuild.signing.keychainPathInternal.toString(), "-P", "password", "-T", "/usr/bin/codesign"])
		1 * commandRunner.run(["security", "list-keychains", "-s", AbstractKeychainTask.loginKeychainPath(), project.xcodebuild.signing.keychainPathInternal.toString()])
	}

}
