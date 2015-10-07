package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.PlistHelper
import org.openbakery.Type
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.openbakery.packaging.PackageTask
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openbakery.stubs.PlistHelperStub

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

	PlistHelperStub plistHelperStub = new PlistHelperStub()

	File provisionLibraryPath
	File projectDir
	File infoPlist
	File payloadAppDirectory
	File archiveDirectory

	@Before
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
		project.xcodebuild.type = Type.iOS
		project.xcodebuild.simulator = false
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"


		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)
		packageTask.plistHelper = plistHelperStub

		packageTask.setProperty("commandRunner", commandRunnerMock)

		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		//infoPlist = new File(project.buildDir, project.xcodebuild.infoPlist)
		//FileUtils.writeStringToFile(infoPlist, "dummy")

		File payloadDirectory = new File(packageTask.outputPath, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "Example.app");
	}

	void mockExampleApp(boolean withPlugin, boolean withSwift) {
		mockExampleApp(withPlugin, withSwift, false)
	}

	void mockExampleApp(boolean withPlugin, boolean withSwift, boolean withFramework) {
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
		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")

		if (withPlugin) {
			File infoPlistWidget = new File(payloadAppDirectory, widgetPath + "/Info.plist");
			plistHelperStub.setValueForPlist(infoPlistWidget.absolutePath, "CFBundleIdentifier", "org.openbakery.ExampleWidget")
		}

		mockCodesignCommand("Payload/Example.app", "entitlements_test.plist")
		if (withPlugin) {
			mockCodesignCommand("Payload/Example.app/" + widgetPath, "entitlements_test1.plist")
		}
		project.xcodebuild.outputPath.mkdirs()

		if (withSwift) {



			File libSwiftCore = new File(applicationBundle, "Frameworks/libswiftCore.dylib")
			FileUtils.writeStringToFile(libSwiftCore, "dummy")
			File libSwiftCoreArchive = new File(archiveDirectory, "SwiftSupport/libswiftCore.dylib")
			FileUtils.writeStringToFile(libSwiftCoreArchive, "dummy")

			File libswiftCoreGraphics = new File(applicationBundle, "Frameworks/libswiftCoreGraphics.dylib")
			FileUtils.writeStringToFile(libswiftCoreGraphics, "dummy")

			mockCodesignLibCommand("Payload/Example.app/Frameworks/libswiftCore.dylib")
			mockCodesignLibCommand("Payload/Example.app/Frameworks/libswiftCoreGraphics.dylib")


		}

		if (withFramework) {
			File framework = new File(applicationBundle, "Frameworks/My.framework")
			framework.mkdirs()
			File frameworkFile = new File(applicationBundle, "Frameworks/My.framework/My")
			FileUtils.writeStringToFile(frameworkFile, "dummy")

			mockCodesignLibCommand("Payload/Example.app/Frameworks/My.framework")

		}

		File mobileprovision = new File("src/test/Resource/test.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision
		mockEntitlementsFromPlist(mobileprovision)

		if (withPlugin) {
			File widgetMobileprovision = new File("src/test/Resource/test1.mobileprovision")
			project.xcodebuild.signing.mobileProvisionFile = widgetMobileprovision
			mockEntitlementsFromPlist(widgetMobileprovision)
		}


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
		commandRunnerMock.run(commandList)

	}

	void mockCodesignCommand(String path, String entitlementsName) {
		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
		File payloadApp = new File(packageTask.outputPath, path)
		File entitlements = new File(project.buildDir.absolutePath, "package/" + entitlementsName)

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



	void mockPlistCommmand(String infoplist, String command) {
		def commandList = ["/usr/libexec/PlistBuddy", infoplist, "-c", command]
		commandRunnerMock.run(commandList).atLeastOnce()
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
	}

	@After
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	@Test
	void testCreatePayload() {
		mockExampleApp(false, false)

		mockControl.play {
			packageTask.packageApplication()
		}
		File payloadDirectory = new File(packageTask.outputPath, "Payload")
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

		File embedProvisioningProfile = new File(packageTask.outputPath, "Payload/Example.app/embedded.mobileprovision")
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

		File firstEmbedProvisioningProfile = new File(packageTask.outputPath, "Payload/Example.app/embedded.mobileprovision")
		assert firstEmbedProvisioningProfile.exists()

		assert FileUtils.checksumCRC32(firstEmbedProvisioningProfile) == FileUtils.checksumCRC32(firstMobileprovision)

		File secondEmbedProvisioningProfile = new File(packageTask.outputPath, "Payload/Example.app/PlugIns/ExampleTodayWidget.appex/embedded.mobileprovision")
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
	void provisioningMatch_2() {
		File appMobileprovision = new File("src/test/Resource/openbakery.mobileprovision")
		File wildcardMobileprovision = new File("src/test/Resource/openbakery-wildcard.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = appMobileprovision
		project.xcodebuild.signing.mobileProvisionFile = wildcardMobileprovision


		assert packageTask.getMobileProvisionFileForIdentifier("org.openbakery.Example") == appMobileprovision
		assert packageTask.getMobileProvisionFileForIdentifier("org.openbakery.Example.widget") == wildcardMobileprovision
		assert packageTask.getMobileProvisionFileForIdentifier("org.openbakery.Example.extension") == wildcardMobileprovision

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


	@Test
	void codesignFramework() {
		mockExampleApp(false, true, true)

		mockControl.play {
			packageTask.packageApplication()
		}
	}

	@Test(expected = IllegalArgumentException)
	void hasNoSigning() {
		project.xcodebuild.signing = null
		packageTask.packageApplication()
	}

	@Test(expected = IllegalArgumentException)
	void hasNoSigningIdentity() {
		project.xcodebuild.signing.identity = null
		packageTask.packageApplication()
	}

	@Test
	void dependsOn() {
		def dependsOn  = packageTask.getDependsOn()
		assert dependsOn.size() == 4

		assert dependsOn.contains(XcodePlugin.ARCHIVE_TASK_NAME)
		assert dependsOn.contains(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME)
		assert dependsOn.contains(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)

	}

	@Test
	void finalized() {
		def finalized = packageTask.finalizedBy.values
		assert finalized.contains(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME)
	}


	@Test
	void deleteCFBundleResourceSpecification() {
		mockExampleApp(false, true)

		mockControl.play {
			packageTask.packageApplication()
		}

		assert plistHelperStub.plistCommands.size() == 1
		assert plistHelperStub.plistCommands.get(0).equals("Delete CFBundleResourceSpecification")
	}


}
