package org.openbakery.configuration

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import spock.lang.Specification

/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfigTaskSpecification extends Specification {

	XcodeConfigTask xcodeConfigTask
	Project project
	CommandRunner commandRunner = Mock(CommandRunner)



	def setup() {
		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeConfigTask = project.getTasks().getByName(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		xcodeConfigTask.commandRunner = commandRunner

		project.xcodebuild.target = "Example"
	}

	def cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
		FileUtils.deleteDirectory(new File("build/Contents"))
	}

	def "non existing target"() {
		given:
		project.xcodebuild.target = "test"

		when:
		xcodeConfigTask.configuration()

		then:
		IllegalArgumentException exception = thrown()
		exception.message == "Target 'test' not found in project"

	}
}
