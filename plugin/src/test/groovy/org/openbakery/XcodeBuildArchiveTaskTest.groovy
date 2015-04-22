package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by rene on 01.12.14.
 */
class XcodeBuildArchiveTaskTest {

	Project project

	XcodeBuildArchiveTask xcodeBuildArchiveTask;

	File projectDir
	File buildOutputDirectory
	File appDirectory

	GMockController mockControl
	CommandRunner commandRunnerMock

	@BeforeMethod
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
		project.xcodebuild.sdk = XcodePlugin.SDK_IPHONEOS
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		xcodeBuildArchiveTask = project.getTasks().getByPath(XcodePlugin.ARCHIVE_TASK_NAME)
		xcodeBuildArchiveTask.plistHelper = new PlistHelper(project, commandRunnerMock)
		xcodeBuildArchiveTask.setProperty("commandRunner", commandRunnerMock)


		buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
		buildOutputDirectory.mkdirs()

		appDirectory = new File(buildOutputDirectory, "Example.app")
		appDirectory.mkdirs()

		File app = new File(appDirectory, "Example")
		FileUtils.writeStringToFile(app, "dummy")


		File dSymDirectory = new File(buildOutputDirectory, "Example.app.dSym")
		dSymDirectory.mkdirs()


		File infoPlist = new File("../example/iOS/Example/Example/Example-Info.plist")
		FileUtils.copyFile(infoPlist, new File(appDirectory, "Info.plist"))

