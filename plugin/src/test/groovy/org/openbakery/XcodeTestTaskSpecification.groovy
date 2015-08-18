package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Created by rene on 18.08.15.
 */
class XcodeTestTaskSpecification extends Specification {
	Project project
	File projectDir
	XcodeTestTask task
	File infoPlist

	def commandRunner = Mock(CommandRunner)

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin


		projectDir.mkdirs()


		task = project.tasks.findByName(XcodePlugin.XCODE_TEST_TASK_NAME)
		task.commandRunner = commandRunner

		task.buildSpec.target = "Test"

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "process macosx test output"() {

		setup:
		def sourceFile = new File("src/test/Resource/xcodebuild-output-macosx.txt")
		def destinationFile = new File(project.getBuildDir(), "test/xcodebuild-output.txt")
		FileUtils.copyFile(sourceFile, destinationFile)
		task.buildSpec.sdk = XcodePlugin.SDK_MACOSX

		// because the available simulators were not moved to the buildSpec yet
		project.xcodebuild.sdk = XcodePlugin.SDK_MACOSX

		when:
		task.executeTask()

		then:
		new File(task.outputDirectory, "test-results.xml").exists()


	}
}
