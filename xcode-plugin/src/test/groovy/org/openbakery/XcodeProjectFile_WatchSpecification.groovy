package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.*

class XcodeProjectFile_WatchSpecification extends Specification {


	XcodeProjectFile xcodeProjectFile
	Project project

	def setup() {

		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.buildDir.mkdirs()

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "Example.xcodeproj/project.pbxproj"));

		project.xcodebuild.target = "ExampleWatchkit WatchKit App"
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
	}

	def "bundle name"() {
		given:
		project.xcodebuild.target = "Example WatchKit Extension"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.bundleName.equals("Example WatchKit Extension")
	}

	def "bundle name WatchkitApp"() {
		given:
		project.xcodebuild.target = "Example WatchKit App"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.bundleName.equals("Example WatchKit App")
	}


	def "Bundle Name WatchkitApp ProductType"() {
		given:
		project.xcodebuild.target = "Example WatchKit App"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productType.equals("app")
	}

	def "BundleName WatchkitApp Extension"() {
		given:
		project.xcodebuild.target = "Example WatchKit Extension"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productType.equals("appex")
	}



	def "all targets"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings.size() == 6
	}

	def "target names"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		expect projectSettings, hasKey("Example")
		expect projectSettings, hasKey("ExampleTests")
		expect projectSettings, hasKey("Example WatchKit App")
		expect projectSettings, hasKey("Example WatchKit Extension")
	}


	def "first target"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings["Example"] instanceof BuildTargetConfiguration
		projectSettings["Example"].buildSettings["Debug"].infoplist == "Example/Example-Info.plist"
		projectSettings["Example"].buildSettings["Debug"].bundleIdentifier == "org.openbakery.gxp.Example"
		projectSettings["Example"].buildSettings["Debug"].productName == "Example"
		projectSettings["Example"].buildSettings["Release"].infoplist == "Example/Example-Info.plist"
		projectSettings["Example"].buildSettings["Release"].bundleIdentifier == "org.openbakery.gxp.Example"
		projectSettings["Example"].buildSettings["Release"].productName == "Example"
		projectSettings["Example"].buildSettings["Release"].sdkRoot == "iphoneos"

	}


	def "ExampleWatchkit WatchKit App build configuration"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings["Example WatchKit App"] instanceof BuildTargetConfiguration
		projectSettings["Example WatchKit App"].buildSettings["Debug"].infoplist == "Example WatchKit App/Info.plist"
		projectSettings["Example WatchKit App"].buildSettings["Debug"].bundleIdentifier == "org.openbakery.gxp.Example.watchkitapp"
		projectSettings["Example WatchKit App"].buildSettings["Debug"].productName == "Example WatchKit App"
		projectSettings["Example WatchKit App"].buildSettings["Release"].infoplist == "Example WatchKit App/Info.plist"
		projectSettings["Example WatchKit App"].buildSettings["Release"].bundleIdentifier == "org.openbakery.gxp.Example.watchkitapp"
		projectSettings["Example WatchKit App"].buildSettings["Release"].productName == "Example WatchKit App"
		projectSettings["Example WatchKit App"].buildSettings["Release"].sdkRoot == "watchos"

	}

}
