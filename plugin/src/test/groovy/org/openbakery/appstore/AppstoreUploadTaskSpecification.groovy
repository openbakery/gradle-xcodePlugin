package org.openbakery.appstore

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import spock.lang.Specification


class AppstoreUploadTaskSpecification extends Specification {

	Project project
	AppstoreUploadTask task
	File infoPlist

	CommandRunner commandRunner = Mock(CommandRunner)
	File ipaBundle;

	def setup() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName('appstoreUpload')
		task.commandRunner = commandRunner

		ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "ipa missing"() {
		given:
		FileUtils.deleteDirectory(project.projectDir)

		when:
		task.upload()

		then:
		thrown(IllegalStateException.class)

	}

	def "task as xcode"() {
		expect:
		task.xcode != null
	}

	def "test upload with api key and issuer"() {
		given:
		project.appstore.apiKey = "key"
		project.appstore.apiIssuer = "1234"

		task.xcode = new XcodeFake()

		when:
		task.upload()

		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool",
													 "--upload-app",
													 "--apiKey",
													 "key",
													 "--apiIssuer",
													 "1234",
													 "--file",
													 ipaBundle.absolutePath], _)

	}

	def "test upload with username and password"() {
		given:
		project.appstore.username = "user"
		project.appstore.password = "pass"

		task.xcode = new XcodeFake()

		when:
		task.upload()

		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool",
													 "--upload-app",
													 "--username",
													 "user",
													 "--password",
													 "pass",
													 "--file",
													 ipaBundle.absolutePath], _)

	}


	def "test when Xcode 13 then add type parameter is added for IPA upload"() {
		given:
		project.appstore.username = "user"
		project.appstore.password = "pass"
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Developer/usr/bin/altool",
													 "--upload-app",
													 "--type",
													 "ios",
													 "--username",
													 "user",
													 "--password",
													 "pass",
													 "--file",
													 ipaBundle.absolutePath], _)
	}

	def "test when Xcode 12 then add type parameter is not added for IPA upload"() {
		given:
		project.appstore.username = "user"
		project.appstore.password = "pass"
		task.xcode = new XcodeFake("12")

		when:
		task.upload()

		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Developer/usr/bin/altool",
													 "--upload-app",
													 "--username",
													 "user",
													 "--password",
													 "pass",
													 "--file",
													 ipaBundle.absolutePath], _)
	}


}
