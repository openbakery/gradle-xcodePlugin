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

	File tmpDirectory
	File loginKeychain

	def setup() {
		File projectDir = new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild')
		project.buildDir = new File(tmpDirectory, 'build').absoluteFile

		loginKeychain = new File(tmpDirectory, "login.keychain")
		FileUtils.writeStringToFile(loginKeychain, "dummy")


		project.apply plugin: org.openbakery.XcodePlugin

		keychainCleanupTask = project.tasks.findByName(XcodePlugin.KEYCHAIN_CLEAN_TASK_NAME);
		keychainCleanupTask.commandRunner = commandRunner


		certificateFile = File.createTempFile("test", ".cert")
		certificateFile.deleteOnExit()
		keychainDestinationFile = new File(project.xcodebuild.signing.signingDestinationRoot, certificateFile.getName())

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
		FileUtils.deleteDirectory(tmpDirectory)
	}

	def "delete keychain OS X 10.8"() {
		given:
		System.setProperty("os.version", "10.8.0");

		String result = "    \""+ loginKeychain.absolutePath + "\"\n" +
										"    \"/Library/Keychains/" + XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE + "delete-me.keychain\"\n" +
										"    \"/Library/Keychains/System.keychain\"";
		commandRunner.runWithResult(["security", "list-keychains"]) >> result

		when:
		keychainCleanupTask.clean()

		then:
		1 * commandRunner.run(["security", "list-keychains", "-s", loginKeychain.absolutePath])

	}

	def "delete keychain OS X 10.9"() {
		given:
		System.setProperty("os.version", "10.9.0");

		String result = "    \""+ loginKeychain.absolutePath  + "\"\n" +
										"    \"/Library/Keychains/" + XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE + "delete-me.keychain\"\n" +
										"    \"/Library/Keychains/System.keychain\"";

		commandRunner.runWithResult(["security", "list-keychains"]) >> result

		when:
		keychainCleanupTask.clean()

		then:
		1 * commandRunner.run(["security", "list-keychains", "-s", loginKeychain.absolutePath])

	}

	String getSecurityList() {
		return  "    \""+ loginKeychain.absolutePath  + "\n" +
						"    \"/Users/me/Go/pipelines/Build-Appstore/build/codesign/gradle-1431356246879.keychain\"\n" +
						"    \"/Users/me/Go/pipelines/Build-Test/build/codesign/gradle-1431356877451.keychain\"\n" +
						"    \"/Users/me/Go/pipelines/Build-Continuous/build/codesign/gradle-1431419900260.keychain\"\n" +
						"    \"/Library/Keychains/System.keychain\""

	}


	def "keychain list update"() {
		given:
		commandRunner.runWithResult(["security", "list-keychains"]) >> getSecurityList()

		when:
		keychainCleanupTask.removeGradleKeychainsFromSearchList()

		then:
		1 * commandRunner.run(["security", "list-keychains", "-s", loginKeychain.absolutePath])

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

		File keychainFile = new File(project.buildDir, "gradle.keychain");
		FileUtils.writeStringToFile(keychainFile, "dummy");

		String keychainFileName = keychainFile.absolutePath

		String result = "    \"" + loginKeychain.absolutePath + "\n" +
										"    \"" + keychainFileName + "\n" +
										"    \"/Library/Keychains/System.keychain\"";

		commandRunner.runWithResult(["security", "list-keychains"]) >> result


		def commandList

		when:
		keychainCleanupTask.removeGradleKeychainsFromSearchList()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security", "list-keychains", "-s", loginKeychain.absolutePath, keychainFileName]
		//1 * commandRunner.run(["security", "list-keychains", "-s", "/Users/me/Library/Keychains/login.keychain", keychainFileName])

	}

}
