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
import org.openbakery.xcode.Xcode
import spock.lang.Specification

class RomeDownloadTaskSpecification extends Specification {

	RomeDownloadTask subject
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode mockXcode = Mock(Xcode)
	File projectDir
	File romeCacheDirectory
	File romefile
	Project project


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

		subject = project.getTasks().getByPath('romeDownload')
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

	def "The rome download task should be present"() {
		expect:
		subject instanceof RomeDownloadTask
	}

	def "rome download task is executed when Romefile exists"() {
		given:
		mockRomeCommand()

		expect:
		subject.getOnlyIf().isSatisfiedBy(subject)
	}

	def "rome download task is skipped when Romefile is missing"() {
		when:
		romefile.delete()

		then:
		!subject.getOnlyIf().isSatisfiedBy(subject)
	}

	def "rome download task is executed if rome is installed"() {
		given:
		mockRomeCommand()

		expect:
		subject.getOnlyIf().isSatisfiedBy(subject)
	}

	def "rome download task is skipped  if rome is not installed"() {
		given:
		commandRunner.runWithResult("which", "rome") >>  { throw new CommandRunnerException("Command not found") }

		expect:
		!subject.getOnlyIf().isSatisfiedBy(subject)
	}


	def "command runner has console appender"() {
		def appender
		given:
		mockRomeCommand()

		when:
		subject.download()

		then:
		1 * commandRunner.run(_, _, _) >> {
			args -> appender = args[2]
		}
		appender instanceof ConsoleOutputAppender
	}


	def "download executes rome download command"() {
		given:
		mockRomeCommand()

		when:
		subject.download()

		then:
		1 * commandRunner.run(_, ["/usr/local/bin/rome", "download"], _)
	}



	def "carthage bootstrap has rome download dependency"() {
		when:

		def xcodeBuildTask = project.getTasks().getByPath(XcodePlugin.CARTHAGE_BOOTSTRAP_TASK_NAME)
		def dependsOn = xcodeBuildTask.getDependsOn()

		then:
		dependsOn.contains(XcodePlugin.ROME_DOWNLOAD_TASK_NAME)
	}

}
