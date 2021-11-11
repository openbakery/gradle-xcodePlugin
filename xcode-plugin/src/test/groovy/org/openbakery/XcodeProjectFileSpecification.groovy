package org.openbakery

import org.apache.commons.configuration2.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
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

	def "settings has target"() {
		when:
		BuildConfiguration configuration = xcodeProjectFile.getBuildConfiguration("Example", "Debug")

		then:
		configuration.target == "Example"
	}

	def "get productName for target"() {
		when:
		BuildConfiguration configuration = xcodeProjectFile.getBuildConfiguration("Example", "Debug")

		then:
		configuration.target == "Example"
		configuration.productName == 'Example'
	}

	def "parse BundleNameWidget"() {
		given:
		project.xcodebuild.target = "ExampleTodayWidget"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.bundleName == "ExampleTodayWidget"
	}

	def "get productName for widget target"() {
		when:
		BuildConfiguration configuration = xcodeProjectFile.getBuildConfiguration("ExampleTodayWidget", "Debug")

		then:
		configuration.target == "ExampleTodayWidget"
		configuration.productName == "ExampleTodayWidget"
	}



	def "parse ProductName"() {
		given:
		project.xcodebuild.target = "Example"

		when:
		xcodeProjectFile.parse()

		then:
		project.xcodebuild.productName == "Example"
	}

	def "parse ProductType for app"() {
		when:
		BuildConfiguration configuration = xcodeProjectFile.getBuildConfiguration("Example", "Debug")

		then:
		configuration.productType == "app"
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


	def "parse ProductType for widget"() {
		when:
		BuildConfiguration configuration = xcodeProjectFile.getBuildConfiguration("ExampleTodayWidget", "Debug")

		then:
		configuration.productType == "appex"
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
		projectSettings.size() == 6
	}

	def "parse entitlements file name"() {
		given:
		File  projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "Example.xcodeproj/project.pbxproj"));

		when:
		def projectSettings = xcodeProjectFile.getProjectSettings()

		then:
		projectSettings["Example"] instanceof BuildTargetConfiguration
		projectSettings["Example"].buildSettings["Debug"].infoplist == "Example/Example-Info.plist"
		projectSettings["Example"].buildSettings["Debug"].entitlements == "Example/Example.entitlements"
	}



	def "update target build settings TARGET_NAME"() {
		given:
		XMLPropertyListConfiguration config = Mock()
		xcodeProjectFile.config = config

		1* config.getString("objects.AAAAAA.buildConfigurationList") >> "item"
		1* config.getList("objects.item.buildConfigurations") >> ["ListItem"]
		1* config.getString("objects.ListItem.name") >> "config"
		1* config.getString("objects.ListItem.buildSettings.PRODUCT_NAME") >> '$(TARGET_NAME)'

		BuildConfiguration buildSettings = new BuildConfiguration("MyTarget")
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

		BuildConfiguration buildSettings = new BuildConfiguration("MyTarget")

		when:
		xcodeProjectFile.updateBuildSettings(buildSettings, "config", "AAAAAA", "MyTarget")

		then:
		buildSettings.productName == "MyTarget-Test"
	}


	def "entitlements for target"() {
		when:
		BuildConfiguration configuration = xcodeProjectFile.getBuildConfiguration("Example", "Debug")

		then:
		configuration.entitlements == "Example/Example.entitlements"
	}


}
