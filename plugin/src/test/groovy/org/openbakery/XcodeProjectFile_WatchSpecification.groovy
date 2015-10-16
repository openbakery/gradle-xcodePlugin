package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.*

/**
 * Created by rene on 08.10.15.
 */
class XcodeProjectFile_WatchSpecification extends Specification {


	XcodeProjectFile xcodeProjectFile
	Project project

	def setup() {

		File projectDir = new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.buildDir.mkdirs()

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "ExampleWatchkit.xcodeproj/project.pbxproj"));

		project.xcodebuild.target = "ExampleWatchkit WatchKit App"
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
	}

	def "bundle name"() {
		given:
		project.xcodebuild.target = "ExampleWatchkit"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.bundleName.equals("ExampleWatchkit")
	}

	def "bundle name WatchkitApp"() {
		given:
		project.xcodebuild.target = "ExampleWatchkit WatchKit App"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.bundleName.equals("ExampleWatchkit WatchKit App")
	}


	def "Bundle Name WatchkitApp ProductType"() {
		given:
		project.xcodebuild.target = "ExampleWatchkit WatchKit App"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productType.equals("app")
	}

	def "BundleName WatchkitApp Extension"() {
		given:
		project.xcodebuild.target = "ExampleWatchkit WatchKit Extension"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productType.equals("appex")
	}


	def "BundleName Watchkit App Devices"() {
		given:
		project.xcodebuild.target = "ExampleWatchkit WatchKit App"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.devices == Devices.WATCH
	}


	def "all targets"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings.size() == 4
	}

	def "target names"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		expect projectSettings, hasKey("ExampleWatchkit")
		expect projectSettings, hasKey("ExampleWatchkitTests")
		expect projectSettings, hasKey("ExampleWatchkit WatchKit App")
		expect projectSettings, hasKey("ExampleWatchkit WatchKit Extension")
	}


	def "first target"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings["ExampleWatchkit"] instanceof BuildConfiguration
		projectSettings["ExampleWatchkit"].debug.infoplist == "ExampleWatchkit/Info.plist"
		projectSettings["ExampleWatchkit"].debug.bundleIdentifier == "org.openbakery.Example"
		projectSettings["ExampleWatchkit"].debug.productName == "ExampleWatchkit"
		projectSettings["ExampleWatchkit"].release.infoplist == "ExampleWatchkit/Info.plist"
		projectSettings["ExampleWatchkit"].release.bundleIdentifier == "org.openbakery.Example"
		projectSettings["ExampleWatchkit"].release.productName == "ExampleWatchkit"
		projectSettings["ExampleWatchkit"].release.sdkRoot == "iphoneos"
		projectSettings["ExampleWatchkit"].release.devices == Devices.UNIVERSAL

	}


	def "ExampleWatchkit WatchKit App build configuration"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings["ExampleWatchkit WatchKit App"] instanceof BuildConfiguration
		projectSettings["ExampleWatchkit WatchKit App"].debug.infoplist == "ExampleWatchkit WatchKit App/Info.plist"
		projectSettings["ExampleWatchkit WatchKit App"].debug.bundleIdentifier == "org.openbakery.Example.watchkitapp"
		projectSettings["ExampleWatchkit WatchKit App"].debug.productName == "ExampleWatchkit WatchKit App"
		projectSettings["ExampleWatchkit WatchKit App"].release.infoplist == "ExampleWatchkit WatchKit App/Info.plist"
		projectSettings["ExampleWatchkit WatchKit App"].release.bundleIdentifier == "org.openbakery.Example.watchkitapp"
		projectSettings["ExampleWatchkit WatchKit App"].release.productName == "ExampleWatchkit WatchKit App"
		projectSettings["ExampleWatchkit WatchKit App"].release.sdkRoot == "watchos"
		projectSettings["ExampleWatchkit WatchKit App"].release.devices == Devices.WATCH

	}

}
