package org.openbakery.internal

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.Devices
import org.openbakery.XcodePlugin
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 14.08.15.
 */
class XcodeBuildSpecTest {

	XcodeBuildSpec buildSpec
	XcodeBuildSpec parentBuildSpec
	Project project

	@Before
	void setup() {
		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin


		parentBuildSpec = new XcodeBuildSpec(project);
		buildSpec = new XcodeBuildSpec(project, parentBuildSpec);

	}

	@After
	void cleanup() {
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
	void testConfiguration() {
		buildSpec.target = "Test"
		parentBuildSpec.configuration = "Debug"
		parentBuildSpec.sdk = "macosx"

		assertThat(buildSpec.target, is(equalTo("Test")))
		assertThat(buildSpec.configuration, is(equalTo("Debug")))
		assertThat(buildSpec.sdk, is(equalTo("macosx")))
	}


	@Test
	void testMergeVersion() {
		parentBuildSpec.version = "version"
		assertThat(buildSpec.version, is(equalTo("version")));
	}


	@Test
	void testMergeScheme() {
		parentBuildSpec.scheme = "scheme"
		assertThat(buildSpec.scheme, is(equalTo("scheme")));
	}


	@Test
	void testMergeConfiguration() {
		parentBuildSpec.configuration = "configuration"
		assertThat(buildSpec.configuration, is(equalTo("configuration")));
	}

	@Test
	void testMergeSdk() {
		parentBuildSpec.sdk = "sdk"
		assertThat(buildSpec.sdk, is(equalTo("sdk")));
	}

	@Test
	void testMergeIpaFileName() {
		parentBuildSpec.ipaFileName = "ipaFileName"
		assertThat(buildSpec.ipaFileName, is(equalTo("ipaFileName")));
	}

	@Test
	void testMergeWorkspace() {
		parentBuildSpec.workspace = "workspace"
		assertThat(buildSpec.workspace, is(equalTo("workspace")));
	}

	@Test
	void testMergeDevices() {
		parentBuildSpec.devices = Devices.PAD
		assertThat(buildSpec.devices, is(Devices.PAD));
	}

	@Test
	void testMergeProductName() {
		parentBuildSpec.productName = "productName"
		assertThat(buildSpec.productName, is(equalTo("productName")));
	}

	@Test
	void testMergeInfoPlist() {
		parentBuildSpec.infoPlist = "infoPlist"
		assertThat(buildSpec.infoPlist, is(equalTo("infoPlist")));
	}

	@Test
	void testMergeProductType() {
		parentBuildSpec.productType = "productType"
		assertThat(buildSpec.productType, is(equalTo("productType")));
	}

	@Test
	void testMergeBundleName() {
		parentBuildSpec.bundleName = "bundleName"
		assertThat(buildSpec.bundleName, is(equalTo("bundleName")));
	}


	@Test
	void testMergeSymRoot() {
		parentBuildSpec.symRoot = "symRoot"
		assertThat(buildSpec.symRoot, is(instanceOf(File)));
		assertThat(buildSpec.symRoot.absolutePath, endsWith("symRoot"));
	}

	@Test
	void testMergeDstRoot() {
		parentBuildSpec.dstRoot = "dstRoot"
		assertThat(buildSpec.dstRoot, is(instanceOf(File)));
		assertThat(buildSpec.dstRoot.absolutePath, endsWith("dstRoot"));
	}

	@Test
	void testMergeObjRoot() {
		parentBuildSpec.objRoot = "objRoot"
		assertThat(buildSpec.objRoot, is(instanceOf(File)));
		assertThat(buildSpec.objRoot.absolutePath, endsWith("objRoot"));
	}

	@Test
	void testMergeSharedPrecompsDir() {
		parentBuildSpec.sharedPrecompsDir = "sharedPrecompsDir"
		assertThat(buildSpec.sharedPrecompsDir, is(instanceOf(File)));
		assertThat(buildSpec.sharedPrecompsDir.absolutePath, endsWith("sharedPrecompsDir"))
	}


	@Test
	void testMergeBundleNameSuffix() {
		parentBuildSpec.bundleNameSuffix = "bundleNameSuffix"
		assertThat(buildSpec.bundleNameSuffix, is("bundleNameSuffix"));
	}


	@Test
	void testWorkspaceNil() {
		assert buildSpec.workspace == null;
	}

	@Test
	void testWorkspaceValue() {

		File workspace = new File(project.projectDir , "Test.xcworkspace")
		workspace.mkdirs()
		assert buildSpec.workspace == "Test.xcworkspace";

	}


	@Test
	void testMergeAdditionalParameters() {
		parentBuildSpec.additionalParameters = ['one', 'two']
		assertThat(buildSpec.additionalParameters, contains('one', 'two'));
	}

	@Test
	void testMergeArch() {
		parentBuildSpec.arch = ['i368', 'x86_64']
		assertThat(buildSpec.arch, contains('i368', 'x86_64'));
	}

	@Test
	void init() {

		buildSpec.sdk = "macosx"
		assertThat(buildSpec.sdk, is("macosx"))

		buildSpec.sdk = "iphoneos"
		assertThat(buildSpec.sdk, is("iphoneos"))

	}

	@Test
	void testMergeSigning() {
		parentBuildSpec.signing.identity = "Me"
		assertThat(buildSpec.signing.identity, is("Me"));
	}

	@Test
	void testMergeEnvironment() {
		parentBuildSpec.environment = ["foo" : "bar"]
		assertThat(buildSpec.environment, hasEntry("foo", "bar"));
	}


	@Test
	void testEnvironment() {
		buildSpec.environment = ["foo" : 1]
		assertThat(buildSpec.environment, hasEntry("foo", "1"));
	}

	@Test
	void testEnvironment_asString() {
		buildSpec.environment = "foo=bar"
		assertThat(buildSpec.environment, hasEntry("foo", "bar"));

		buildSpec.environment = "foo="
		assertThat(buildSpec.environment, hasEntry("foo", ""));

	}


	@Test
	void testWith() {
		parentBuildSpec.sdk = "macosx"

		def third = new XcodeBuildSpec(project)
		third.with(buildSpec)

		assertThat(third.sdk, is("macosx"))

	}

	@Test
	void testWithThatHasParent() {
		parentBuildSpec.sdk = "macosx"

		def third = new XcodeBuildSpec(project)
		buildSpec.with(third)

		assertThat(buildSpec.sdk, is("macosx"))
	}

	@Test
	void testWithThatHasParent_2() {
		parentBuildSpec.sdk = "macosx"

		def third = new XcodeBuildSpec(project)
		third.sdk = "Foo"
		buildSpec.with(third)

		assertThat(buildSpec.sdk, is("Foo"))

	}
}
