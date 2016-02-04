package org.openbakery.cocoapods

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import spock.lang.Specification

/**
 * Created by rene on 04.02.16.
 */
class CocoapodsUpdateTaskSpecification extends Specification {

	Project project
	CocoapodsUpdateTask cocoapodsTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {

		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin:org.openbakery.XcodePlugin

		cocoapodsTask = project.getTasks().getByPath('cocoapodsUpdate')

		cocoapodsTask.commandRunner = commandRunner

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "update pods"() {
		given:
		commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir") >> "/tmp/gems"

		when:
		cocoapodsTask.update()

		then:
		1 * commandRunner.run(["gem", "install", "-N", "--user-install", "cocoapods"])
		1 * commandRunner.run(["/tmp/gems/bin/pod", "setup"], _)
		1 * commandRunner.run(["/tmp/gems/bin/pod", "update"], _)

	}
}
