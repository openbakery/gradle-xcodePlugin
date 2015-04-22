package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.PlistHelper
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.openbakery.packaging.PackageTask
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by Stefan Gugarel on 10/02/15.
 */
class PackageTaskOSXTest {

	Project project
	PackageTask packageTask;

	GMockController mockControl
	CommandRunner commandRunnerMock

	File provisionLibraryPath
	File projectDir
	File infoPlist
	File appDirectory
	File archiveDirectory
	File provisionProfile



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
		packageTask.plistHelper = new PlistHelper(project, commandRunnerMock)

		packageTask.setProperty("commandRunner", commandRunnerMock)


		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		appDirectory = new File(packageTask.outputPath, "Example.app");

		provisionProfile = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
	}

	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(projectDir)
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
		File payloadApp = new File(packageTask.outputPath, path)

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

		File infoPlist = new File(this.appDirectory, "Contents/Info.plist")


		mockPlistCommmand(infoPlist.absolutePath, "Delete CFBundleResourceSpecification")

		mockCodesignCommand("Example.app")

		if (withFramework) {
			mockCodesignCommand("Example.app/Contents/Frameworks/Sparkle.framework/Versions/Current")
		}

		project.xcodebuild.outputPath.mkdirs()
	}

	@Test
	void testCreatePackagePath() {
		mockExampleApp(false, false)

		mockControl.play {
			packageTask.packageApplication()
		}

		// has to be same folder as signing for MacOSX
		assert packageTask.outputPath.exists()
	}

	@Test
	void testCopyApp() {

		mockExampleApp(false, false)

		mockControl.play {
			packageTask.packageApplication()
		}
		assert appDirectory.exists()
	}

	@Test
	void removeResourceRules() {

		mockExampleApp(false, false)

		mockControl.play {
			packageTask.packageApplication()
		}

		assert !(new File(appDirectory, "ResourceRules.plist")).exists()
	}

	@Test
	void codesignMacAppOnly() {

		mockExampleApp(false, false)


		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		mockControl.play {
			packageTask.packageApplication()
		}
	}

	@Test
	void codesignMacAppWithFramework() {

		mockExampleApp(true, false)

		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		mockControl.play {
			packageTask.packageApplication()
		}
	}


	@Test
	void embedProvisioningProfile() {

		mockExampleApp(false, false)

		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		mockControl.play {
			packageTask.packageApplication()
		}

		File embedProvisioningProfile = new File(packageTask.outputPath, "/Example.app/Contents/embedded.provisionprofile")
		assert !embedProvisioningProfile.exists()

	}

	@Test
	void embedProvisioningProfileWithFramework() {

		mockExampleApp(true, false)

		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		mockControl.play {
			packageTask.packageApplication()
		}

		File embedProvisioningProfile = new File(packageTask.outputPath, "/Example.app/Contents/embedded.provisionprofile")
		assert !embedProvisioningProfile.exists()

	}


	List<String> getZipEntries(File file) {
		ZipFile zipFile = new ZipFile(file);

		List<String> entries = new ArrayList<String>()
		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}
		return entries;
	}

	@Test
	void outputFile() {
		mockExampleApp(true, false)

		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		mockControl.play {
			packageTask.packageApplication()
		}

		File outputFile = new File(packageTask.outputPath, "Example.zip")
		assert outputFile.exists();

		List<String> zipEntries = getZipEntries(outputFile);

		assert zipEntries.contains("Example.app/Example")

	}
}
