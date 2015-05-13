package org.openbakery.signing

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 13.05.15.
 */
class KeychainRemoveFromSearchListTaskTest {


	KeychainRemoveFromSearchListTask task
	Project project
	File projectDir


	@BeforeMethod
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		task = project.tasks.findByName(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME)
	}

	@Test
	void checkGroup() {
		assert task.group == XcodePlugin.XCODE_GROUP_NAME
	}

	@Test
	void remove() {

		def commandRunnerMock = new MockFor(CommandRunner)

		commandRunnerMock.demand.runWithResult { parameters ->
			def expectedParameters = ["security", "list-keychains"]
			if (parameters.equals(expectedParameters)) {
				return FileUtils.readFileToString(new File("src/test/Resource/security-list.txt"))
			}
			println "parameters expected: " + expectedParameters
			println "but was: " + parameters
	 }

		commandRunnerMock.demand.run { parameters ->
			assert parameters.equals(["security", "list-keychains", "-s", "/Users/me/Library/Keychains/login.keychain"])
	 }

		task.commandRunner = commandRunnerMock.proxyInstance()
		task.remove()
		commandRunnerMock.verify task.commandRunner


	}
}
