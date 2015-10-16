package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.XcodePlugin
import spock.lang.Specification


class KeychainCleanupTaskSpecification extends Specification {

	Project project

	KeychainCleanupTask keychainCleanupTask

	CommandRunner commandRunner = Mock(CommandRunner)
	File keychainDestinationFile
	File certificateFile


	def setup() {
		File projectDir = new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild/build').absoluteFile

		project.apply plugin: org.openbakery.XcodePlugin

		keychainCleanupTask = project.tasks.findByName(XcodePlugin.KEYCHAIN_CLEAN_TASK_NAME);
		keychainCleanupTask.commandRunner = commandRunner


		certificateFile = File.createTempFile("test", ".cert")
		certificateFile.deleteOnExit()
		keychainDestinationFile = new File(project.xcodebuild.signing.signingDestinationRoot, certificateFile.getName())

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
	}

	def "delete keychain OS X 10.8"() {
		given:
		System.setProperty("os.version", "10.8.0");
		String userHome = System.getProperty("user.home")
		String loginKeychain = userHome + "/Library/Keychains/login.keychain"

		String result = "    \""+ loginKeychain + "\"\n" +
										"    \"/Library/Keychains/" + XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE + "delete-me.keychain\"\n" +
										"    \"/Library/Keychains/System.keychain\"";
		commandRunner.runWithResult(["security", "list-keychains"]) >> result

		when:
		keychainCleanupTask.clean()

		then:
		1 * commandRunner.run(["security", "list-keychains", "-s", loginKeychain])

	}

	def "delete keychain OS X 10.9"() {
		given:
		System.setProperty("os.version", "10.9.0");
		String userHome = System.getProperty("user.home")
		String loginKeychain = userHome + "/Library/Keychains/login.keychain"

		String result = "    \""+ userHome + "/Library/Keychains/login.keychain\"\n" +
										"    \"/Library/Keychains/" + XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE + "delete-me.keychain\"\n" +
										"    \"/Library/Keychains/System.keychain\"";

		commandRunner.runWithResult(["security", "list-keychains"]) >> result

		when:
		keychainCleanupTask.clean()

		then:
		1 * commandRunner.run(["security", "list-keychains", "-s", loginKeychain])

	}

	String getSecurityList() {
		String userHome = System.getProperty("user.home")
		String loginKeychain = userHome + "/Library/Keychains/login.keychain"

		return  "    \""+ loginKeychain + "\n" +
						"    \"/Users/me/Go/pipelines/Build-Appstore/build/codesign/gradle-1431356246879.keychain\"\n" +
						"    \"/Users/me/Go/pipelines/Build-Test/build/codesign/gradle-1431356877451.keychain\"\n" +
						"    \"/Users/me/Go/pipelines/Build-Continuous/build/codesign/gradle-1431419900260.keychain\"\n" +
						"    \"/Library/Keychains/System.keychain\""

	}


	def "keychain list update"() {
		given:
		String userHome = System.getProperty("user.home")
		String loginKeychain = userHome + "/Library/Keychains/login.keychain"

		commandRunner.runWithResult(["security", "list-keychains"]) >> getSecurityList()

		when:
		keychainCleanupTask.removeGradleKeychainsFromSearchList()

		then:
		1 * commandRunner.run(["security", "list-keychains", "-s", loginKeychain])

	}


	def "get keychain list"() {
		given:
		commandRunner.runWithResult(["security", "list-keychains"]) >> getSecurityList()

		when:
		List<String> keychainList = keychainCleanupTask.getKeychainList()

		then:
		keychainList.size == 1
	}


	def "remove only missing keychain in list"() {
		given:
		System.setProperty("os.version", "10.9.0");
		String userHome = System.getProperty("user.home")
		String loginKeychain = userHome + "/Library/Keychains/login.keychain"

		File keychainFile = new File(project.buildDir, "gradle.keychain");
		FileUtils.writeStringToFile(keychainFile, "dummy");

		String keychainFileName = keychainFile.absolutePath

		String result = "    \"" + loginKeychain + "\n" +
										"    \"" + keychainFileName + "\n" +
										"    \"/Library/Keychains/System.keychain\"";

		commandRunner.runWithResult(["security", "list-keychains"]) >> result


		def commandList

		when:
		keychainCleanupTask.removeGradleKeychainsFromSearchList()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security", "list-keychains", "-s", loginKeychain, keychainFileName]
		//1 * commandRunner.run(["security", "list-keychains", "-s", "/Users/me/Library/Keychains/login.keychain", keychainFileName])

	}

}
