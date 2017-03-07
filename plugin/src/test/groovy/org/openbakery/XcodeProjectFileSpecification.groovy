package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test
import spock.lang.Specification

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
		given:
		File  projectDir = new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "ExampleWatchkit.xcodeproj/project.pbxproj"));

		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings["ExampleWatchkit"] instanceof BuildTargetConfiguration
		projectSettings["ExampleWatchkit"].buildSettings["Debug"].infoplist == "ExampleWatchkit/Info.plist"
		projectSettings["ExampleWatchkit"].buildSettings["Debug"].entitlements == "ExampleWatchkit/ExampleWatchkit.entitlements"
	}



	def "update target build settings TARGET_NAME"() {
		given:
		XMLPropertyListConfiguration config = Mock()
		xcodeProjectFile.config = config

		1* config.getString("objects.AAAAAA.buildConfigurationList") >> "item"
		1* config.getList("objects.item.buildConfigurations") >> ["ListItem"]
		1* config.getString("objects.ListItem.name") >> "config"
		1* config.getString("objects.ListItem.buildSettings.PRODUCT_NAME") >> '$(TARGET_NAME)'

		BuildConfiguration buildSettings = new BuildConfiguration()
		//buildSettings.productName = '$(TARGET_NAME)-Test'

		when:
		xcodeProjectFile.updateBuildSettings(buildSettings, "config", "AAAAAA", "MyTarget")

		then:
		buildSettings.productName == "MyTarget"
	}


	def "update target build settings TARGET_NAME complex"() {
		given:
		XMLPropertyListConfiguration config = Mock()
		xcodeProjectFile.config = config

		1* config.getString("objects.AAAAAA.buildConfigurationList") >> "item"
		1* config.getList("objects.item.buildConfigurations") >> ["ListItem"]
		1* config.getString("objects.ListItem.name") >> "config"
		1* config.getString("objects.ListItem.buildSettings.PRODUCT_NAME") >> '$(TARGET_NAME)-Test'

		BuildConfiguration buildSettings = new BuildConfiguration()

		when:
		xcodeProjectFile.updateBuildSettings(buildSettings, "config", "AAAAAA", "MyTarget")

		then:
		buildSettings.productName == "MyTarget-Test"
	}

}
