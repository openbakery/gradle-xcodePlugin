package org.openbakery.carthage

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.output.ConsoleOutputAppender
import spock.lang.Specification

import static org.openbakery.carthage.CarthageUpdateTask.*
import static org.openbakery.xcode.Type.*

class CarthageUpdateTaskSpecification extends Specification {

	File projectDir
	Project project
	CarthageUpdateTask carthageUpdateTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {
		projectDir = File.createTempDir()

		project = ProjectBuilder.builder()
				.withProjectDir(projectDir)
				.build()

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
		commandRunner.runWithResult("which", "carthage") >> {
			throw new CommandRunnerException("Command failed to run (exit code 1):")
		}
		commandRunner.runWithResult("ls", "/usr/local/bin/carthage") >> {
			throw new CommandRunnerException("Command failed to run (exit code 1):")
		}

		when:
		carthageUpdateTask.update()

		then:
		def e = thrown(IllegalStateException)
		e.message.startsWith("The carthage command was not found. Make sure that Carthage is installed")
	}


	def "verify that if carthage is not installed at /usr/local/bin/carthage"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> {
			throw new CommandRunnerException("Command failed to run (exit code 1):")
		}

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
		project.xcodebuild.type = platform

		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.run(_, [CARTHAGE_USR_BIN_PATH,
								  ACTION_UPDATE,
								  ARG_PLATFORM,
								  carthagePlatform,
								  ARG_CACHE_BUILDS], _) >> {
			args -> args[2] instanceof ConsoleOutputAppender
		}

		where:
		platform | carthagePlatform
		tvOS     | CARTHAGE_PLATFORM_TVOS
		macOS    | CARTHAGE_PLATFORM_MACOS
		watchOS  | CARTHAGE_PLATFORM_WATCHOS
		iOS      | CARTHAGE_PLATFORM_IOS
	}

	def "run update if Carthage exists for another platform"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> CARTHAGE_USR_BIN_PATH

		File carthageDirectory = new File(projectDir, "Carthage")
		File platformDirectory = new File(new File(carthageDirectory, "Build"), "tvOS")
		carthageDirectory.mkdirs()
		platformDirectory.mkdirs()

		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.run(_, [CARTHAGE_USR_BIN_PATH,
								  ACTION_UPDATE,
								  ARG_PLATFORM,
								  CARTHAGE_PLATFORM_IOS,
								  ARG_CACHE_BUILDS], _) >> {
			args -> args[2] instanceof ConsoleOutputAppender
		}
	}

	def "skip update if Carthage exists"() {
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
