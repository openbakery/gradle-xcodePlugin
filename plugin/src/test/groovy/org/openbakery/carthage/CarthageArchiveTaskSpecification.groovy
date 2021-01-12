package org.openbakery.carthage

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.openbakery.CommandRunner
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.XCConfig
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static org.openbakery.carthage.AbstractCarthageTaskBase.*
import static org.openbakery.xcode.Type.*

class CarthageArchiveTaskSpecification extends Specification {


	CarthageArchiveTask task
	CommandRunner commandRunner = Mock(CommandRunner)
	File projectDir
	File carthageFile
	Project project
	File xcconfigPath

	@Rule
	public ExpectedException exception = ExpectedException.none()

	void setup() {
		projectDir = File.createTempDir()

		carthageFile = new File(projectDir, "Cartfile")
		carthageFile << 'github "Alamofire/Alamofire"'

		project = ProjectBuilder.builder()
				.withProjectDir(projectDir)
				.build()

		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.getTasks().getByPath('carthageArchive') as CarthageArchiveTask
		assert task != null

		task.commandRunner = commandRunner

		File carthageDirectory = project.rootProject.file("Carthage")
		xcconfigPath = new File(carthageDirectory, "gradle-xc12-carthage.xcconfig")
	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}

	def "The carthage archive task should be present"() {
		expect:
		task instanceof CarthageArchiveTask
	}

	def "carthage archive task is executed when cartfile exists"() {
		expect:
		task.getOnlyIf().isSatisfiedBy(task)
	}

	def "carthage archive task is skipped when cartfile is missing"() {
		when:
		carthageFile.delete()

		then:
		!task.getOnlyIf().isSatisfiedBy(task)
	}


	private List<String> getCommandRunnerArgsForPlatform(String carthagePlatform) {
		return [CARTHAGE_USR_BIN_PATH,
						ACTION_BUILD,
						ARGUMENT_ARCHIVE,
						ARGUMENT_PLATFORM,
						carthagePlatform,
						ARGUMENT_DERIVED_DATA,
						new File(project.xcodebuild.derivedDataPath, "carthage").absolutePath]
	}


	@Unroll
	def "When bootstrap is executed should only update the platform: #platform"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"
		project.xcodebuild.type = platform

		when:
		task.archive()

		then:
		1 * commandRunner.run(_,
				getCommandRunnerArgsForPlatform(carthagePlatform),
				_,
				_) >> {
			args -> args[3] instanceof ConsoleOutputAppender
		}

		where:
		platform | carthagePlatform
		tvOS     | CARTHAGE_PLATFORM_TVOS
		macOS    | CARTHAGE_PLATFORM_MACOS
		watchOS  | CARTHAGE_PLATFORM_WATCHOS
		iOS      | CARTHAGE_PLATFORM_IOS
	}

	def "The task should not be executed if the 'Cartfile` file is missing"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"
		project.xcodebuild.type = platform

		when:
		carthageFile.delete()
		task.archive()

		then:
		0 * commandRunner.run(_,
				getCommandRunnerArgsForPlatform(carthagePlatform),
				_,
				_) >> {
			args -> args[3] instanceof ConsoleOutputAppender
		}

		where:
		platform | carthagePlatform
		tvOS     | CARTHAGE_PLATFORM_TVOS
		macOS    | CARTHAGE_PLATFORM_MACOS
		watchOS  | CARTHAGE_PLATFORM_WATCHOS
		iOS      | CARTHAGE_PLATFORM_IOS
	}




	def "The task output directory should be platform dependant"() {
		when:
		task.xcode.getXcodeSelectEnvironmentValue(_) >> new HashMap<String, String>()
		project.xcodebuild.type = platform

		then:
		Provider<File> outputDirectory = task.outputDirectory
		outputDirectory.isPresent()
		outputDirectory.get().name == carthagePlatform

		where:
		platform | carthagePlatform
		tvOS     | CARTHAGE_PLATFORM_TVOS
		macOS    | CARTHAGE_PLATFORM_MACOS
		watchOS  | CARTHAGE_PLATFORM_WATCHOS
		iOS      | CARTHAGE_PLATFORM_IOS
	}


	@Ignore
	def "The xcode selection should be applied if a xcode version is defined"() {
		Map<String, String> environment = null
		given:
		task.xcode = new XcodeFake("12.0.0.ABCD")
		project.xcodebuild.version = 12

		when:
		task.archive()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> environment = arguments[2] }

		environment != null
		environment["DEVELOPER_DIR"] == "/Applications/Xcode-12.app/Contents/Developer"
	}


	@Unroll
	def "has derived data argument"() {
		def commandList

		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		task.archive()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		commandList[5] == ARGUMENT_DERIVED_DATA
	}


	@Unroll
	def "has derived data path set to xcodebuild.derivedData + carthage "() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		project.xcodebuild.derivedDataPath = xcodebuildDerivedDataPath
		task.archive()

		then:
		1 * commandRunner.run(_,
			[CARTHAGE_USR_BIN_PATH,
			 ACTION_BUILD,
			 ARGUMENT_ARCHIVE,
			 ARGUMENT_PLATFORM,
			 CARTHAGE_PLATFORM_IOS,
			 ARGUMENT_DERIVED_DATA,
			 derivedDataPathParameter]
			, _
			, _) >> {
			args -> args[3] instanceof ConsoleOutputAppender
		}

		where:
		xcodebuildDerivedDataPath      | derivedDataPathParameter
		new File("foo")      | new File("foo/carthage").absolutePath
		new File("/foo")     | new File("/foo/carthage").absolutePath
		new File("/bar")     | new File("/bar/carthage").absolutePath
		new File("/foo/bar") | new File("/foo/bar/carthage").absolutePath
	}


	@Ignore
	def "When xcode 12 was set the environment contains XCODE_XCCONFIG_FILE and DEVELOPER_DIR"() {
		Map<String, String> environment = null
		given:
		task.xcode = new XcodeFake("12.0.0")
		project.xcodebuild.version = 12

		when:
		task.archive()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> environment = arguments[2] }

		environment != null
		environment["XCODE_XCCONFIG_FILE"] == xcconfigPath.absolutePath
		environment["DEVELOPER_DIR"] == "/Applications/Xcode-12.app/Contents/Developer"
	}


	def "When xcode 12 then create xcconfig with proper to fix IRGen Expression"() {
		given:
		task.xcode = new XcodeFake("12.0.0.12A7209")

		when:
		task.archive()
		def xcConfig = new XCConfig(xcconfigPath)

		then:
		xcConfig.entries["SWIFT_SERIALIZE_DEBUGGING_OPTIONS"] == "NO"
		xcConfig.entries["OTHER_SWIFT_FLAGS"] == '$(inherited) -Xfrontend -no-serialize-debugging-options'
	}



}
