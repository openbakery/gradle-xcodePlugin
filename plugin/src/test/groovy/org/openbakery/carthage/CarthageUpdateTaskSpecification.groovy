package org.openbakery.carthage

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.output.ConsoleOutputAppender
import spock.lang.Specification

/**
 * Created by rene on 18.08.16.
 */
class CarthageUpdateTaskSpecification extends Specification {


	Project project
	CarthageUpdateTask carthageUpdateTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		carthageUpdateTask = project.getTasks().getByPath('carthageUpdate')

		carthageUpdateTask.commandRunner = commandRunner

	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "has carthageUpdate task"() {

		expect:
		carthageUpdateTask instanceof CarthageUpdateTask

	}

	def "verify that if carthage is not installed a excpetion is thrown"() {
		given:
		commandRunner.run("which", "carthage") >> { throw new CommandRunnerException("Command failed to run (exit code 1):") }

		when:
		carthageUpdateTask.update()

		then:
		def e = thrown(IllegalStateException)
		e.message.startsWith("The carthage command was not found. Make sure that Carthage is installed")
	}

	def "verify that carthage is installed"() {
		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.run("which", "carthage")

	}

	def "run carthage update"() {
		when:
		carthageUpdateTask.update()


		then:
		1 * commandRunner.run(["carthage", "update"], _ ) >> {
			args -> args[1] instanceof ConsoleOutputAppender
		}

	}

}
