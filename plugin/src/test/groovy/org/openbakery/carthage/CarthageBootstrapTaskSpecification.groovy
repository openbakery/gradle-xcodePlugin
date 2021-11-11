package org.openbakery.carthage

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.XCConfig
import org.openbakery.xcode.Xcode
import spock.lang.Specification
import spock.lang.Unroll

import static org.openbakery.carthage.AbstractCarthageTaskBase.*
import static org.openbakery.xcode.Type.*

class CarthageBootstrapTaskSpecification extends Specification {

	CarthageBootstrapTask subject
	CommandRunner commandRunner = Mock(CommandRunner)
	File projectDir
	File carthageFile
	Project project


	void setup() {
		projectDir = File.createTempDir()

		createCarthageFile()


		project = ProjectBuilder.builder()
				.withProjectDir(projectDir)
				.build()

		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin


		subject = project.getTasks().getByPath('carthageBootstrap')
		assert subject != null

		subject.commandRunner = commandRunner
	}


	void createCarthageFile(String name = "Cartfile") {
		carthageFile = new File(projectDir, name)
		carthageFile << 'github "Alamofire/Alamofire"'
	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}

	def "The carthage bootstrap task should be present"() {
		expect:
		subject instanceof CarthageBootstrapTask
	}

	def "carthage task is executed when cartfile exists"() {
		expect:
		subject.getOnlyIf().isSatisfiedBy(subject)
	}


	def "carthage task is executed when only Cartfile.private exists"() {
		carthageFile.delete()

		when:
		createCarthageFile("Cartfile.private")

		then:
		subject.getOnlyIf().isSatisfiedBy(subject)
	}

	def "carthage task is skipped when cartfile is missing"() {
		when:
		carthageFile.delete()

		then:
		!subject.getOnlyIf().isSatisfiedBy(subject)
	}


	@Unroll
	def "When bootstrap is executed should only update the platform: #platform"() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"
		project.xcodebuild.type = platform

		when:
		subject.bootstrap()

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
		subject.bootstrap()

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

	def "The subject output directory should be platform dependant"() {
		when:
		subject.xcode.getXcodeSelectEnvironmentValue(_) >> new HashMap<String, String>()
		project.xcodebuild.type = platform

		then:
		Provider<File> outputDirectory = subject.outputDirectory
		outputDirectory.isPresent()
		outputDirectory.get().name == carthagePlatform

		where:
		platform | carthagePlatform
		tvOS     | CARTHAGE_PLATFORM_TVOS
		macOS    | CARTHAGE_PLATFORM_MACOS
		watchOS  | CARTHAGE_PLATFORM_WATCHOS
		iOS      | CARTHAGE_PLATFORM_IOS
	}


	def "The xcode selection should be applied if a xcode version is defined"() {
		Map<String, String> environment = null
		given:
		def xcode = new XcodeFake("12.0.0.ABCD")
		def xcodePath = new File(projectDir, "/Applications/Xcode-12.app")
		xcodePath.mkdirs()
		xcode.selectXcode(new File(xcodePath, Xcode.XCODE_CONTENT_XCODE_BUILD))
		subject.xcode = xcode
		project.xcodebuild.version = 12

		when:
		subject.bootstrap()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> environment = arguments[2] }

		environment != null
		environment["DEVELOPER_DIR"] == "/Applications/Xcode-12.app/Contents/Developer"
	}


	private List<String> getCommandRunnerArgsForPlatform(String carthagePlatform) {
		return [CARTHAGE_USR_BIN_PATH,
						ACTION_BOOTSTRAP,
						ARGUMENT_PLATFORM,
						carthagePlatform,
						ARGUMENT_CACHE_BUILDS,
						ARGUMENT_DERIVED_DATA,
						new File(project.xcodebuild.derivedDataPath, "carthage").absolutePath]
	}


	@Unroll
	def "has derived data argument"() {
		def commandList

		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		subject.bootstrap()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		commandList[5] == ARGUMENT_DERIVED_DATA
		commandList[5] == "--derived-data"
	}

	@Unroll
	def "has derived data path set to xcodebuild.derivedData + carthage "() {
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		project.xcodebuild.derivedDataPath = xcodebuildDerivedDataPath
		subject.bootstrap()

		then:
		1 * commandRunner.run(_,
				[CARTHAGE_USR_BIN_PATH,
				 ACTION_BOOTSTRAP,
				 ARGUMENT_PLATFORM,
				 CARTHAGE_PLATFORM_IOS,
				 ARGUMENT_CACHE_BUILDS,
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


	def "build without cache"() {
		def commandList
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		project.carthage.cache = false
		subject.bootstrap()


		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		!commandList.contains(ARGUMENT_CACHE_BUILDS)
	}


	def "When xcode 12 then create xcconfig without SWIFT_SERIALIZE_DEBUGGING_OPTIONS"() {
		given:
		File carthageDirectory = project.rootProject.file("Carthage")
		File xcconfigPath = new File(carthageDirectory, "gradle-xc12-carthage.xcconfig")
		subject.xcode = new XcodeFake("12.0.0.12A7209")

		when:
		subject.bootstrap()
		def xcConfig = new XCConfig(xcconfigPath)

		then:
		xcConfig.entries["SWIFT_SERIALIZE_DEBUGGING_OPTIONS"] == null
		xcConfig.entries["OTHER_SWIFT_FLAGS"] == null
	}


	def "When xcode 12 builds xcframeworks then NO xcconfig with SWIFT_SERIALIZE_DEBUGGING_OPTIONS is created"() {
		given:
		File carthageDirectory = project.rootProject.file("Carthage")
		File xcconfigPath = new File(carthageDirectory, "gradle-xc12-carthage.xcconfig")
		subject.xcode = new XcodeFake("12.0.0.12A7209")
		subject.xcframework = true

		when:
		subject.bootstrap()

		then:
		xcconfigPath.exists() == false
	}



	def "ARGUMENT_XCFRAMEWORK_BUILDS is --use-xcframeworks"() {
		expect:
		ARGUMENT_XCFRAMEWORK_BUILD == "--use-xcframeworks"
	}


	def "cache parameter is merged"() {
		given:
		project.carthage.cache = false

		when:
		subject.bootstrap()

		then:
		subject.parameters.cache == false
	}

	def "xcframework parameter is merged"() {
		given:
		project.carthage.xcframework = true

		when:
		subject.bootstrap()

		then:
		subject.parameters.xcframework == true
	}

	def "build with global xcframework parameter"() {
		def commandList
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		project.carthage.xcframework = true
		subject.bootstrap()


		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		commandList.contains(ARGUMENT_XCFRAMEWORK_BUILD)
		commandList.contains("--use-xcframeworks")
	}

	def "build with task xcframework parameter"() {
		def commandList
		given:
		commandRunner.runWithResult("which", "carthage") >> "/usr/local/bin/carthage"

		when:
		subject.xcframework = true
		subject.bootstrap()


		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		commandList.contains(ARGUMENT_XCFRAMEWORK_BUILD)
		commandList.contains("--use-xcframeworks")
	}

}
