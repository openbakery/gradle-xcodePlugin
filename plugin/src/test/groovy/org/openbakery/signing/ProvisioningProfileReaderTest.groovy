package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.packaging.PackageTask
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*


/**
 * Created by Stefan Gugarel on 05/02/15.
 */
class ProvisioningProfileReaderTest {

	Project project

	PackageTask packageTask;

	File projectDir
	File buildOutputDirectory
	File appDirectory

	@Before
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.infoPlist = 'Info.plist'
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.sdk = XcodePlugin.SDK_MACOSX
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)


		buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration)
		buildOutputDirectory.mkdirs()

		appDirectory = new File(buildOutputDirectory, "Example.app")
		appDirectory.mkdirs()

		File infoPlist = new File("../example/OSX/ExampleOSX/ExampleOSX/Info.plist")
		FileUtils.copyFile(infoPlist, new File(appDirectory, "" + "Contents/Info.plist"))
	}

	@After
	void cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}

	@Test
	void readUUIDFromFile() {
		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/test.mobileprovision", project, new CommandRunner())
		assertThat(reader.getUUID(), is(equalTo("FFFFFFFF-AAAA-BBBB-CCCC-DDDDEEEEFFFF")))
	}

	@Test
	void readApplicationIdentifierPrefix() {
		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/test.mobileprovision", project, new CommandRunner())
		assertThat(reader.getApplicationIdentifierPrefix(), is(equalTo("AAAAAAAAAAA")))
	}


	@Test
	void readApplicationIdentifier() {
		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/test.mobileprovision", project, new CommandRunner())
		assertThat(reader.getApplicationIdentifier(), is(equalTo("org.openbakery.Example")))
	}


	@Test(expected = IllegalArgumentException.class)
	void readProfileHasExpired() {
		new ProvisioningProfileReader("src/test/Resource/expired.mobileprovision", project, new CommandRunner())
	}

	// OSX Tests

	@Test
	void readMacProvisioningProfile() {

		File wildcardMacProfile = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")

		assert wildcardMacProfile.exists()

		ProvisioningProfileReader provisioningProfileReader = new ProvisioningProfileReader(wildcardMacProfile, project, new CommandRunner())

		def applicationIdentifier = provisioningProfileReader.getApplicationIdentifier()

		assertThat(applicationIdentifier, is(equalTo("*")))

	}

	@Test
	void extractEntitlements() {

		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/test-wildcard-mac-development.provisionprofile", project, new CommandRunner())

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile)

		assertThat(entitlementsFile.exists(), is(true));

		String expectedContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
						"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
						"<plist version=\"1.0\">\n" +
						"<dict>\n" +
						"\t<key>com.apple.application-identifier</key>\n" +
						"\t<string>Z7L2YCUH45.*</string>\n" +
						"\t<key>com.apple.developer.aps-environment</key>\n" +
						"\t<string>development</string>\n" +
						"\t<key>com.apple.developer.icloud-container-development-container-identifiers</key>\n" +
						"\t<array/>\n" +
						"\t<key>com.apple.developer.icloud-container-environment</key>\n" +
						"\t<array>\n" +
						"\t\t<string>Development</string>\n" +
						"\t\t<string>Production</string>\n" +
						"\t</array>\n" +
						"\t<key>com.apple.developer.icloud-container-identifiers</key>\n" +
						"\t<array/>\n" +
						"\t<key>com.apple.developer.icloud-services</key>\n" +
						"\t<string>*</string>\n" +
						"\t<key>com.apple.developer.team-identifier</key>\n" +
						"\t<string>Z7L2YCUH45</string>\n" +
						"\t<key>com.apple.developer.ubiquity-container-identifiers</key>\n" +
						"\t<array/>\n" +
						"\t<key>com.apple.developer.ubiquity-kvstore-identifier</key>\n" +
						"\t<string>Z7L2YCUH45.*</string>\n" +
						"\t<key>keychain-access-groups</key>\n" +
						"\t<array>\n" +
						"\t\t<string>Z7L2YCUH45.*</string>\n" +
						"\t</array>\n" +
						"</dict>\n" +
						"</plist>"

		assertThat(entitlementsFile.text, is(equalTo(expectedContents)));

	}
}
