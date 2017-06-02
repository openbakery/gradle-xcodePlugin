package org.openbakery.appstore

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.testdouble.XcodeFake
import spock.lang.Specification


class AppstoreValidateTaskSpecification extends Specification {

	Project project
	AppstoreValidateTask task
	File infoPlist

	CommandRunner commandRunner = Mock(CommandRunner)
	File ipaBundle;

	def setup() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.getTasks().getByPath(XcodePlugin.APPSTORE_VALIDATE_TASK_NAME)

		task.commandRunner = commandRunner
		task.xcode = new XcodeFake()
		ipaBundle = new File(project.buildDir, "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "ipa missing"() {
		given:
		FileUtils.deleteDirectory(project.projectDir)

		when:
		task.validate()

		then:
		thrown(IllegalStateException)
	}


	def "test validate"() {
		given:
		project.appstore.username = "me@example.com"
		project.appstore.password = "1234"

		def command = "/Applications/Xcode.app/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool"

		when:
		task.validate()

		then:
		1 * commandRunner.run([command, "--validate-app", "--username", "me@example.com", "--password", "1234", "--file", ipaBundle.absolutePath], _)
	}


	def "password missing"() {
		given:
		project.appstore.username = "me@example.com"

		when:
		task.validate()

		then:
		thrown(IllegalArgumentException.class)
	}

	def "username missing"() {
		when:
		task.validate()

		then:
		thrown(IllegalArgumentException.class)
	}
}
