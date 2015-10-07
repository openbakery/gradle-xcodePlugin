package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 07.10.15.
 */
class XcodeProjectFile_WatchTest {


	XcodeProjectFile xcodeProjectFile
	Project project

	@Before
	void setUp() {

		File projectDir = new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.buildDir.mkdirs()

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "ExampleWatchkit.xcodeproj/project.pbxproj"));

	}

	@After
	void cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
	}

	@Test
	void testBundleName() {

		project.xcodebuild.target = "ExampleWatchkit"

		xcodeProjectFile.parse()

		assertThat(project.xcodebuild.bundleName, is(equalTo("ExampleWatchkit")))
	}


	@Test
	void testBundleNameWatchkitApp() {
		project.xcodebuild.target = "ExampleWatchkit WatchKit App"

		xcodeProjectFile.parse()

		assertThat(project.xcodebuild.bundleName, is(equalTo("ExampleWatchkit WatchKit App")))
	}


	@Test
	void testBundleNameWatchkitApp_ProductType() {
		project.xcodebuild.target = "ExampleWatchkit WatchKit App"
		xcodeProjectFile.parse()
		assertThat(project.xcodebuild.productType, is(equalTo("app")))
	}


	@Test
	void testBundleNameWatchkitApp_Extension() {
		project.xcodebuild.target = "ExampleWatchkit WatchKit Extension"
		xcodeProjectFile.parse()
		assertThat(project.xcodebuild.productType, is(equalTo("appex")))
	}


	@Test
	void testBundleNameWatchkitApp_Devices() {
		project.xcodebuild.target = "ExampleWatchkit WatchKit App"
		xcodeProjectFile.parse()
		assertThat(project.xcodebuild.devices, is(Devices.WATCH))
		assertThat(project.xcodebuild._sdkRoot, is("watchos"))
	}


}
