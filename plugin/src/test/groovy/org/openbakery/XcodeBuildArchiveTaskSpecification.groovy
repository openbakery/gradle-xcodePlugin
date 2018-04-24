package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.RandomStringUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Xcodebuild
import org.openbakery.xcode.XcodebuildParameters
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class XcodeBuildArchiveTaskSpecification extends Specification {

	Project project

	XcodeBuildArchiveTask xcodeBuildArchiveTask

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
		project.apply plugin: XcodePlugin
		project.xcodebuild.infoPlist = 'Info.plist'
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.iOS
		project.xcodebuild.simulator = false
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"
		project.xcodebuild.signing.identity = "my identity"


		xcodeBuildArchiveTask = project.getTasks().getByPath(XcodePlugin.ARCHIVE_TASK_NAME)
		xcodeBuildArchiveTask.plistHelper = plistHelper
		xcodeBuildArchiveTask.commandRunner = commandRunner
		xcodeBuildArchiveTask.xcode.commandRunner = commandRunner
		xcodeBuildArchiveTask.destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode7.txt"))


		buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-iphoneos")
		buildOutputDirectory.mkdirs()


		appDirectory = TestHelper.createDummyApp(buildOutputDirectory, "Example")
		FileUtils.copyFileToDirectory(new File("../example/iOS/ExampleWatchkit/ExampleWatchkit/ExampleWatchkit.entitlements"), new File(projectDir, "ExampleWatchkit"))
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	void createFrameworkLib(String item) {
		File lib = new File(appDirectory, "Frameworks/" + item)
		FileUtils.writeStringToFile(lib, "foo")
	}

	def createSwiftLibs(Xcodebuild xcodebuild) {
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

		File swiftLibsDirectory = new File(xcodebuild.getToolchainDirectory(),  "usr/lib/swift/iphoneos")
		swiftLibsDirectory.mkdirs()

		swiftLibs.each { item ->
			File lib = new File(swiftLibsDirectory, item)
			FileUtils.writeStringToFile(lib, "bar")
		}
		return swiftLibs
	}

	void mockSwiftLibs(Xcodebuild xcodebuild) {
		def swiftLibs = createSwiftLibs(xcodebuild)
		swiftLibs[0..4].each { item ->
			File lib = new File(appDirectory, "Frameworks/" + item)
			FileUtils.writeStringToFile(lib, "foo")
		}
	}

	Xcodebuild createXcodeBuild(String version) {
		XcodeFake xcode = createXcode(version)
		commandRunner.runWithResult(_, ["xcodebuild", "clean", "-showBuildSettings"]) >> "  TOOLCHAIN_DIR = " + xcode.path + "/Contents/Developer/Toolchains/Swift_2.3.xctoolchain\n"
		return new Xcodebuild(new File("."), commandRunner, xcode, new XcodebuildParameters(), [])
	}


	Xcode createXcode(String version) {
		XcodeFake xcode = new XcodeFake()
		File xcodePath = new File(projectDir, "Xcode" + version + ".app")
		new File(xcodePath, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodePath, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		xcode.versionString = version
		xcode.path = xcodePath.absolutePath
		return xcode
	}

	def setupProject() {
		CommandRunner commandRunner = new CommandRunner()
		commandRunner.defaultBaseDirectory = projectDir.absolutePath
		xcodeBuildArchiveTask.plistHelper = new PlistHelper(commandRunner)
		project.xcodebuild.plistHelper = xcodeBuildArchiveTask.plistHelper

		File infoPlist = new File("../example/iOS/ExampleWatchkit/ExampleWatchkit/Info.plist")
		File destinationInfoPlist = new File(projectDir, "ExampleWatchkit/Info.plist")
		FileUtils.copyFile(infoPlist, destinationInfoPlist)

		xcodeBuildArchiveTask.plistHelper.setValueForPlist(destinationInfoPlist, "CFBundleIdentifier", "org.openbakery.test.ExampleWatchkit")

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
		dependsOn.size() == 2

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

	def "copy OnDemandResources"() {
		when:
		TestHelper.createOnDemandResources(appDirectory)
		xcodeBuildArchiveTask.archive()

		File onDemandResourcesDirectory = new File(projectDir, "build/archive/Example.xcarchive/Products/OnDemandResources")
		File infoPlist_onDemandResourcesDirectory = new File(projectDir, "build/archive/Example.xcarchive/Products/OnDemandResources/org.openbakery.test.Example.SampleImages.assetpack/Info.plist")

		then:
		onDemandResourcesDirectory.exists()
		infoPlist_onDemandResourcesDirectory.exists()
	}

	def "copy OnDemandResources.plist"() {
		when:
		TestHelper.createOnDemandResources(appDirectory)
		xcodeBuildArchiveTask.archive()

		File onDemandResourcesPlist = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/OnDemandResources.plist")

		then:
		onDemandResourcesPlist.exists()
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

	def copyFrameworkDsmys() {
		given:
		File extensionDirectory = new File(buildOutputDirectory, "OBInjector/OBInjector.framework.dSYM")
		extensionDirectory.mkdirs()

		when:
		xcodeBuildArchiveTask.archive()

		File dsymFile = new File(projectDir, "build/archive/Example.xcarchive/dSYMs/OBInjector.framework.dSYM")

		then:
		dsymFile.exists()

	}


	def createInfoPlist() {
		given:
		xcodeBuildArchiveTask.plistHelper = new PlistHelper(new CommandRunner())

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"

		when:
		xcodeBuildArchiveTask.archive()

		File infoPlist = new File(projectDir, "build/archive/Example.xcarchive/Info.plist")

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(infoPlist)
		List icons = config.getList("ApplicationProperties.IconPaths")

		then:
		infoPlist.exists()
		config.getString("ApplicationProperties.ApplicationPath") == "Applications/Example.app"
		config.getString("ApplicationProperties.CFBundleIdentifier") == "org.openbakery.test.Example"
		config.getString("ApplicationProperties.CFBundleShortVersionString") == "1.0"
		config.getString("ApplicationProperties.CFBundleVersion") == "1.0"
		config.getString("ApplicationProperties.SigningIdentity") == "iPhone Developer: Firstname Surename (AAAAAAAAAA)"

		icons.size() == 2
		icons.contains("Applications/Example.app/Icon-72.png")
		icons.contains("Applications/Example.app/Icon.png")
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

		File zipFile = new File(projectDir, "build/archive/Example.zip")
		ZipFile zip = new ZipFile(zipFile)
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
		Xcodebuild xcodebuild = createXcodeBuild("6")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode
		mockSwiftLibs(xcodebuild)

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

	def "copy swift framework"() {
		given:
		Xcodebuild xcodebuild = createXcodeBuild("7")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode
		mockSwiftLibs(xcodebuild)

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


	def "copy swift with non default toolchain"() {
		given:
		XcodeFake xcode = createXcode("7")
		commandRunner.runWithResult(_, ["xcodebuild", "clean", "-showBuildSettings"]) >> "  TOOLCHAIN_DIR = " + xcode.path + "/Contents/Developer/Toolchains/Swift.xctoolchain\n"
		Xcodebuild xcodebuild =  new Xcodebuild(new File("."), commandRunner, xcode, new XcodebuildParameters(), [])
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode
		mockSwiftLibs(xcodebuild)


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

		Xcodebuild xcodebuild = createXcodeBuild("7")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode
		createSwiftLibs(xcodebuild)
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
		plistHelper.setValueForPlist(infoPlist, "CFBundleIdentifier", "")
		plistHelper.setValueForPlist(infoPlist, "CFBundleShortVersionString", "")
		plistHelper.setValueForPlist(infoPlist, "CFBundleVersion", "")

		File infoPlistToConvert = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Info.plist")

		when:
		xcodeBuildArchiveTask.archive()

		then:
		1 * commandRunner.run(["/usr/bin/plutil", "-convert", "binary1", infoPlistToConvert.absolutePath])
	}



	def "convert InfoPlist to binary with error"() {
		given:

		File infoPlist = new File(appDirectory, "Info.plist")

		plistHelper.setValueForPlist(infoPlist, "CFBundleIdentifier", "")
		plistHelper.setValueForPlist(infoPlist, "CFBundleShortVersionString", "")
		plistHelper.setValueForPlist(infoPlist, "CFBundleVersion", "")

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
		xcodeBuildArchiveTask.parameters = project.xcodebuild.xcodebuildParameters
		File bundle = new File(xcodeBuildArchiveTask.parameters.outputPath, "ExampleWatchkit.app/Watch/ExampleWatchkit WatchKit App.app")

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

		entitlements.text.contains("<key>application-identifier</key>\n\t<string>UNKNOWN00ID.org.openbakery.test.Example</string>")
		entitlements.text.contains("<array>\n\t\t<string>UNKNOWN00ID.org.openbakery.test.Example</string>")
	}

	def "copy entitlements if present"() {
		given:
		project.xcodebuild.signing.addMobileProvisionFile( new File("src/test/Resource/openbakery.mobileprovision") )
		setupProject()

		when:
		xcodeBuildArchiveTask.archive()

		File entitlements = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/archived-expanded-entitlements.xcent")

		then:
		entitlements.exists()
		entitlements.isFile()

		entitlements.text.contains("<key>application-identifier</key>\n\t<string>AAAAAAAAAAA.org.openbakery.test.Example</string>")
		entitlements.text.contains("<array>\n\t\t<string>AAAAAAAAAAA.org.openbakery.test.Example</string>")
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

		Xcodebuild xcodebuild = createXcodeBuild("7")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode
		createSwiftLibs(xcodebuild)

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


	def "copy all bcsymbolmap to BCSymbolMaps directory"() {
		given:
		project.xcodebuild.bitcode = true
		setupProject()

		when:
		xcodeBuildArchiveTask.archive()

		then:
		new File(projectDir, "build/archive/Example.xcarchive/BCSymbolMaps").exists()
		new File(projectDir, "build/archive/Example.xcarchive/BCSymbolMaps/14C60358-AC0B-35CF-A079-042050D404EE.bcsymbolmap").exists()
		new File(projectDir, "build/archive/Example.xcarchive/BCSymbolMaps/2154C009-2AC2-3241-9E2E-D8B8046B03C8.bcsymbolmap").exists()
		new File(projectDir, "build/archive/Example.xcarchive/BCSymbolMaps/23CFBC47-4B7D-391C-AB95-48408893A14A.bcsymbolmap").exists()
	}

	def "do not copy bcsymbolmap if build is not bitcode build"() {
		given:
		setupProject()
		project.xcodebuild.bitcode = false

		when:
		xcodeBuildArchiveTask.archive()

		then:
		!new File(projectDir, "build/archive/Example.xcarchive/BCSymbolMaps").exists()

	}

	def "copy message extension support folder"() {
		given:
		XcodeFake xcode = createXcode("8")
		commandRunner.runWithResult(_, ["xcodebuild", "clean", "-showBuildSettings"]) >> "  PLATFORM_DIR = " + xcode.path + "Contents/Developer/Platforms/iPhoneOS.platform\n"
		Xcodebuild xcodebuild = new Xcodebuild(new File("."), commandRunner, xcode, new XcodebuildParameters(), [])
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode

		File stubDirectory = new File(xcodebuild.platformDirectory, "Library/Application Support/MessagesApplicationExtensionStub")
		stubDirectory.mkdirs()
		File stub = new File(stubDirectory, "MessagesApplicationExtensionStub")
		FileUtils.writeStringToFile(stub, "fixture")

		setupProject()

		File extensionDirectory = new File(buildOutputDirectory, "Example.app/PlugIns/ExampleWatchKit Sticker Pack.appex")
		extensionDirectory.mkdirs()
		File infoPlist = new File("../example/iOS/ExampleWatchkit/ExampleWatchKit Sticker Pack/Info.plist")
		File destinationInfoPlist = new File(extensionDirectory, "Info.plist")
		FileUtils.copyFile(infoPlist, destinationInfoPlist)

		when:
		xcodeBuildArchiveTask.archive()

		File supportMessagesDirectory = new File(projectDir, "build/archive/Example.xcarchive/MessagesApplicationExtensionSupport")
        File supportMessagesStub = new File(supportMessagesDirectory, 'MessagesApplicationExtensionStub')

		then:
		supportMessagesDirectory.exists()
		supportMessagesDirectory.list().length == 1
		supportMessagesStub.exists()
	}
}
