package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by Stefan on 10/02/15.
 */
class PackageTaskOSXTest {

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
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.sdk = XcodePlugin.SDK_MACOSX
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"
		project.xcodebuild.signing.identity = 'iPhone Developer: Firstname Surename (AAAAAAAAAA)'

		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)

		packageTask.setProperty("commandRunner", commandRunnerMock)
		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		payloadAppDirectory = new File(project.xcodebuild.signing.signingDestinationRoot, "Example.app");
	}

	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(projectDir)
	}


	@Test
	void codesignMacAppWithFramework() {

		mockExampleApp(true, false)

		File mobileprovision = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision

		mockControl.play {
			packageTask.packageApplication()
		}
		//File payloadDirectory = new File(project.xcodebuild.signing.signingDestinationRoot, "Payload")
		//assert payloadDirectory.exists()
	}


	@Test
	void embedProvisioningProfile() {

		mockExampleApp(false, false)

		File mobileprovision = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision

		mockControl.play {
			packageTask.packageApplication()
		}

		File embedProvisioningProfile = new File(project.xcodebuild.signing.signingDestinationRoot, "Example.app/Contents/embedded.provisionprofile")
		assert embedProvisioningProfile.exists()

		assert FileUtils.checksumCRC32(embedProvisioningProfile) == FileUtils.checksumCRC32(mobileprovision)
	}

	@Test
	void embedProvisioningProfileWithFramework() {

		mockExampleApp(true, false)

		File mobileprovision = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision

		mockControl.play {
			packageTask.packageApplication()
		}

		File embedProvisioningProfile = new File(project.xcodebuild.signing.signingDestinationRoot, "Example.app/Contents/embedded.provisionprofile")
		assert embedProvisioningProfile.exists()

		assert FileUtils.checksumCRC32(embedProvisioningProfile) == FileUtils.checksumCRC32(mobileprovision)
	}

	void mockExampleApp(boolean withFramework, boolean withSwift) {
		String frameworkPath = "Contents/Frameworks/Sparkle.framework"
		// create dummy app


		def applicationBundle = new File(archiveDirectory, "Products/Applications/Example.app")

		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
		}

		FileUtils.writeStringToFile(new File(appDirectory, "Example"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "ResourceRules.plist"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "Contents/Info.plist"), "dummy");

		if (withFramework) {
			File framworkFile = new File(appDirectory, frameworkPath)

			framworkFile.mkdirs()
		}

		File infoPlist = new File(payloadAppDirectory, "Contents/Info.plist")


		mockPlistCommmand(infoPlist.absolutePath, "Delete CFBundleResourceSpecification")


		mockValueFromPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")

		mockCodesignCommand("Example.app")

		if (withFramework) {
			mockCodesignCommand("Example.app/Contents/Frameworks/Sparkle.framework/Versions/Current")
		}

		project.xcodebuild.outputPath.mkdirs()

//		if (withSwift) {
//
//
//
//			File libSwiftCore = new File(applicationBundle, "Frameworks/libswiftCore.dylib")
//			FileUtils.writeStringToFile(libSwiftCore, "dummy")
//			File libSwiftCoreArchive = new File(archiveDirectory, "SwiftSupport/libswiftCore.dylib")
//			FileUtils.writeStringToFile(libSwiftCoreArchive, "dummy")
//
//			File libswiftCoreGraphics = new File(applicationBundle, "Frameworks/libswiftCoreGraphics.dylib")
//			FileUtils.writeStringToFile(libswiftCoreGraphics, "dummy")
//
//			mockCodesignSwiftCommand("Payload/Example.app/Frameworks/libswiftCore.dylib")
//			mockCodesignSwiftCommand("Payload/Example.app/Frameworks/libswiftCoreGraphics.dylib")
//
//
//		}

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
}
