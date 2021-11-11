package org.openbakery.carthage

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.output.ConsoleOutputAppender
import spock.lang.Specification
import spock.lang.Unroll

import static org.openbakery.carthage.CarthageUpdateTask.*
import static org.openbakery.xcode.Type.*

class CarthageUpdateTaskSpecification extends Specification {

	File projectDir
	File cartfile
	Project project
	CarthageUpdateTask carthageUpdateTask;

	CommandRunner commandRunner = Mock(CommandRunner)


	def setup() {
		projectDir = File.createTempDir()

		cartfile = new File(projectDir, "Cartfile")
		cartfile << 'github "Alamofire/Alamofire"'

		project = ProjectBuilder.builder()
				.withProjectDir(projectDir)
				.build()

		project.buildDir = new File(projectDir, 'build').absoluteFile
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


	private List<String> getCommand(String carthagePlatform, String derivedDataPath = new File(project.xcodebuild.derivedDataPath, "carthage").absolutePath) {
		return [CARTHAGE_USR_BIN_PATH,
						ACTION_UPDATE,
						ARGUMENT_PLATFORM,
						carthagePlatform,
						ARGUMENT_CACHE_BUILDS,
						ARGUMENT_DERIVED_DATA,
						derivedDataPath
		]
	}


	def "command runner has console appender"() {
		def appender
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.run(_, getCommand(CARTHAGE_PLATFORM_IOS), _, _) >> {
			args -> appender = args[3]
		}
		appender instanceof ConsoleOutputAppender
	}



	def "run carthage update"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"
		project.xcodebuild.type = platform

		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.run(_, getCommand(carthagePlatform), _, _)

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
		carthageDirectory.mkdirs()

		when:
		carthageUpdateTask.update()

		then:
		1 * commandRunner.run(_, getCommand(CARTHAGE_PLATFORM_IOS), _, _)
	}

	def "does not update if cartfile is missing"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"
		project.xcodebuild.type = platform

		when:
		cartfile.delete()
		carthageUpdateTask.update()

		then:
		0 * commandRunner.run(_, getCommand(carthagePlatform), _, _)

		where:
		platform | carthagePlatform
		tvOS     | CARTHAGE_PLATFORM_TVOS
		macOS    | CARTHAGE_PLATFORM_MACOS
		watchOS  | CARTHAGE_PLATFORM_WATCHOS
		iOS      | CARTHAGE_PLATFORM_IOS
	}

	def "output directory should be relative to the xcodebuild platform type"() {
		when:
		project.xcodebuild.type = platform

		then:
		Provider<File> outputDirectory = carthageUpdateTask.getOutputDirectory()
		outputDirectory.isPresent()
		outputDirectory.get().name == carthagePlatform

		where:
		platform | carthagePlatform
		tvOS     | CARTHAGE_PLATFORM_TVOS
		macOS    | CARTHAGE_PLATFORM_MACOS
		watchOS  | CARTHAGE_PLATFORM_WATCHOS
		iOS      | CARTHAGE_PLATFORM_IOS
	}



	@Unroll
	def "has derived data path set to xcodebuild.derivedData + carthage "() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		project.xcodebuild.derivedDataPath = xcodebuildDerivedDataPath
		carthageUpdateTask.update()

		then:
		1 * commandRunner.run(_, getCommand(CARTHAGE_PLATFORM_IOS, derivedDataPathParameter), _, _)


		where:
		xcodebuildDerivedDataPath      | derivedDataPathParameter
		new File("foo")      | new File("foo/carthage").absolutePath
		new File("bar")      | new File("bar/carthage").absolutePath
		new File("foo/bar")  | new File("foo/bar/carthage").absolutePath
	}

}
