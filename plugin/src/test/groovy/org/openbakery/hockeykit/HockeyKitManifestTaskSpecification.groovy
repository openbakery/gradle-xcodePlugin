package org.openbakery.hockeykit

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.XcodeBuildArchiveTask
import spock.lang.Specification

class HockeyKitManifestTaskSpecification extends Specification {


	Project project
	HockeyKitManifestTask hockeyKitManifestTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	PlistHelperStub plistHelper = new PlistHelperStub()

	File infoPlist

	def setup() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'Test'
		project.xcodebuild.infoPlist = 'Info.plist'

		hockeyKitManifestTask = project.getTasks().getByPath('hockeykitManifest')
		hockeyKitManifestTask.plistHelper = plistHelper

		hockeyKitManifestTask.commandRunner = commandRunner

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();
		FileUtils.writeStringToFile(infoPlist, "dummy")

	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "create Manifest"() {
		given:
		project.xcodebuild.bundleNameSuffix = '-b1234'
		project.hockeykit.versionDirectoryName = "1234"

		plistHelper.setValueForPlist(infoPlist, "CFBundleIdentifier", "com.example.Test")
		plistHelper.setValueForPlist(infoPlist, "CFBundleDisplayName", "Test")
		plistHelper.setValueForPlist(infoPlist, "CFBundleVersion", "1.0.0-b1234")
		plistHelper.setValueForPlist(infoPlist, "CFBundleShortVersionString", "1.0.0")

			when:
		hockeyKitManifestTask.createManifest()

		File manifestFile = new File(project.buildDir, "hockeykit/com.example.Test/1234/Test-b1234.plist")
		String xmlContent = FileUtils.readFileToString(manifestFile, "UTF-8")

		then:
		manifestFile.exists()
		xmlContent.contains("com.example.Test")
		xmlContent.contains("1.0.0-b1234")

	}

}
