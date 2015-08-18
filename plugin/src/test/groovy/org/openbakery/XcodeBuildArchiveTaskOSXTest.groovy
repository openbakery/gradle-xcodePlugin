package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by Stefan Gugarel on 04/02/15.
 */
class XcodeBuildArchiveTaskOSXTest {

	Project project

	XcodeBuildArchiveTask xcodeBuildArchiveTask;

	File projectDir
	File buildOutputDirectory
	File appDirectory

	GMockController mockControl
	CommandRunner commandRunnerMock

	@Before
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.infoPlist = 'Info.plist'
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.sdk = XcodePlugin.SDK_MACOSX
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		xcodeBuildArchiveTask = project.getTasks().getByPath(XcodePlugin.ARCHIVE_TASK_NAME)

		buildOutputDirectory = new File(project.xcodebuild.symRoot, "Debug")
		buildOutputDirectory.mkdirs()

		appDirectory = new File(buildOutputDirectory, "Example.app")
		appDirectory.mkdirs()

		File infoPlist = new File("../example/OSX/ExampleOSX/ExampleOSX/Info.plist")
		FileUtils.copyFile(infoPlist, new File(appDirectory, "" + "Contents/Info.plist"))
	}


	@After
	void cleanAfterTest() {
		FileUtils.deleteDirectory(projectDir)
	}


	@Test
	void copyOSXApp() {

		xcodeBuildArchiveTask.executeTask()

		File appFile = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app")

		assert appFile.exists(): "App file does not exist: " + appFile.absolutePath

	}

	@Test
	void createInfoPlist() {

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"

		xcodeBuildArchiveTask.executeTask()

		File infoPlist = new File(projectDir, "build/archive/Example.xcarchive/Info.plist")

		assert infoPlist.exists(): "file does not exist: " + infoPlist.absolutePath

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(infoPlist)

		assert config.getString("ApplicationProperties.ApplicationPath").equals("Applications/Example.app")
		assert config.getString("ApplicationProperties.CFBundleIdentifier").equals("com.cocoanetics.ExampleOSX")
		assert config.getString("ApplicationProperties.CFBundleShortVersionString").equals("1.0")
		assert config.getString("ApplicationProperties.CFBundleVersion").equals("1")
		assert config.getString("ApplicationProperties.SigningIdentity").equals("iPhone Developer: Firstname Surename (AAAAAAAAAA)")

		assert config.getString("Name").equals("Example")
		assert config.getString("SchemeName").equals("Example")

	}

	@Test
	void getIconPathMacOSX() {

		// Info.plist from Example.app
		File infoPlistInAppFile = new File(projectDir, "/build/sym/Debug/Example.app/Contents/Info.plist")


		//xcodeBuildArchiveTask.configureTask()
		// add key CFBundleIconFile
		xcodeBuildArchiveTask.plistHelper.setValueForPlist(infoPlistInAppFile, "CFBundleIconFile", "icon")

		def macOSXIcons = xcodeBuildArchiveTask.getMacOSXIcons(infoPlistInAppFile)

		assert macOSXIcons.size() == 1
		assert macOSXIcons.get(0).equals("Applications/Example.app/Contents/Resources/icon.icns")
	}

	@Test
	void getNoIconMacOSX() {

		// Info.plist from Example.app
		File infoPlistInAppFile = new File(projectDir, "/build/sym/Debug/Example.app/Contents/Info.plist")

		def macOSXIcons = xcodeBuildArchiveTask.getMacOSXIcons(infoPlistInAppFile)

		assert macOSXIcons.size() == 0
	}

	void mockGetPlistValues(File plist, String key, String value) {

		def commandList = ["/usr/libexec/PlistBuddy", plist.absolutePath, "-c", "Print :" + key]
		commandRunnerMock.runWithResult(commandList).returns(value);

	}

	@Test
	void doNotConvertInfoPlistToBinary() {
		xcodeBuildArchiveTask.plistHelper = new PlistHelper(project, commandRunnerMock)
		xcodeBuildArchiveTask.setProperty("commandRunner", commandRunnerMock)


		File infoPlist = new File(appDirectory, "Contents/Info.plist")
		mockGetPlistValues(infoPlist, "CFBundleIdentifier", "");
		mockGetPlistValues(infoPlist, "CFBundleShortVersionString", "");
		mockGetPlistValues(infoPlist, "CFBundleVersion", "");
		mockGetPlistValues(infoPlist, "CFBundleIconFile", "");

		File infoPlistToConvert = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Info.plist")

		List<String> commandList
		commandList?.clear()
		commandList = ["/usr/bin/plutil", "-convert", "binary1", infoPlistToConvert.absolutePath]
		commandRunnerMock.run(commandList).times(0)


		mockControl.play {
			xcodeBuildArchiveTask.executeTask()
		}
	}
}
