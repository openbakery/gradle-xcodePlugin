package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.RandomStringUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.stubs.PlistHelperStub
import org.openbakery.stubs.SimulatorControlStub
import org.openbakery.tools.DestinationResolver
import org.openbakery.tools.Xcodebuild
import org.openbakery.util.PlistHelper
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by rene on 01.12.14.
 */
class XcodeBuildArchiveTaskSpecification extends Specification {

	Project project

	XcodeBuildArchiveTask xcodeBuildArchiveTask;

	File projectDir
	File buildOutputDirectory
	File appDirectory

	CommandRunner commandRunner = Mock(CommandRunner)
	PlistHelperStub plistHelper = new PlistHelperStub()

	def setup() {

		String tmpName =  "gradle-xcodebuild-" + RandomStringUtils.randomAlphanumeric(5)
		projectDir = new File(System.getProperty("java.io.tmpdir"), tmpName)
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.infoPlist = 'Info.plist'
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.iOS
		project.xcodebuild.simulator = false
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		xcodeBuildArchiveTask = project.getTasks().getByPath(XcodePlugin.ARCHIVE_TASK_NAME)
		xcodeBuildArchiveTask.plistHelper = plistHelper
		xcodeBuildArchiveTask.commandRunner = commandRunner
		xcodeBuildArchiveTask.xcode.commandRunner = commandRunner

		buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-iphoneos")
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


		FileUtils.copyFileToDirectory(new File("../example/iOS/ExampleWatchkit/ExampleWatchkit/ExampleWatchkit.entitlements"), new File(projectDir, "ExampleWatchkit"))
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	void createFrameworkLib(String item) {
		File lib = new File(appDirectory, "Frameworks/" + item)
		FileUtils.writeStringToFile(lib, "foo")
	}

	def createSwiftLibs(String xcodepath) {
		def swiftLibs = [
						"libswiftCore.dylib",
						"libswiftCoreGraphics.dylib",
						"libswiftDarwin.dylib",
						"libswiftDispatch.dylib",
						"libswiftFoundation.dylib",
						"libswiftObjectiveC.dylib",
						"libswiftSecurity.dylib",
						"libswiftUIKit.dylib"
		]

		File swiftLibsDirectory = new File(xcodepath + "/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphoneos")
		swiftLibsDirectory.mkdirs();

		swiftLibs.each { item ->
			File lib = new File(swiftLibsDirectory, item)
			FileUtils.writeStringToFile(lib, "bar")
		}
		return swiftLibs
	}

	void mockSwiftLibs(String xcodepath) {
		def swiftLibs = createSwiftLibs(xcodepath)
		swiftLibs[0..4].each { item ->
			File lib = new File(appDirectory, "Frameworks/" + item)
			FileUtils.writeStringToFile(lib, "foo")
		}

	}

	def createXcode(String version) {
		File xcode = new File(projectDir, "Xcode" + version + ".app")
		new File(xcode, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		commandRunner.runWithResult(xcode.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode "+ version + "\nBuild version ABCDE"
		return xcode
	}

	def mockXcodeVersion(String version) {
		project.xcodebuild.commandRunner = commandRunner

		File xcode = createXcode(version)
		commandRunner.runWithResult("xcode-select", "-p") >> (xcode.absolutePath + "/Contents/Developer")

		xcodeBuildArchiveTask.xcode.version = new Version(version)
		return xcode.absolutePath
	}

	def setupProject() {
		CommandRunner commandRunner = new CommandRunner()
		commandRunner.defaultBaseDirectory = projectDir.absolutePath
		xcodeBuildArchiveTask.plistHelper = new PlistHelper(project, commandRunner);
		project.xcodebuild.plistHelper = xcodeBuildArchiveTask.plistHelper

		File infoPlist = new File("../example/iOS/ExampleWatchkit/ExampleWatchkit/Info.plist")
		FileUtils.copyFile(infoPlist, new File(projectDir, "ExampleWatchkit/Info.plist"))

		project.xcodebuild.target = "ExampleWatchkit"
		project.xcodebuild.configuration = "Debug"
		XcodeProjectFile xcodeProjectFile = new XcodeProjectFile(project, new File("../example/iOS/ExampleWatchkit/ExampleWatchkit.xcodeproj/project.pbxproj"))
		xcodeProjectFile.parse()
		project.xcodebuild.projectSettings = xcodeProjectFile.getProjectSettings()

	}


	def "archiveDirectory"() {
		when:
		xcodeBuildArchiveTask.archive()

		File archiveDirectory = new File(projectDir, "build/archive/Example.xcarchive")

		then:
		archiveDirectory.exists()
		archiveDirectory.isDirectory()
	}

	def "depends on"() {
		when:
		def dependsOn = xcodeBuildArchiveTask.getDependsOn()
		then:
		dependsOn.size() == 3

		dependsOn.contains(XcodePlugin.XCODE_BUILD_TASK_NAME)
		dependsOn.contains(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)

	}

	def "archive directory with BundleSuffix"() {
		given:
		project.xcodebuild.bundleNameSuffix = "-1.2.3"

		when:
		xcodeBuildArchiveTask.archive()

		File archiveDirectory = new File(projectDir, "build/archive/Example-1.2.3.xcarchive")

		then:
		archiveDirectory.exists()
		archiveDirectory.isDirectory()
	}


	def applicationsFolder() {
		when:
		xcodeBuildArchiveTask.archive()
		File applicationsDirectory = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications")

		then:
		applicationsDirectory.exists()
	}


	def "copy App"() {
		when:
		xcodeBuildArchiveTask.archive()

		File appFile = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Example")

		then:
		appFile.exists()
	}

	def "copy Dsym"() {
		when:
		xcodeBuildArchiveTask.archive()

		File dsymFile = new File(projectDir, "build/archive/Example.xcarchive/dSYMs/Example.app.dSYM")

		then:
		dsymFile.exists()
	}


	def copyMultipleDsyms() {
		given:
		File extensionDirectory = new File(buildOutputDirectory, "Example.app/PlugIns/ExampleTodayWidget.appex")
		extensionDirectory.mkdirs()

		File dSymDirectory = new File(buildOutputDirectory, "ExampleTodayWidget.appex.dSYM")
		dSymDirectory.mkdirs()

		when:
		xcodeBuildArchiveTask.archive()

		then:
		new File(projectDir, "build/archive/Example.xcarchive/dSYMs/ExampleTodayWidget.appex.dSYM").exists()
		new File(projectDir, "build/archive/Example.xcarchive/dSYMs/Example.app.dSYM").exists()
	}


	def createInfoPlist() {
		given:
		xcodeBuildArchiveTask.plistHelper = new PlistHelper(project, new CommandRunner())

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"

		when:
		xcodeBuildArchiveTask.archive()

		File infoPlist = new File(projectDir, "build/archive/Example.xcarchive/Info.plist")

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(infoPlist)
		List icons = config.getList("ApplicationProperties.IconPaths");

		then:
		infoPlist.exists()
		config.getString("ApplicationProperties.ApplicationPath") == "Applications/Example.app"
		config.getString("ApplicationProperties.CFBundleIdentifier") == "org.openbakery.Example"
		config.getString("ApplicationProperties.CFBundleShortVersionString") == "1.0"
		config.getString("ApplicationProperties.CFBundleVersion") == "1.0"
		config.getString("ApplicationProperties.SigningIdentity") == "iPhone Developer: Firstname Surename (AAAAAAAAAA)"

		icons.size() == 2
		icons.get(0).equals("Applications/Example.app/Icon-72.png")
		icons.get(1).equals("Applications/Example.app/Icon.png")
		config.getString("Name").equals("Example")
		config.getString("SchemeName").equals("Example")

	}

	def "Zip for simulator build"() {
		given:
		project.xcodebuild.simulator = true
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-iphonesimulator")
		buildOutputDirectory.mkdirs()

		File appDirectory = new File(buildOutputDirectory, "Example.app")
		appDirectory.mkdirs()
		File app = new File(appDirectory, "Example")
		FileUtils.writeStringToFile(app, "dummy")

		when:
		xcodeBuildArchiveTask.archive()

		File zipFile = new File(projectDir, "build/archive/Example.zip");
		ZipFile zip = new ZipFile(zipFile);
		List<String> entries = new ArrayList<String>()
		for (ZipEntry entry : zip.entries()) {
			entries.add(entry.getName())
		}

		then:
		zipFile.exists()
		entries.contains("Example.app/Example")

	}


	def "swift framework in App Xcode 6"() {
		given:
		String xcodepath = mockXcodeVersion("6")
		mockSwiftLibs(xcodepath)

		when:
		xcodeBuildArchiveTask.archive()

		File libswiftCore = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		File supportLibswiftDirectory = new File(projectDir, "build/archive/Example.xcarchive/SwiftSupport")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")

		then:
		libswiftCore.exists()
		supportLibswiftDirectory.list().length == 5
		supportLibswiftCore.exists()
		FileUtils.readFileToString(supportLibswiftCore).equals("bar")
	}

	def "swift framework in App Xcode 7"() {
		given:

		String xcodepath = mockXcodeVersion("7")
		mockSwiftLibs(xcodepath)

		when:
		xcodeBuildArchiveTask.archive()

		File libswiftCore = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		File supportLibswiftDirectory = new File(projectDir, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")

		then:
		libswiftCore.exists()
		supportLibswiftDirectory.list().length == 5
		supportLibswiftCore.exists()
		FileUtils.readFileToString(supportLibswiftCore).equals("bar")
	}


	def "no swift but framework in App Xcode 7"() {
		given:

		String xcodepath = mockXcodeVersion("7")
		createSwiftLibs(xcodepath)
		createFrameworkLib("myFramework.dylib")

		when:
		xcodeBuildArchiveTask.archive()

		File myFramework = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/myFramework.dylib")
		File supportLibswiftDirectory = new File(projectDir, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")

		then:
		myFramework.exists()
		!supportLibswiftDirectory.exists()
		!supportLibswiftCore.exists()
	}

	def "convert Info Plist to binary"() {
		given:

		File infoPlist = new File(appDirectory, "Info.plist")
		plistHelper.setValueForPlist(infoPlist, "CFBundleIdentifier", "");
		plistHelper.setValueForPlist(infoPlist, "CFBundleShortVersionString", "");
		plistHelper.setValueForPlist(infoPlist, "CFBundleVersion", "");

		File infoPlistToConvert = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Info.plist")

		when:
		xcodeBuildArchiveTask.archive()

		then:
		1 * commandRunner.run(["/usr/bin/plutil", "-convert", "binary1", infoPlistToConvert.absolutePath])
	}



	def "convert InfoPlist to binary with error"() {
		given:

		File infoPlist = new File(appDirectory, "Info.plist")

		plistHelper.setValueForPlist(infoPlist, "CFBundleIdentifier", "");
		plistHelper.setValueForPlist(infoPlist, "CFBundleShortVersionString", "");
		plistHelper.setValueForPlist(infoPlist, "CFBundleVersion", "");

		File infoPlistToConvert = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Info.plist")

		def commandList = ["/usr/bin/plutil", "-convert", "binary1", infoPlistToConvert.absolutePath]
		commandRunner.run(commandList) >> { throw new CommandRunnerException("Permission Denied!") }

		when:
		xcodeBuildArchiveTask.archive()

		then:
		true
		// should not fail!
	}


	def "application directory"() {

		when:
		File applicationDirectory = xcodeBuildArchiveTask.getApplicationsDirectory()

		then:
		applicationDirectory == new File(projectDir, "build/archive/Example.xcarchive/Products/Applications")

	}


	def "bundle destination directory"() {
		given:
		File bundle = new File(project.xcodebuild.outputPath, "ExampleWatchkit.app/Watch/ExampleWatchkit WatchKit App.app")

		when:
		File applicationDirectory = xcodeBuildArchiveTask.getDestinationDirectoryForBundle(bundle)

		then:
		applicationDirectory == new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/ExampleWatchkit.app/Watch/ExampleWatchkit WatchKit App.app")
	}

	def "copy entitlements if present with default application identifier"() {

		given:
		setupProject()

		when:
		xcodeBuildArchiveTask.archive()

		File entitlements = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/archived-expanded-entitlements.xcent")

		then:
		entitlements.exists()
		entitlements.isFile()

		entitlements.text.contains("<key>application-identifier</key>\n\t<string>UNKNOWN00ID.org.openbakery.Example</string>")
		entitlements.text.contains("<array>\n\t\t<string>UNKNOWN00ID.org.openbakery.Example</string>")
	}

	def "copy entitlements if present"() {
		given:
		project.xcodebuild.signing.mobileProvisionFile = new File("src/test/Resource/openbakery.mobileprovision")
		setupProject()

		when:
		xcodeBuildArchiveTask.archive()

		File entitlements = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/archived-expanded-entitlements.xcent")

		then:
		entitlements.exists()
		entitlements.isFile()

		entitlements.text.contains("<key>application-identifier</key>\n\t<string>AAAAAAAAAAA.org.openbakery.Example</string>")
		entitlements.text.contains("<array>\n\t\t<string>AAAAAAAAAAA.org.openbakery.Example</string>")
	}

	def "copy entitlements but there are non"() {

		given:
		setupProject()

		when:
		xcodeBuildArchiveTask.archive()

		then:
		// should not thow an exception
		true

	}

	def "delete empty frameworks directory"() {
		given:
		commandRunner.runWithResult("xcode-select", "-p") >> ("/Applications/Xcode.app/Contents/Developer")
		setupProject()
		File frameworksDirectory = new File(appDirectory, "Frameworks")
		frameworksDirectory.mkdirs()

		when:
		xcodeBuildArchiveTask.archive()
		File archiveFrameworksDirectory = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks")

		then:
		!archiveFrameworksDirectory.exists()
	}


	def "delete frameworks directory in extension"() {
		given:
		setupProject()
		File extensionDirectory = new File(appDirectory, "PlugIns/ExampleTodayWidget.appex/Frameworks/MyFramework.framework")
		extensionDirectory.mkdirs()

		when:
		xcodeBuildArchiveTask.archive()
		File archiveFrameworksDirectory = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/PlugIns/ExampleTodayWidget.appex/Frameworks")

		then:
		!archiveFrameworksDirectory.exists()

	}


	def "create archive using xcodebuild"() {
		def commandList
		def expectedCommandList

		given:
		xcodeBuildArchiveTask.destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode7.txt"))

		setupProject()

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.type = Type.iOS

		project.xcodebuild.useXcodebuildArchive = true


		when:
		xcodeBuildArchiveTask.archive()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", "Debug",
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO",
														 "-derivedDataPath", new File(project.buildDir, "derivedData").absolutePath,
														 "DSTROOT=" + new File(project.buildDir, "dst").absolutePath,
														 "OBJROOT=" + new File(project.buildDir, "obj").absolutePath,
														 "SYMROOT=" + new File(project.buildDir, "sym").absolutePath,
														 "SHARED_PRECOMPS_DIR=" + new File(project.buildDir, "shared").absolutePath,
														 "archive",
														 "-archivePath",
														 new File(project.buildDir, "archive/Example.xcarchive").absolutePath

			]
		}
		commandList == expectedCommandList

	}
}
