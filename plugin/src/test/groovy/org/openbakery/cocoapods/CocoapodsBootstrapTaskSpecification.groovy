package org.openbakery.cocoapods

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import spock.lang.Specification

class CocoapodsBootstrapTaskSpecification extends Specification {


	Project project
	CocoapodsBootstrapTask cocoapodsBootstrapTask

	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {

		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin:org.openbakery.XcodePlugin

		cocoapodsBootstrapTask = project.getTasks().getByPath('cocoapodsBootstrap')
		cocoapodsBootstrapTask.commandRunner = commandRunner

	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "task executes pod install"() {

		when:
		cocoapodsBootstrapTask.bootstrap()

		then:
		1 * commandRunner.run(["gem", "install", "-N", "--user-install", "cocoapods"])

	}


	def "task executes pod setup"() {
		given:
		commandRunner.runWithResult("which", "pod") >> "/usr/local/bin/pod"

		when:
		cocoapodsBootstrapTask.bootstrap()

		then:
		1 * commandRunner.run(project.projectDir.absolutePath, ["/usr/local/bin/pod", "setup"], _)

	}


}
