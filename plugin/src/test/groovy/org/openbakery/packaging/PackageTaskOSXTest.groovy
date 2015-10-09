package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.Type
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openbakery.stubs.PlistHelperStub

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


	PlistHelperStub plistHelperStub = new PlistHelperStub()

	@Before
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)



		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.OSX
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"
		project.xcodebuild.signing.identity = 'iPhone Developer: Firstname Surename (AAAAAAAAAA)'

		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)
		packageTask.plistHelper = plistHelperStub

		packageTask.setProperty("commandRunner", commandRunnerMock)


		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		appDirectory = new File(packageTask.outputPath, "Example.app");

		provisionProfile = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
	}

	@After
	void cleanUp() {
		FileUtils.deleteDirectory(projectDir)
	}

	void mockCodesignLibCommand(String path) {
		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
		File payloadApp = new File(packageTask.outputPath, path)

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
		commandRunnerMock.run(commandList, ['DEVELOPER_DIR':'/Applications/Xcode.app/Contents/Developer/'])
	}

	void mockCodesignCommand(String path) {
		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
		File payloadApp = new File(packageTask.outputPath, path)
		File entitlements = new File(project.buildDir.absolutePath, "package/entitlements_test-wildcard-mac-development.plist")

		def commandList = [
				"/usr/bin/codesign",
				"--force",
				"--entitlements",
				entitlements.absolutePath,
				"--sign",
				"iPhone Developer: Firstname Surename (AAAAAAAAAA)",
				"--verbose",
				payloadApp.absolutePath,
				"--keychain",
				"/var/tmp/gradle.keychain"

		]
		commandRunnerMock.run(commandList, ['DEVELOPER_DIR':'/Applications/Xcode.app/Contents/Developer/'])

	}

	void mockValueFromPlist(String infoplist, String key, String value) {
		def commandList = ["/usr/libexec/PlistBuddy", infoplist, "-c", "Print :" + key]
		commandRunnerMock.runWithResult(commandList).returns(value).atLeastOnce()
	}

	void mockEntitlementsFromPlist(File provisioningProfile) {
		def commandList = ['security', 'cms', '-D', '-i', provisioningProfile.absolutePath]
		String result = new File('src/test/Resource/entitlements.plist').text
		commandRunnerMock.runWithResult(commandList).returns(result).atLeastOnce()

		String basename = FilenameUtils.getBaseName(provisioningProfile.path)
		File plist = new File(project.buildDir.absolutePath + "/tmp/provision_" + basename + ".plist")
		commandList = ['/usr/libexec/PlistBuddy', '-x', plist.absolutePath, '-c', 'Print Entitlements']
		commandRunnerMock.runWithResult(commandList).returns(result).atLeastOnce()

		mockValueFromPlist(plist.absolutePath, "Entitlements:com.apple.application-identifier", "org.openbakery.Example")

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


		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "Delete CFBundleResourceSpecification")

		mockCodesignCommand("Example.app")

		if (withFramework) {
			mockCodesignLibCommand("Example.app/Contents/Frameworks/Sparkle.framework")
		}

		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")


		File mobileprovision = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision
		mockEntitlementsFromPlist(mobileprovision)

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
