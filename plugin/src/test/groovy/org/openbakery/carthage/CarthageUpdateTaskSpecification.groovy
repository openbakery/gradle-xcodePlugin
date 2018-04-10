package org.openbakery.carthage

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.output.ConsoleOutputAppender
import spock.lang.Specification

class CarthageUpdateTaskSpecification extends Specification {


	File projectDir
	Project project
	CarthageUpdateTask carthageUpdateTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		carthageUpdateTask = project.getTasks().getByPath('carthageUpdate')

		carthageUpdateTask.commandRunner = commandRunner

	}



	def "has carthageUpdate task"() {

		expect:
		carthageUpdateTask instanceof CarthageUpdateTask

	}

	def "verify that if carthage is not installed a exception is thrown"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> { throw new CommandRunnerException("Command failed to run (exit code 1):") }
		commandRunner.runWithResult("ls", "/usr/local/bin/carthage") >> { throw new CommandRunnerException("Command failed to run (exit code 1):") }

		when:
		carthageUpdateTask.update()

		then:
		def e = thrown(IllegalStateException)
		e.message.startsWith("The carthage command was not found. Make sure that Carthage is installed")
	}


	def "verify that if carthage is not installed at /usr/local/bin/carthage"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> { throw new CommandRunnerException("Command failed to run (exit code 1):") }

		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.runWithResult("ls", "/usr/local/bin/carthage")
	}

	def "verify that carthage is installed"() {
		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.runWithResult("which", "carthage")

	}

	def "run carthage update"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		carthageUpdateTask.update()


		then:
		1 * commandRunner.run(_, ["/usr/local/bin/carthage", "update", "--platform", "iOS"], _ ) >> {
			args -> args[2] instanceof ConsoleOutputAppender
		}

	}

	def "run update if Charthage exists for another platfrom"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		File carthageDirectory = new File(projectDir, "Carthage")
		File platformDirectory = new File(new File(carthageDirectory, "Build"), "tvOS")
		carthageDirectory.mkdirs()
		platformDirectory.mkdirs()

		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.run(_, ["/usr/local/bin/carthage", "update", "--platform", "iOS"], _ ) >> {
			args -> args[2] instanceof ConsoleOutputAppender
		}
	}

	def "skip update if Charthage exists"() {
		given:
		File carthageDirectory = new File(projectDir, "Carthage")
		File platformDirectory = new File(new File(carthageDirectory, "Build"), "iOS")
		carthageDirectory.mkdirs()
		platformDirectory.mkdirs()

		when:
		carthageUpdateTask.update()

		then:
		0 * commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"
	}

}
