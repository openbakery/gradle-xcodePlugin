package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.internal.XcodeBuildSpec
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 17.02.15.
 */
class XcodeProjectFileTest {


	XcodeProjectFile xcodeProjectFile
	Project project
	XcodeBuildSpec buildSpec

	@BeforeMethod
	void setUp() {

		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.buildDir.mkdirs()

		project.apply plugin: org.openbakery.XcodePlugin

		File projectFile = new File(projectDir, "Example.xcodeproj/project.pbxproj")
		//xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "Example.xcodeproj/project.pbxproj"));

		buildSpec = new XcodeBuildSpec(project)

		xcodeProjectFile = new XcodeProjectFile(projectFile, project.buildDir, buildSpec);

	}


	@Test
	void testBundleName() {

		buildSpec.target = "Example"

		xcodeProjectFile.parse()

		assert buildSpec.bundleName.equals("Example")
	}


	@Test
	void testBundleNameWidget() {
		buildSpec.target = "ExampleTodayWidget"

		xcodeProjectFile.parse()

		assert buildSpec.bundleName.equals("ExampleTodayWidget")
	}


	@Test
	void testProductName() {
		buildSpec.target = "Example"
		xcodeProjectFile.parse()
		assert buildSpec.productName.equals("Example")
	}

	@Test
	void testProductNameOfWidget() {
		buildSpec.target = "ExampleTodayWidget"
		xcodeProjectFile.parse()
		assert buildSpec.productName.equals("ExampleTodayWidget")
	}

	@Test
	void testProductType() {
		buildSpec.target = "ExampleTodayWidget"
		xcodeProjectFile.parse()
		assert buildSpec.productType.equals("appex")
	}


	@Test
	void testProductNameFromConfig() {
		buildSpec.productName = 'MyFancyProductName'
		buildSpec.target = "Example"
		xcodeProjectFile.parse()
		assert buildSpec.productName.equals("MyFancyProductName")
	}

}
