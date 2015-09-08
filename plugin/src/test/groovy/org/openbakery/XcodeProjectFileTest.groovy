package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by rene on 17.02.15.
 */
class XcodeProjectFileTest {


	XcodeProjectFile xcodeProjectFile
	Project project

	@Before
	void setUp() {

		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.buildDir.mkdirs()

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "Example.xcodeproj/project.pbxproj"));

	}

	@After
	void cleanup() {
		//FileUtils.deleteDirectory(project.buildDir)
	}

	@Test
	void testBundleName() {

		project.xcodebuild.target = "Example"

		xcodeProjectFile.parse()

		assert project.xcodebuild.bundleName.equals("Example")
	}


	@Test
	void testBundleNameWidget() {
		project.xcodebuild.target = "ExampleTodayWidget"

		xcodeProjectFile.parse()

		assert project.xcodebuild.bundleName.equals("ExampleTodayWidget")
	}


	@Test
	void testProductName() {
		project.xcodebuild.target = "Example"
		xcodeProjectFile.parse()
		assert project.xcodebuild.productName.equals("Example")
	}

	@Test
	void testProductNameOfWidget() {
		project.xcodebuild.target = "ExampleTodayWidget"
		xcodeProjectFile.parse()
		assert project.xcodebuild.productName.equals("ExampleTodayWidget")
	}

	@Test
	void testProductType() {
		project.xcodebuild.target = "ExampleTodayWidget"
		xcodeProjectFile.parse()
		assert project.xcodebuild.productType.equals("appex")
	}


	@Test
	void testProductNameFromConfig() {
		project.xcodebuild.productName = 'MyFancyProductName'
		project.xcodebuild.target = "Example"
		xcodeProjectFile.parse()
		assert project.xcodebuild.productName.equals("MyFancyProductName")
	}

}
