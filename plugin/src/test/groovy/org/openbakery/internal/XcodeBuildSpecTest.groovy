package org.openbakery.internal

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.Devices
import org.openbakery.XcodePlugin
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 14.08.15.
 */
class XcodeBuildSpecTest {

	XcodeBuildSpec buildSpec
	XcodeBuildSpec parentBuildSpec
	Project project

	@BeforeMethod
	void setup() {
		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin


		parentBuildSpec = new XcodeBuildSpec(project);
		buildSpec = new XcodeBuildSpec(project, parentBuildSpec);

	}

	@AfterMethod
	def cleanup() {
		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		FileUtils.deleteDirectory(projectDir)
	}


	@Test
	void testDefaultValues() {

		assertThat(buildSpec.sdk, is(equalTo(XcodePlugin.SDK_IPHONESIMULATOR)))
		assertThat(buildSpec.configuration, is(equalTo('Debug')))
		assertThat(buildSpec.devices, is(equalTo(Devices.UNIVERSAL)))


	}

	@Test
	void testMergeNothing() {
		buildSpec.target = "Test"
		parentBuildSpec.target = "Build"

		assertThat(buildSpec.target, is(equalTo("Test")))
	}


	@Test
	void testMerge() {
		buildSpec.target = ""
		parentBuildSpec.target = "Build"

		assertThat(buildSpec.target, is(equalTo("Build")))
	}

	@Test
	void testMergeEmpty() {
		buildSpec.target = null
		parentBuildSpec.target = ""

		assertThat(buildSpec.target, is(nullValue()))
	}

	@Test
	void testMergeConfiguration() {
		buildSpec.target = "Test"
		parentBuildSpec.configuration = "Debug"
		parentBuildSpec.sdk = "macosx"

		assertThat(buildSpec.target, is(equalTo("Test")))
		assertThat(buildSpec.configuration, is(equalTo("Debug")))
		assertThat(buildSpec.sdk, is(equalTo("macosx")))
	}

	@Test
	void testMergeAll() {
		parentBuildSpec.version = "version"
		parentBuildSpec.scheme = "scheme"
		parentBuildSpec.configuration = "configuration"
		parentBuildSpec.sdk = "sdk"
		parentBuildSpec.ipaFileName = "ipaFileName"
		parentBuildSpec.workspace = "workspace"
		parentBuildSpec.symRoot = "symRoot"
		parentBuildSpec.devices = Devices.PAD
		parentBuildSpec.productName = "productName"
		parentBuildSpec.infoPlist = "infoPlist"
		parentBuildSpec.productType = "productType"
		parentBuildSpec.bundleName = "bundleName"

		assertThat(buildSpec.version, is(equalTo("version")));
		assertThat(buildSpec.scheme, is(equalTo("scheme")));
		assertThat(buildSpec.configuration, is(equalTo("configuration")));
		assertThat(buildSpec.sdk, is(equalTo("sdk")));
		assertThat(buildSpec.ipaFileName, is(equalTo("ipaFileName")));
		assertThat(buildSpec.workspace, is(equalTo("workspace")));
		assertThat(buildSpec.symRoot, is(equalTo("symRoot")));
		assertThat(buildSpec.devices, is(Devices.PAD));
		assertThat(buildSpec.productName, is(equalTo("productName")));
		assertThat(buildSpec.infoPlist, is(equalTo("infoPlist")));
		assertThat(buildSpec.productType, is(equalTo("productType")));
		assertThat(buildSpec.bundleName, is(equalTo("bundleName")));
	}

	@Test
	void testWorkspaceNil() {
		assert buildSpec.workspace == null;
	}

	@Test
	void testWorkspace() {

		File workspace = new File(project.projectDir , "Test.xcworkspace")
		workspace.mkdirs()
		assert buildSpec.workspace == "Test.xcworkspace";

	}


	@Test
	void init() {

		buildSpec.sdk = "macosx"
		assertThat(buildSpec.sdk, is("macosx"))

		buildSpec.sdk = "iphoneos"
		assertThat(buildSpec.sdk, is("iphoneos"))


	}


}
