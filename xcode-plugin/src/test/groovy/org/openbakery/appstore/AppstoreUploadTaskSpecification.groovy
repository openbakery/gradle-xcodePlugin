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


		project.appstore.username = "user"
		project.appstore.password = "pass"
		project.appstore.publicId = "1"
		project.appstore.appleId = "1"
		project.appstore.bundleVersion = "1"
		project.appstore.shortBundleVersion = "1"
		project.appstore.bundleIdentifier = "org.openbakery.example.App"

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
		String[] commandList

		project.appstore.username = "user"
		project.appstore.password = "pass"
		project.appstore.publicId = "23"
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList.join(" ").contains("--type ios")
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


	def "when xcode 13 and publicId is missing an exception is thrown"() {
		given:
		project.appstore.publicId = null
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		def exception = thrown(IllegalArgumentException.class)
		exception.message.contains("appstore.publicId")
	}

	def "when xcode 13 and appleId is missing an exception is thrown"() {
		given:
		project.appstore.appleId = null
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		def exception = thrown(IllegalArgumentException.class)
		exception.message.contains("appstore.appleId")
	}

	def "when xcode 13 and bundleVersion is missing an exception is thrown"() {
		given:
		project.appstore.bundleVersion = null

		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		def exception = thrown(IllegalArgumentException.class)
		exception.message.contains("appstore.bundleVersion")
	}


	def "when xcode 13 and shortBundleVersion is missing an exception is thrown"() {
		given:
		project.appstore.shortBundleVersion = null
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		def exception = thrown(IllegalArgumentException.class)
		exception.message.contains("appstore.shortBundleVersion")
	}


	def "when xcode 13 and Bundle Identifier is missing an exception is thrown"() {
		given:
		project.appstore.bundleIdentifier = null
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		def exception = thrown(IllegalArgumentException.class)
		exception.message.contains("appstore.bundleIdentifier")
	}


	def "When Xcode 13 use the new upload-package parameter to upload the IPA"() {
		given:
		String[] commandList
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[0]
		}

		commandList.contains("/Applications/Xcode.app/Contents/Developer/usr/bin/altool")
		commandList.contains("--upload-package")
	}


	def "When Xcode 13 use the new upload-package followed by the IPA"() {
		given:
		String[] commandList
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[0]
		}

		commandList.join(" ").contains("--upload-package " + ipaBundle.absolutePath)
		!commandList.contains("--file")
	}

	def "When Xcode 13 use the new upload-package with --asc-public-id parameter"() {
		given:
		String[] commandList
		project.appstore.publicId = "4567"
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList.join(" ").contains("--asc-public-id 4567")
	}


	def "When Xcode 13 use the new upload-package with --apple-id parameter"() {
		given:
		String[] commandList
		project.appstore.appleId = "myId"
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList.join(" ").contains("--apple-id myId")
	}

	def "When Xcode 13 use the new upload-package with --bundle-version parameter"() {
		given:
		String[] commandList
		project.appstore.bundleVersion = "1.2.3"
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList.join(" ").contains("--bundle-version 1.2.3")
	}

	def "When Xcode 13 use the new upload-package with --short-bundle-version parameter"() {
		given:
		String[] commandList
		project.appstore.shortBundleVersion = "1.2.3.4"
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList.join(" ").contains("--bundle-short-version-string 1.2.3.4")
	}

	def "When Xcode 13 use the new upload-package with --bundle-id parameter"() {
		given:
		String[] commandList
		task.xcode = new XcodeFake("13")

		when:
		task.upload()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList.join(" ").contains("--bundle-id org.openbakery.example.App")
	}



}
