package org.openbakery.rome

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.XcodePlugin
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import spock.lang.Specification
import org.openbakery.rome.RomeUploadTask
import spock.lang.Unroll

import static org.openbakery.carthage.AbstractCarthageTaskBase.getCARTHAGE_PLATFORM_IOS
import static org.openbakery.carthage.AbstractCarthageTaskBase.getCARTHAGE_PLATFORM_MACOS
import static org.openbakery.carthage.AbstractCarthageTaskBase.getCARTHAGE_PLATFORM_TVOS
import static org.openbakery.carthage.AbstractCarthageTaskBase.getCARTHAGE_PLATFORM_WATCHOS
import static org.openbakery.xcode.Type.iOS
import static org.openbakery.xcode.Type.macOS
import static org.openbakery.xcode.Type.tvOS
import static org.openbakery.xcode.Type.watchOS

class RomeUploadTaskSpecification extends Specification {

	RomeUploadTask subject
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode mockXcode = Mock(Xcode)
	File projectDir
	File romeCacheDirectory
	File romefile
	Project project

	@Rule
	public ExpectedException exception = ExpectedException.none()

	void setup() {
		projectDir = File.createTempDir()
		romeCacheDirectory = File.createTempDir()

		romefile = new File(projectDir, "Romefile")
		romefile << 'cache:\n  local: ' << romeCacheDirectory

		project = ProjectBuilder.builder()
				.withProjectDir(projectDir)
				.build()

		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		subject = project.getTasks().getByPath('romeUpload')
		assert subject != null

		subject.commandRunner = commandRunner
	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
		FileUtils.deleteDirectory(romeCacheDirectory)
	}

	def mockRomeCommand() {
		commandRunner.runWithResult("which", "rome") >> "/usr/local/bin/rome"
	}


	def mockRomeListCommand(String result) {
		List<String> commands = ["/usr/local/bin/rome", "list", "--platform", "iOS"]
		commandRunner.runWithResult(projectDir.canonicalPath , commands) >> result
	}


	def "The rome upload task should be present"() {
		expect:
		subject instanceof RomeUploadTask
	}

	def "rome upload task is executed when Romefile exists"() {
		given:
		mockRomeCommand()

		expect:
		subject.getOnlyIf().isSatisfiedBy(subject)
	}

	def "rome upload task is skipped when Romefile is missing"() {
		when:
		romefile.delete()

		then:
		!subject.getOnlyIf().isSatisfiedBy(subject)
	}

	def "rome upload task is executed if rome is installed"() {
		given:
		mockRomeCommand()

		expect:
		subject.getOnlyIf().isSatisfiedBy(subject)
	}

	def "rome upload task is skipped  if rome is not installed"() {
		given:
		commandRunner.runWithResult("which", "rome") >>  { throw new CommandRunnerException("Command not found") }

		expect:
		!subject.getOnlyIf().isSatisfiedBy(subject)
	}


	def "command runner has console appender"() {
		def appender
		given:
		mockRomeCommand()
		mockRomeListCommand("FirstFramework 1234123412341234123412341234123412341234 : -iOS")
		project.xcodebuild.type = Type.iOS

		when:
		subject.upload()

		then:
		1 * commandRunner.run(_, _, _) >> {
			args -> appender = args[2]
		}
		appender instanceof ConsoleOutputAppender
	}



	def "rome upload has carthage bootstrap dependency"() {
		when:

		def dependsOn = subject.getDependsOn()

		then:
		dependsOn.contains(XcodePlugin.CARTHAGE_BOOTSTRAP_TASK_NAME)
	}

	def "xcodebuild has rome upload dependency"() {
		when:

		def xcodeBuildTask = project.getTasks().getByPath(XcodePlugin.XCODE_BUILD_TASK_NAME)
		def dependsOn = xcodeBuildTask.getDependsOn()

		then:
		dependsOn.contains(XcodePlugin.ROME_UPLOAD_TASK_NAME)
	}


	@Unroll
	def "When upload is executed the list of missing uploads is fetched the platform: #platform"() {
		given:
		mockRomeCommand()
		project.xcodebuild.type = platform

		when:
		subject.upload()

		then:
		1 * commandRunner.runWithResult(projectDir.canonicalPath, ["/usr/local/bin/rome", "list", "--platform", romePlatform])

		where:
		platform | romePlatform
		tvOS     | AbstractRomeTask.ROME_PLATFORM_TVOS
		macOS    | AbstractRomeTask.ROME_PLATFORM_MACOS
		watchOS  | AbstractRomeTask.ROME_PLATFORM_WATCHOS
		iOS      | AbstractRomeTask.ROME_PLATFORM_IOS
	}



	def "Upload only missing from list"() {
		given:
		mockRomeCommand()
		mockRomeListCommand("""FirstFramework 1234123412341234123412341234123412341234 : -iOS
SecondFramework 1.0.0 : -iOS""")
		project.xcodebuild.type = Type.iOS

		when:
		subject.upload()

		then:
		1 * commandRunner.run(projectDir.canonicalPath, ["/usr/local/bin/rome", "upload", "--platform", "iOS", "FirstFramework"], _)
		1 * commandRunner.run(projectDir.canonicalPath, ["/usr/local/bin/rome", "upload", "--platform", "iOS", "SecondFramework"], _)

	}

	def "Upload nothing, because list is empty"() {
		given:
		mockRomeCommand()
		mockRomeListCommand("")
		project.xcodebuild.type = Type.iOS

		when:
		subject.upload()

		then:
		0 * commandRunner.run(_, _, _)
		// should not throw a exception

	}


}
