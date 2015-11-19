package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test
import spock.lang.Specification

/**
 * Created by rene on 17.02.15.
 */
class XcodeProjectFileSpecification extends Specification {


	XcodeProjectFile xcodeProjectFile
	Project project

	def setup() {

		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.buildDir.mkdirs()

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "Example.xcodeproj/project.pbxproj"));

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
	}

	def "parse BundleName"() {
		given:
		project.xcodebuild.target = "Example"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.bundleName == "Example"
	}


	def "parse BundleNameWidget"() {
		given:
		project.xcodebuild.target = "ExampleTodayWidget"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.bundleName == "ExampleTodayWidget"
	}


	def "parse ProductName"() {
		given:
		project.xcodebuild.target = "Example"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productName == "Example"
	}

	def "parse ProductName of widget"() {
		given:
		project.xcodebuild.target = "ExampleTodayWidget"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productName == "ExampleTodayWidget"
	}

	def "parse ProductType"() {
		given:
		project.xcodebuild.target = "ExampleTodayWidget"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productType == "appex"
	}

	def "ProductName from config"() {
		given:
		project.xcodebuild.productName = 'MyFancyProductName'
		project.xcodebuild.target = "Example"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productName == "MyFancyProductName"
	}

	def "all targets"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings.size() == 5
	}

	def "parse entitlements file name"() {
		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings["Example"] instanceof BuildTargetConfiguration
		projectSettings["Example"].buildSettings["Debug"].infoplist == "Example/Example-Info.plist"
		projectSettings["Example"].buildSettings["Debug"].entitlements == "Example/Example.entitlements"


	}


}
