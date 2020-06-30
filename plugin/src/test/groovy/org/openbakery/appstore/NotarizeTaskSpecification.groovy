package org.openbakery.appstore

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.test.ApplicationDummyMacOS
import org.openbakery.testdouble.XcodeFake

class NotarizeTaskSpecification extends spock.lang.Specification {


	Project project
	NotarizeTask task
	File infoPlist

	CommandRunner commandRunner = Mock(CommandRunner)
	File zipBundle
	ApplicationDummyMacOS applicationDummy

	def setup() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		task = (NotarizeTask)project.tasks.findByName('notarize')
		if (task != null) {
			task.commandRunner = commandRunner
		}

		zipBundle = new File(project.getBuildDir(), "package/Test.zip")
		FileUtils.writeStringToFile(zipBundle, "dummy")


		File buildDirectory = new File(project.getBuildDir(), "package")

		applicationDummy = new ApplicationDummyMacOS(buildDirectory)
		applicationDummy.create()

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "notarize task is present"() {
		expect:
		task != null
	}


	def "zip is missing throws exception"() {
		given:
		FileUtils.deleteDirectory(project.projectDir)

		when:
		task.notarize()

		then:
		thrown(IllegalStateException.class)
	}


	def "when username is missing throw exception"() {
		when:
		task.notarize()

		then:
		def ex = thrown(IllegalArgumentException.class)
		ex.message == "Appstore username is missing. Parameter: appstore.username"
	}


	def "when password missing  throw exception"() {
		given:
		project.appstore.username = "me@example.com"

		when:
		task.notarize()

		then:
		def ex = thrown(IllegalArgumentException.class)
		ex.message == "Appstore password is missing. Parameter: appstore.password"
	}


	def "when ascProvider missing throw exception"() {
		given:
		project.appstore.username = "me@example.com"
		project.appstore.password = "secret"

		when:
		task.notarize()

		then:
		def ex = thrown(IllegalArgumentException.class)
		ex.message == "asc-provider is missing. Parameter: appstore.ascProvider"
	}



	def "test notarize command"() {
		given:
		project.appstore.username = "me@example.com"
		project.appstore.password = "1234"
		project.appstore.ascProvider = "ME1234"

		def command = "/Applications/Xcode.app/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool"

		task.xcode = new XcodeFake()

		when:
		task.notarize()

		then:
		1 * commandRunner.run([
			command,
			"--notarize-app",
			"--primary-bundle-id", "org.openbakery.macOS.Example",
			"--asc-provider", "ME1234",
			"--username", "me@example.com",
			"--password", "1234",
			"--file", zipBundle.absolutePath], _)
	}

}
