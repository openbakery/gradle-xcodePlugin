package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.openbakery.stubs.PlistHelperStub
import org.openbakery.util.PlistHelper
import spock.lang.Specification

/**
 * Created by Stefan Gugarel on 04/02/15.
 */
class XcodeBuildArchiveTaskOSXSpecification extends Specification {

	Project project

	XcodeBuildArchiveTask xcodeBuildArchiveTask;

	File projectDir
	File buildOutputDirectory
	File appDirectory

	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.infoPlist = 'Info.plist'
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.OSX
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		xcodeBuildArchiveTask = project.getTasks().getByPath(XcodePlugin.ARCHIVE_TASK_NAME)
		xcodeBuildArchiveTask.commandRunner = commandRunner

		buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration)
		buildOutputDirectory.mkdirs()

		appDirectory = new File(buildOutputDirectory, "Example.app")
		appDirectory.mkdirs()

		File infoPlist = new File("../example/OSX/ExampleOSX/ExampleOSX/Info.plist")
		FileUtils.copyFile(infoPlist, new File(appDirectory, "" + "Contents/Info.plist"))
	}


	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}


	def "copyOSXApp"() {
		when:
		xcodeBuildArchiveTask.archive()

		File appFile = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app")

		then:
		appFile.exists()

	}

	def "reateInfoPlist"() {
		given:

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"

		when:
		xcodeBuildArchiveTask.archive()

		File infoPlist = new File(projectDir, "build/archive/Example.xcarchive/Info.plist")

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(infoPlist)

		then:
		infoPlist.exists()
		config.getString("ApplicationProperties.ApplicationPath") == "Applications/Example.app"
		config.getString("ApplicationProperties.CFBundleIdentifier") == "com.cocoanetics.ExampleOSX"
		config.getString("ApplicationProperties.CFBundleShortVersionString") == "1.0"
		config.getString("ApplicationProperties.CFBundleVersion") == "1"
		config.getString("ApplicationProperties.SigningIdentity") == "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
		config.getString("Name") == "Example"
		config.getString("SchemeName") == "Example"

	}

	def "get Icon Path OSX"() {
		given:
		// Info.plist from Example.app
		File infoPlistInAppFile = new File(projectDir, "/build/sym/Debug/Example.app/Contents/Info.plist")

		// add key CFBundleIconFile
		xcodeBuildArchiveTask.plistHelper.setValueForPlist(infoPlistInAppFile, "CFBundleIconFile", "icon")

		when:
		def macOSXIcons = xcodeBuildArchiveTask.getMacOSXIcons()

		then:
		macOSXIcons.size() == 1
		macOSXIcons.get(0) == "Applications/Example.app/Contents/Resources/icon.icns"
	}

	def "no Icon Mac OSX"() {
		given:
		// Info.plist from Example.app
		File infoPlistInAppFile = new File(projectDir, "/build/sym/Debug/Example.app/Contents/Info.plist")

		when:
		def macOSXIcons = xcodeBuildArchiveTask.getMacOSXIcons()

		then:
		macOSXIcons.size() == 0
	}


	def "do not convert InfoPlist to binary"() {
		given:
		PlistHelperStub plistHelperStub = new PlistHelperStub()
		xcodeBuildArchiveTask.plistHelper = plistHelperStub

		File infoPlist = new File(appDirectory, "Contents/Info.plist")
		plistHelperStub.setValueForPlist(infoPlist, "CFBundleIdentifier", "");
		plistHelperStub.setValueForPlist(infoPlist, "CFBundleShortVersionString", "");
		plistHelperStub.setValueForPlist(infoPlist, "CFBundleVersion", "");
		plistHelperStub.setValueForPlist(infoPlist, "CFBundleIconFile", "");

		File infoPlistToConvert = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Info.plist")


		when:
		xcodeBuildArchiveTask.archive()

		then:
		0 * commandRunner.run(["/usr/bin/plutil", "-convert", "binary1", infoPlistToConvert.absolutePath])

	}


	def "copy plugins"() {
		File xctext = new File(appDirectory, "Contents/PlugIns/Today.appex/Contents/MacOS/Today")
		FileUtils.writeStringToFile(xctext, "dummy")

		when:
		xcodeBuildArchiveTask.archive()

		File appFile = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Contents/PlugIns/Today.appex/Contents/MacOS/Today")

		then:
		appFile.exists()

	}

	def "xctest should not be copied"() {
		File xctest = new File(appDirectory, "Contents/PlugIns/ExampleOSXTests.xctest/foobar")
		FileUtils.writeStringToFile(xctest, "dummy")

		when:
		xcodeBuildArchiveTask.archive()

		File appFile = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Contents/PlugIns/ExampleOSXTests.xctest")

		then:
		!appFile.exists()
	}

}
