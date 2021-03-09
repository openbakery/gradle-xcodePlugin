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


	def "test validate using api key"() {
		given:
		project.appstore.apiKey = "key"
		project.appstore.apiIssuer = "issuer"

		def command = "/Applications/Xcode.app/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool"

		when:
		task.validate()

		then:
		1 * commandRunner.run([command, "--validate-app", "--apiKey", "key", "--apiIssuer", "issuer", "--file", ipaBundle.absolutePath], _)
	}


	def "test validate using username/password"() {
		given:
		project.appstore.username = "user"
		project.appstore.password = "secret"

		def command = "/Applications/Xcode.app/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool"

		when:
		task.validate()

		then:
		1 * commandRunner.run([command, "--validate-app", "--username", "user", "--password", "secret", "--file", ipaBundle.absolutePath], _)
	}


	def "apiIssuer missing"() {
		given:
		project.appstore.apiKey = "me@example.com"

		when:
		task.validate()

		then:
		thrown(IllegalArgumentException.class)
	}

	def "apiKey missing"() {
		given:
		project.appstore.apiIssuer = "me@example.com"

		when:
		task.validate()

		then:
		def ex = thrown(IllegalArgumentException.class)
		ex.message == "Appstore apiKey is missing. Parameter: appstore.apiKey"
	}

	def "credentials missing"() {
		when:
		task.validate()

		then:
		def ex = thrown(IllegalArgumentException.class)
		ex.message == "Credentials are missing. Either apiKey/apiIssuer of username/password"
	}

	def "username is missing"() {
		given:
		project.appstore.password = "secret"

		when:
		task.validate()

		then:
		def ex = thrown(IllegalArgumentException.class)
		ex.message == "Appstore username is missing. Parameter: appstore.username"
	}

	def "password is missing"() {
		given:
		project.appstore.username = "user"

		when:
		task.validate()

		then:
		def ex = thrown(IllegalArgumentException.class)
		ex.message == "Appstore password is missing. Parameter: appstore.password"
	}

}
