package org.openbakery.carthage

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import spock.lang.Specification

class AbstractCarthageTaskBaseSpecification extends Specification {


	AbstractCarthageTaskBase task
	CommandRunner commandRunner = Mock(CommandRunner)
	XcodeFake xcodeFake
	File projectDir
	File carthageFile
	Project project
	File xcconfigPath
	StyledTextOutput output

	void setup() {
		projectDir = File.createTempDir()

		createCarthageFile()

		project = ProjectBuilder.builder()
				.withProjectDir(projectDir)
				.build()

		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.getTasks().getByPath('carthageBootstrap')
		assert task != null


		task.commandRunner = commandRunner

		File carthageDirectory = project.rootProject.file("Carthage")
		xcconfigPath = new File(carthageDirectory, "gradle-xc12-carthage.xcconfig")

		output = task.services.get(StyledTextOutputFactory).create(CarthageUpdateTask)

	}

	void createCarthageFile(String name = "Cartfile") {
		carthageFile = new File(projectDir, name)
		carthageFile << 'github "Alamofire/Alamofire"'
	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}

	def "The carthage task should be present"() {
		expect:
		task instanceof AbstractCarthageTaskBase
	}

	def "The has carthage file when only Cartfile is present"() {
		expect:
		task.hasCartfile()
	}

	def "The has carthage file when only Cartfile.private is present"() {
		carthageFile.delete()

		when:
		createCarthageFile("Cartfile.private")

		then:
		task.hasCartfile()
	}

	def "When xcode 11 then xcconfig is not created"() {
		given:
		task.xcode = new XcodeFake("11")

		when:
		task.run("bootstrap", output)

		then:
		!xcconfigPath.exists()
	}


	def "When xcode 12 then create xcconfig"() {
		given:
		task.xcode = new XcodeFake("12.0.0.12A7209")

		when:
		task.run("bootstrap", output)

		then:
		xcconfigPath.exists()
	}


	def "When xcode 12 then create xcconfig with proper content for Xcode 12"() {
		given:
		task.xcode = new XcodeFake("12.0.0.12A7209")

		when:
		task.run("bootstrap", output)

		then:
		def content = FileUtils.readFileToString(xcconfigPath).split("\n")

		content[0] == 'EXCLUDED_ARCHS__EFFECTIVE_PLATFORM_SUFFIX_iphonesimulator__NATIVE_ARCH_64_BIT_x86_64__XCODE_1200 = arm64 arm64e armv7 armv7s armv6 armv8'
		content[1] == 'EXCLUDED_ARCHS__EFFECTIVE_PLATFORM_SUFFIX_appletvsimulator__NATIVE_ARCH_64_BIT_x86_64__XCODE_1200 = arm64 arm64e armv7 armv7s armv6 armv8'
		content[2] == 'EXCLUDED_ARCHS = $(inherited) $(EXCLUDED_ARCHS__EFFECTIVE_PLATFORM_SUFFIX_$(PLATFORM_NAME)__NATIVE_ARCH_64_BIT_$(NATIVE_ARCH_64_BIT)__XCODE_$(XCODE_VERSION_MAJOR))'
	}


	def "When xcode 12.0.1 then create xcconfig with proper build arch value"() {
		given:
		task.xcode = new XcodeFake("12.0.0.ABCD")


		when:
		task.run("bootstrap", output)

		then:
		def content = FileUtils.readFileToString(xcconfigPath).split("\n")

		content[0] == 'EXCLUDED_ARCHS__EFFECTIVE_PLATFORM_SUFFIX_iphonesimulator__NATIVE_ARCH_64_BIT_x86_64__XCODE_1200 = arm64 arm64e armv7 armv7s armv6 armv8'
		content[1] == 'EXCLUDED_ARCHS__EFFECTIVE_PLATFORM_SUFFIX_appletvsimulator__NATIVE_ARCH_64_BIT_x86_64__XCODE_1200 = arm64 arm64e armv7 armv7s armv6 armv8'
		content[2] == 'EXCLUDED_ARCHS = $(inherited) $(EXCLUDED_ARCHS__EFFECTIVE_PLATFORM_SUFFIX_$(PLATFORM_NAME)__NATIVE_ARCH_64_BIT_$(NATIVE_ARCH_64_BIT)__XCODE_$(XCODE_VERSION_MAJOR))'
	}


	def "When xcode 12 then the environment contains xconfig"() {
		Map<String, String> environment = null
		given:
		task.xcode = new XcodeFake("12.0.0.ABCD")

		when:
		task.run("bootstrap", output)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> environment = arguments[2] }

		environment != null
		environment["XCODE_XCCONFIG_FILE"] == xcconfigPath.absolutePath
	}

	def "When xcode 12 was set via xcodeversion then the environment contains XCODE_XCCONFIG_FILE and DEVELOPER_DIR"() {
		Map<String, String> environment = null
		given:
		task.xcode = new XcodeFake("12.0.0.ABCD")
		project.xcodebuild.version = 12

		when:
		task.run("bootstrap", output)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> environment = arguments[2] }

		environment != null
		environment["XCODE_XCCONFIG_FILE"] == xcconfigPath.absolutePath
		environment["DEVELOPER_DIR"] == "/Applications/Xcode-12.app/Contents/Developer"
	}


}
