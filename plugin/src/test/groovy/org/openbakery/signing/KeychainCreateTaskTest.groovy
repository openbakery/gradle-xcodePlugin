package org.openbakery.signing

import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openbakery.CommandRunner
import org.openbakery.Version

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 22.08.13
 * Time: 17:29
 */
class KeychainCreateTaskTest {

	Project project
	KeychainCreateTask keychainCreateTask


	GMockController mockControl = new GMockController()
	CommandRunner commandRunnerMock
	File keychainDestinationFile
	File certificateFile

	@Before
	void setup() {
		commandRunnerMock = mockControl.mock(CommandRunner)
		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		keychainCreateTask = project.tasks.findByName('keychainCreate')
		keychainCreateTask.setProperty("commandRunner", commandRunnerMock)

		certificateFile = File.createTempFile("test", ".cert")
		keychainDestinationFile = new File(project.xcodebuild.signing.signingDestinationRoot, certificateFile.getName())
	}

	@After
	void cleanAfterTest() {
		certificateFile.delete();
		new File(keychainCreateTask.buildSpec.signing.signingDestinationRoot, certificateFile.getName()).delete()
		keychainCreateTask.buildSpec.signing.keychainPathInternal.delete()
	}

	@Test
	void OSVersion() {
		System.setProperty("os.version", "10.9.0");

		Version version = keychainCreateTask.getOSVersion()
		assert version != null;

		assert version.major == 10
		assert version.minor == 9
		assert version.maintenance == 0
	}


	void expectKeychainCreateCommand() {
		List<String> commandList
		commandList?.clear()
		commandList = ["security", "create-keychain", "-p", "This_is_the_default_keychain_password", keychainCreateTask.buildSpec.signing.keychainPathInternal.toString()]
		commandRunnerMock.run(commandList).times(1)
	}

	void expectKeychainImportCommand() {
		List<String> commandList
		commandList?.clear()
		commandList = ["security", "-v", "import",  keychainDestinationFile.toString(), "-k", keychainCreateTask.buildSpec.signing.keychainPathInternal.toString(), "-P", "password", "-T", "/usr/bin/codesign"];
		commandRunnerMock.run(commandList).times(1)

	}

	void expectKeychainListCommand(result) {
		List<String> commandList
		commandList?.clear()
		commandList = ["security", "list-keychains"];
		commandRunnerMock.runWithResult(commandList).returns(result).times(1)
	}


	void expectKeychainListSetCommand() {

		List<String> commandList
		commandList?.clear()
		String userHome = System.getProperty("user.home")
		commandList = ["security", "list-keychains", "-s"]
		commandList.add(userHome + "/Library/Keychains/login.keychain")
		commandList.add(keychainCreateTask.buildSpec.signing.keychainPathInternal.toString())
		commandRunnerMock.run(commandList).times(1)

	}

	@Test
	void create_with_os_x_10_8() {
		System.setProperty("os.version", "10.8.0");
		project.xcodebuild.sdk = 'iphoneos'
		project.xcodebuild.signing.certificateURI = certificateFile.toURL()
		project.xcodebuild.signing.certificatePassword = "password"

		expectKeychainCreateCommand()
		expectKeychainImportCommand()

		mockControl.play {
			keychainCreateTask.executeTask()
		}

	}


	@Test
	void create_with_os_x_10_9() {
		System.setProperty("os.version", "10.9.0");
		project.xcodebuild.sdk = 'iphoneos'
		project.xcodebuild.signing.certificateURI = certificateFile.toURL()
		project.xcodebuild.signing.certificatePassword = "password"

		keychainCreateTask.buildSpec.signing.keychainPathInternal.createNewFile()
		expectKeychainImportCommand()

		String userHome = System.getProperty("user.home")
		String result = "    \""+ userHome + "/Library/Keychains/login.keychain\"";
		expectKeychainListCommand(result)
		expectKeychainListSetCommand()

		mockControl.play {
			keychainCreateTask.executeTask()
		}
	}


}
