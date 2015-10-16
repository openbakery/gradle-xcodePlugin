package org.openbakery.signing

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import spock.lang.Specification

/**
 * User: rene
 * Date: 14/10/15
 */
class KeychainRemoveFromSearchListSpecification extends Specification {

	Project project

	KeychainRemoveFromSearchListTask task

	String loginKeychain
	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {
		File projectDir = new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild/build').absoluteFile

		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME);
		task.commandRunner = commandRunner

		String userHome = System.getProperty("user.home")
		loginKeychain = userHome + "/Library/Keychains/login.keychain"

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
	}

	def "check group"() {
		when:
		true

		then:
		task.group == XcodePlugin.XCODE_GROUP_NAME
	}


	String getSecurityList() {

		return  "    \""+ loginKeychain + "\n" +
						"    \"/Users/me/Go/pipelines/Build-Appstore/build/codesign/gradle-1431356246879.keychain\"\n" +
						"    \"/Users/me/Go/pipelines/Build-Test/build/codesign/gradle-1431356877451.keychain\"\n" +
						"    \"/Users/me/Go/pipelines/Build-Continuous/build/codesign/gradle-1431419900260.keychain\"\n" +
						"    \"/Library/Keychains/System.keychain\""

	}

	def "remove"() {
		def commandList;

		given:
		commandRunner.runWithResult(["security", "list-keychains"]) >> getSecurityList()

		when:
		task.remove()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security", "list-keychains", "-s", loginKeychain]



	}
}
