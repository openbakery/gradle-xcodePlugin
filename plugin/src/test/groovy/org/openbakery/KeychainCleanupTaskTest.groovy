package org.openbakery

import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.13
 * Time: 09:57
 * To change this template use File | Settings | File Templates.
 */
class KeychainCleanupTaskTest {

	Project project
	KeychainCleanupTask keychainCleanupTask


	GMockController mockControl = new GMockController()
	CommandRunner commandRunnerMock
	File keychainDestinationFile
	File certificateFile

	@BeforeClass
	def setup() {

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		keychainCleanupTask = project.tasks.findByName('keychain-clean')
		commandRunnerMock = mockControl.mock(CommandRunner)
		keychainCleanupTask.setProperty("commandRunner", commandRunnerMock)

		certificateFile = File.createTempFile("test", ".cert")
		certificateFile.deleteOnExit()
		keychainDestinationFile = new File(project.xcodebuild.signing.keychainDestinationRoot, certificateFile.getName())

	}

	void expectKeychainListCommand(result) {
		List<String> commandList
		commandList?.clear()
		commandList = ["security", "list-keychains"];
		commandRunnerMock.runCommandWithResult(commandList).returns(result).times(1)
	}

	void expectKeychainDeleteCommand() {
		List<String> commandList
		commandList?.clear()
		commandList = ["security", "delete-keychain", "/Library/Keychains/" + XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE + "delete-me.keychain"];
		commandRunnerMock.runCommand(commandList).times(1)
	}

	void expectKeychainListSetCommand() {
		List<String> commandList
		commandList?.clear()
		String userHome = System.getProperty("user.home")
		commandList = ["security", "list-keychains", "-s"]
		commandList.add(userHome + "/Library/Keychains/login.keychain")
		commandList.add("/Library/Keychains/System.keychain")
		commandRunnerMock.runCommand(commandList).times(1)
	}

	@Test
	void delete_keychain_os_x_10_8() {
		System.setProperty("os.version", "10.8.0");
		String userHome = System.getProperty("user.home")

		String result = "    \""+ userHome + "/Library/Keychains/login.keychain\"\n" +
										"    \"/Library/Keychains/" + XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE + "delete-me.keychain\"\n" +
										"    \"/Library/Keychains/System.keychain\"";

		expectKeychainListCommand(result)
		expectKeychainDeleteCommand()

		mockControl.play {
			keychainCleanupTask.clean()
		}
	}


	@Test
	void delete_keychain_os_x_10_9() {
		System.setProperty("os.version", "10.9.0");
		String userHome = System.getProperty("user.home")

		String result = "    \""+ userHome + "/Library/Keychains/login.keychain\"\n" +
										"    \"/Library/Keychains/" + XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE + "delete-me.keychain\"\n" +
										"    \"/Library/Keychains/System.keychain\"";

		expectKeychainListCommand(result)
		expectKeychainListSetCommand()

		mockControl.play {
			keychainCleanupTask.clean()
		}
	}
}
