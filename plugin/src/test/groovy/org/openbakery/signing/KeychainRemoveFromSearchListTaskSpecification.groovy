package org.openbakery.signing

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import spock.lang.Specification

/**
 * User: rene
 * Date: 14/10/15
 */
class KeychainRemoveFromSearchListTaskSpecification extends Specification {

	Project project

	KeychainRemoveFromSearchListTask task
	File tmpDirectory
	File loginKeychain
	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {
		File projectDir = new File("../example/iOS/ExampleWatchkit")

		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild')

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(tmpDirectory, 'build').absoluteFile

		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME);
		task.commandRunner = commandRunner
		task.security.commandRunner = commandRunner

		loginKeychain = new File(tmpDirectory, "login.keychain")
		FileUtils.writeStringToFile(loginKeychain, "dummy")


	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
		FileUtils.deleteDirectory(tmpDirectory)
	}

	def "check group"() {
		when:
		true

		then:
		task.group == XcodePlugin.XCODE_GROUP_NAME
	}


	String createSecurityListResult(String... keychainFiles) {

		StringBuilder builder = new StringBuilder();
		builder.append("    \"")
		builder.append(loginKeychain.absolutePath)
		builder.append("\"\n")


		for (String keychainFile in keychainFiles) {
			builder.append("    \"")
			builder.append(keychainFile)
			builder.append("\"\n")
		}

		builder.append("    \"/Library/Keychains/System.keychain\"")
		return builder.toString()
	}

	def "remove"() {
		def commandList;

		given:
		def securityList = createSecurityListResult(
						"/Users/me/Go/pipelines/Build-Appstore/build/codesign/gradle-1431356246879.keychain",
						"/Users/me/Go/pipelines/Build-Test/build/codesign/gradle-1431356877451.keychain",
						"/Users/me/Go/pipelines/Build-Continuous/build/codesign/gradle-1431419900260.keychain"
		)

		commandRunner.runWithResult(["security", "list-keychains"]) >> securityList

		when:
		task.remove()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security", "list-keychains", "-s", loginKeychain.absolutePath]

	}

	def "remove with existing files"() {
		def commandList;

		given:
		File dummyKeychain = new File(project.buildDir, "gradle-1234.keychain")
		FileUtils.writeStringToFile(dummyKeychain, "dummy");
		def securityList = createSecurityListResult(dummyKeychain.getAbsolutePath())
		commandRunner.runWithResult(["security", "list-keychains"]) >> securityList

		when:
		task.remove()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security", "list-keychains", "-s", loginKeychain.absolutePath, dummyKeychain.getAbsolutePath()]
	}


	def "remove current used keychain"() {
		def commandList;

		given:
		File dummyKeychain = new File(project.buildDir, "gradle-1234.keychain")
		FileUtils.writeStringToFile(dummyKeychain, "dummy");
		def securityList = createSecurityListResult(dummyKeychain.getAbsolutePath())
		commandRunner.runWithResult(["security", "list-keychains"]) >> securityList

		project.xcodebuild.signing.keychain = dummyKeychain

		when:
		task.remove()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security", "list-keychains", "-s", loginKeychain.absolutePath]
	}


	def "remove current used internal keychain"() {
		def commandList;

		given:
		project.xcodebuild.signing.signingDestinationRoot = project.buildDir

		FileUtils.writeStringToFile(project.xcodebuild.signing.keychainPathInternal, "dummy");
		def securityList = createSecurityListResult(project.xcodebuild.signing.keychainPathInternal.getAbsolutePath())
		commandRunner.runWithResult(["security", "list-keychains"]) >> securityList

		when:
		task.remove()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security", "list-keychains", "-s", loginKeychain.absolutePath]
	}



}
