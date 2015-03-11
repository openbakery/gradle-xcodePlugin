package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.PlistHelper
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by rene on 13.11.14.
 */
class PackageTaskTest {


	Project project
	PackageTask packageTask;

	GMockController mockControl
	CommandRunner commandRunnerMock

	File provisionLibraryPath
	File projectDir
	File infoPlist
	File payloadAppDirectory
	File archiveDirectory

	@BeforeMethod
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		//project.xcodebuild.infoPlist = 'Info.plist'
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.sdk = XcodePlugin.SDK_IPHONEOS
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)

		packageTask.setProperty("commandRunner", commandRunnerMock)

		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		//infoPlist = new File(project.buildDir, project.xcodebuild.infoPlist)
		//FileUtils.writeStringToFile(infoPlist, "dummy")

		File payloadDirectory = new File(project.xcodebuild.signing.signingDestinationRoot, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "Example.app");
	}

	void mockExampleApp(boolean withPlugin, boolean withSwift) {
		String widgetPath = "PlugIns/ExampleTodayWidget.appex"
		// create dummy app


		def applicationBundle = new File(archiveDirectory, "Products/Applications/Example.app")

		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
		}

		FileUtils.writeStringToFile(new File(appDirectory, "Example"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "ResourceRules.plist"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "Info.plist"), "dummy");

		if (withPlugin) {
			File widgetsDirectory = new File(applicationBundle, widgetPath)
			FileUtils.writeStringToFile(new File(widgetsDirectory, "ExampleTodayWidget"), "dummy");
		}

		File infoPlist = new File(payloadAppDirectory, "Info.plist")
		mockValueFromPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")

		if (withPlugin) {
			File infoPlistWidget = new File(payloadAppDirectory, widgetPath + "/Info.plist");
			mockValueFromPlist(infoPlistWidget.absolutePath, "CFBundleIdentifier", "org.openbakery.ExampleWidget")
		}

		mockPlistCommmand(infoPlist.absolutePath, "Delete CFBundleResourceSpecification")

		mockCodesignCommand("Payload/Example.app")
		if (withPlugin) {
			mockCodesignCommand("Payload/Example.app/" + widgetPath)
		}
		project.xcodebuild.outputPath.mkdirs()

		if (withSwift) {



			File libSwiftCore = new File(applicationBundle, "Frameworks/libswiftCore.dylib")
			FileUtils.writeStringToFile(libSwiftCore, "dummy")
			File libSwiftCoreArchive = new File(archiveDirectory, "SwiftSupport/libswiftCore.dylib")
			FileUtils.writeStringToFile(libSwiftCoreArchive, "dummy")

			File libswiftCoreGraphics = new File(applicationBundle, "Frameworks/libswiftCoreGraphics.dylib")
			FileUtils.writeStringToFile(libswiftCoreGraphics, "dummy")

			mockCodesignSwiftCommand("Payload/Example.app/Frameworks/libswiftCore.dylib")
			mockCodesignSwiftCommand("Payload/Example.app/Frameworks/libswiftCoreGraphics.dylib")


		}

	}



	void mockCodesignSwiftCommand(String path) {
		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
		File payloadApp = new File(project.xcodebuild.signing.signingDestinationRoot, path)

		def commandList = [
						"/usr/bin/codesign",
						"--force",
						"--sign",
						"iPhone Developer: Firstname Surename (AAAAAAAAAA)",
						"--verbose",
						payloadApp.absolutePath,
						"--keychain",
						"/var/tmp/gradle.keychain"

		]
		commandRunnerMock.run(commandList)

	}

	void mockCodesignCommand(String path) {
		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
		File payloadApp = new File(project.xcodebuild.signing.signingDestinationRoot, path)

		def commandList = [
						"/usr/bin/codesign",
						"--force",
						"--preserve-metadata=identifier,entitlements",
						"--sign",
						"iPhone Developer: Firstname Surename (AAAAAAAAAA)",
						"--verbose",
						payloadApp.absolutePath,
						"--keychain",
						"/var/tmp/gradle.keychain"

		]
		commandRunnerMock.run(commandList)

	}



	void mockPlistCommmand(String infoplist, String command) {
		def commandList = ["/usr/libexec/PlistBuddy", infoplist, "-c", command]
		commandRunnerMock.run(commandList).atLeastOnce()
	}


	void mockValueFromPlist(String infoplist, String key, String value) {
		def commandList = ["/usr/libexec/PlistBuddy", infoplist, "-c", "Print :" + key]
		commandRunnerMock.runWithResult(commandList).returns(value).atLeastOnce()
	}

	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	@Test
	void testCreatePayload() {
		mockExampleApp(false, false)

		mockControl.play {
			packageTask.packageApplication()
		}
		File payloadDirectory = new File(project.xcodebuild.signing.signingDestinationRoot, "Payload")
		assert payloadDirectory.exists()
	}

	@Test
	void testCopyApp() {

		mockExampleApp(false, false)

		mockControl.play {
			packageTask.packageApplication()
		}
		assert payloadAppDirectory.exists()
	}

	@Test
	void removeResourceRules() {

		mockExampleApp(false, false)


		mockControl.play {
			packageTask.packageApplication()
		}

		assert !(new File(payloadAppDirectory, "ResourceRules.plist")).exists()




	}

	@Test
	void embedProvisioningProfile() {

		mockExampleApp(false, false)

		File mobileprovision = new File("src/test/Resource/test.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision

		mockControl.play {
			packageTask.packageApplication()
		}

		File embedProvisioningProfile = new File(project.xcodebuild.signing.signingDestinationRoot, "Payload/Example.app/embedded.mobileprovision")
		assert embedProvisioningProfile.exists()

		assert FileUtils.checksumCRC32(embedProvisioningProfile) == FileUtils.checksumCRC32(mobileprovision)
	}


	@Test
	void embedMultipleProvisioningProfile() {
		mockExampleApp(true, false)

		File firstMobileprovision = new File("src/test/Resource/test.mobileprovision")
		File secondMobileprovision = new File("src/test/Resource/test1.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = firstMobileprovision
		project.xcodebuild.signing.mobileProvisionFile = secondMobileprovision

		mockControl.play {
			packageTask.packageApplication()
		}

		File firstEmbedProvisioningProfile = new File(project.xcodebuild.signing.signingDestinationRoot, "Payload/Example.app/embedded.mobileprovision")
		assert firstEmbedProvisioningProfile.exists()

		assert FileUtils.checksumCRC32(firstEmbedProvisioningProfile) == FileUtils.checksumCRC32(firstMobileprovision)

		File secondEmbedProvisioningProfile = new File(project.xcodebuild.signing.signingDestinationRoot, "Payload/Example.app/PlugIns/ExampleTodayWidget.appex/embedded.mobileprovision")
		assert secondEmbedProvisioningProfile.exists()

		assert FileUtils.checksumCRC32(secondEmbedProvisioningProfile) == FileUtils.checksumCRC32(secondMobileprovision)

	}

	@Test
	void testSign() {

		mockExampleApp(false, false)


		mockControl.play {
			packageTask.packageApplication()
		}
	}

	@Test
	void testSignMultiple() {

		mockExampleApp(true, false)

		mockControl.play {
			packageTask.packageApplication()
		}
	}

	@Test
	void testIpa() {
		mockExampleApp(false, false)

		mockControl.play {
			packageTask.packageApplication()
		}


		File ipaBundle = new File(project.getBuildDir(), "package/Example.ipa")

		assert ipaBundle.exists()

		ZipFile zipFile = new ZipFile(ipaBundle);

		List<String> entries = new ArrayList<String>()

		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}

		assert entries.contains("Payload/Example.app/Example")

	}

	@Test
	void provisioningMatch() {
		File appMobileprovision = new File("src/test/Resource/test.mobileprovision")
		File widgetMobileprovision = new File("src/test/Resource/test1.mobileprovision")
		File wildcardMobileprovision = new File("src/test/Resource/test-wildcard.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = appMobileprovision
		project.xcodebuild.signing.mobileProvisionFile = widgetMobileprovision
		project.xcodebuild.signing.mobileProvisionFile = wildcardMobileprovision


		assert packageTask.getMobileProvisionFileForIdentifier("org.openbakery.Example") == appMobileprovision
		assert packageTask.getMobileProvisionFileForIdentifier("org.openbakery.ExampleWidget") == widgetMobileprovision

		assert packageTask.getMobileProvisionFileForIdentifier("org.openbakery.Test") == wildcardMobileprovision
		assert packageTask.getMobileProvisionFileForIdentifier("org.Test") == wildcardMobileprovision

	}

	@Test
	void swiftFramework() {

		mockExampleApp(false, true)

		mockControl.play {
			packageTask.packageApplication()
		}

		File ipaBundle = new File(project.getBuildDir(), "package/Example.ipa")

		assert ipaBundle.exists()

		ZipFile zipFile = new ZipFile(ipaBundle);

		List<String> entries = new ArrayList<String>()

		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}

		assert entries.contains("SwiftSupport/libswiftCore.dylib")
	}


	@Test
	void swiftCodesignLibs() {

		mockExampleApp(false, true)


		mockControl.play {
			packageTask.packageApplication()
		}
	}


}