		FileUtils.writeStringToFile(new File(buildOutputDirectory, "Example.app/Icon.png"), "dummy")
		FileUtils.writeStringToFile(new File(buildOutputDirectory, "Example.app/Icon-72.png"), "dummy")

	}

	void mockSwiftLibs() {
		def swiftLibs = [
						"libswiftCore.dylib",
						"libswiftCoreGraphics.dylib",
						"libswiftCoreImage.dylib",
						"libswiftDarwin.dylib",
						"libswiftDispatch.dylib",
						"libswiftFoundation.dylib",
						"libswiftObjectiveC.dylib",
						"libswiftSecurity.dylib",
						"libswiftUIKit.dylib"
		]
		swiftLibs[0..4].each { item ->
			File lib = new File(appDirectory, "Frameworks/" + item)
			FileUtils.writeStringToFile(lib, "foo")
		}

		project.xcodebuild.xcodePath = new File(projectDir, "xcode");

		File swiftLibsDirectory = new File(project.xcodebuild.xcodePath + "/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphoneos")
		swiftLibsDirectory.mkdirs();

		swiftLibs.each { item ->
			File lib = new File(swiftLibsDirectory, item)
			FileUtils.writeStringToFile(lib, "bar")
		}

	}

	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	@Test
	void archiveDirectory() {
		xcodeBuildArchiveTask.archive()

		File archiveDirectory = new File(projectDir, "build/archive/Example.xcarchive")
		assert archiveDirectory.exists() : "Archive directory does not exist: " + archiveDirectory.absolutePath

		assert archiveDirectory.isDirectory() : "Archive directory is not a directory"
	}

	@Test
	void archiveDirectoryWithBundleSuffix() {
		project.xcodebuild.bundleNameSuffix = "-1.2.3"

		xcodeBuildArchiveTask.archive()

		File archiveDirectory = new File(projectDir, "build/archive/Example-1.2.3.xcarchive")
		assert archiveDirectory.exists() : "Archive directory does not exist: " + archiveDirectory.absolutePath

		assert archiveDirectory.isDirectory() : "Archive directory is not a directory"
	}


	@Test
	void applicationsFolder() {

		xcodeBuildArchiveTask.archive()

		File applicationsDirectory = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications")
		assert applicationsDirectory.exists(): "Applications directory does not exist: " + applicationsDirectory.absolutePath


	}

	@Test
	void copyApp() {

		xcodeBuildArchiveTask.archive()

		File appFile = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Example")

		assert appFile.exists(): "App file does not exist: " + appFile.absolutePath

	}

	@Test
	void copyDsym() {
		xcodeBuildArchiveTask.archive()

		File dsymFile = new File(projectDir, "build/archive/Example.xcarchive/dSYMs/Example.app.dSYM")

		assert dsymFile.exists(): "App file does not exist: " + dsymFile.absolutePath

	}

	@Test
	void copyMultipleDsyms() {

		File extensionDirectory = new File(buildOutputDirectory, "Example.app/PlugIns/ExampleTodayWidget.appex")
		extensionDirectory.mkdirs()

		File dSymDirectory = new File(buildOutputDirectory, "ExampleTodayWidget.appex.dSYM")
		dSymDirectory.mkdirs()

		xcodeBuildArchiveTask.archive()

		File dsymFile = new File(projectDir, "build/archive/Example.xcarchive/dSYMs/ExampleTodayWidget.appex.dSYM")

		assert dsymFile.exists(): "App file does not exist: " + dsymFile.absolutePath


		dsymFile = new File(projectDir, "build/archive/Example.xcarchive/dSYMs/Example.app.dSYM")
		assert dsymFile.exists(): "App file does not exist: " + dsymFile.absolutePath
	}


	@Test
	void createInfoPlist() {

		xcodeBuildArchiveTask.plistHelper = new PlistHelper(project, new CommandRunner())

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"

		xcodeBuildArchiveTask.archive()

		File infoPlist = new File(projectDir, "build/archive/Example.xcarchive/Info.plist")

		assert infoPlist.exists(): "file does not exist: " + infoPlist.absolutePath

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(infoPlist)

		assert config.getString("ApplicationProperties.ApplicationPath").equals("Applications/Example.app")
		assert config.getString("ApplicationProperties.CFBundleIdentifier").equals("org.openbakery.Example")
		assert config.getString("ApplicationProperties.CFBundleShortVersionString").equals("1.0")
		assert config.getString("ApplicationProperties.CFBundleVersion").equals("1.0")
		assert config.getString("ApplicationProperties.SigningIdentity").equals("iPhone Developer: Firstname Surename (AAAAAAAAAA)")


		List icons = config.getList("ApplicationProperties.IconPaths");
		assert icons.size() == 2

		assert icons.get(0).equals("Applications/Example.app/Icon-72.png")
		assert icons.get(1).equals("Applications/Example.app/Icon.png")


		assert config.getString("Name").equals("Example")
		assert config.getString("SchemeName").equals("Example")


	}

	@Test
	void testZipForSimulatorBuild() {
		project.xcodebuild.sdk = XcodePlugin.SDK_IPHONESIMULATOR
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
		buildOutputDirectory.mkdirs()

		File appDirectory = new File(buildOutputDirectory, "Example.app")
		appDirectory.mkdirs()
		File app = new File(appDirectory, "Example")
		FileUtils.writeStringToFile(app, "dummy")

		xcodeBuildArchiveTask.archive()

		File zipFile = new File(projectDir, "build/archive/Example.zip");
		assert zipFile.exists() : "Zipfile does not exist: " + zipFile.absolutePath


		ZipFile zip = new ZipFile(zipFile);

		List<String> entries = new ArrayList<String>()

		for (ZipEntry entry : zip.entries()) {
			entries.add(entry.getName())
		}

		assert entries.contains("Example.app/Example")

	}




	@Test
	void swiftFrameworkInApp() {
		mockSwiftLibs()
		xcodeBuildArchiveTask.archive()

		File libswiftCore = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")

		assert libswiftCore.exists(): "libswiftCore file does not exist: " + libswiftCore.absolutePath


		File supportLibswiftDirectory = new File(projectDir, "build/archive/Example.xcarchive/SwiftSupport/")

		assert supportLibswiftDirectory.list().length == 5

		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")

		assert supportLibswiftCore.exists(): "libswiftCore file does not exist: " + supportLibswiftCore.absolutePath

		assert FileUtils.readFileToString(supportLibswiftCore).equals("bar")
	}


	void mockGetPlistValues(File plist, String key, String value) {

		def commandList = ["/usr/libexec/PlistBuddy", plist.absolutePath, "-c", "Print :" + key]
		commandRunnerMock.runWithResult(commandList).returns(value);

	}

	@Test
	void convertInfoPlistToBinary() {

		File infoPlist = new File(appDirectory, "Info.plist")
		mockGetPlistValues(infoPlist, "CFBundleIdentifier", "");
		mockGetPlistValues(infoPlist, "CFBundleShortVersionString", "");
		mockGetPlistValues(infoPlist, "CFBundleVersion", "");

		File infoPlistToConvert = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Info.plist")

		List<String> commandList
		commandList?.clear()
		commandList = ["/usr/bin/plutil", "-convert", "binary1", infoPlistToConvert.absolutePath]
		commandRunnerMock.run(commandList).times(1)


		mockControl.play {
			xcodeBuildArchiveTask.archive()
		}
	}
}
