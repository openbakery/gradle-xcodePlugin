package org.openbakery

import org.apache.commons.configuration2.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.testdouble.SimulatorControlFake
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
		xcodeBuildArchiveTask.destinationResolver = new DestinationResolver(new SimulatorControlFake("simctl-list-xcode7.txt"))


		buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-iphoneos")
		buildOutputDirectory.mkdirs()


		appDirectory = TestHelper.createDummyApp(buildOutputDirectory, "Example")

		FileUtils.copyFileToDirectory(new File("../example/iOS/Example/Example/Example.entitlements"), new File(projectDir, "Example"))
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	void createFrameworkLib(String item) {
		File lib = new File(appDirectory, "Frameworks/" + item)
		FileUtils.writeStringToFile(lib, "foo")
	}

	def createSwiftLibs(Xcodebuild xcodebuild, boolean includeWatchLib = false, String platform = "iphoneos") {
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

		if (includeWatchLib) {
			swiftLibs.add(0, "libswiftWatchKit.dylib")
		}

		File swiftLibsDirectory = new File(xcodebuild.getToolchainDirectory(),  "usr/lib/swift/$platform")
		swiftLibsDirectory.mkdirs()

		swiftLibs.each { item ->
			File lib = new File(swiftLibsDirectory, item)
			FileUtils.writeStringToFile(lib, "bar")
		}

		return swiftLibs
	}

	void mockSwiftLibs(Xcodebuild xcodebuild, File watchAppDirectory = null) {
		def includeWatchLibs = watchAppDirectory != null

		def swiftLibs = createSwiftLibs(xcodebuild, includeWatchLibs)
		def libFiles = swiftLibs[0..4].collect { new File(appDirectory, "Frameworks/" + it) }

		if (includeWatchLibs) {
			def watchSwiftLibs = createSwiftLibs(xcodebuild, true, "watchos")
            libFiles += watchSwiftLibs[0..4].collect { new File(watchAppDirectory, "Frameworks/" + it) }
		}

		libFiles.forEach { FileUtils.writeStringToFile(it, "foo") }
	}

	void createWatchKitStub(String platformDirectory) {
		File stubDirectory = new File(platformDirectory, "/Developer/SDKs/iPhoneOS.sdk/Library/Application Support/WatchKit")
		stubDirectory.mkdirs()
		File stub = new File(stubDirectory, "WK")
		FileUtils.writeStringToFile(stub, "fixture")
	}

	List<String> bitcodeStripCommand(File dylib, Xcodebuild xcodebuild, String platform = "iphoneos") {
		return [
            "/usr/bin/xcrun",
            "bitcode_strip",
            "${xcodebuild.toolchainDirectory}/usr/lib/swift/${platform}/${dylib.name}",
            "-r",
            "-o",
            dylib.absolutePath
		]
	}

	Xcodebuild createXcodeBuild(String version) {
		XcodeFake xcode = createXcode(version)
		commandRunner.runWithResult(_, ["xcodebuild", "clean", "-showBuildSettings"]) >> "  TOOLCHAIN_DIR = " + xcode.path + "/Contents/Developer/Toolchains/Swift_2.3.xctoolchain\nPLATFORM_DIR = " + xcode.path + "/Contents/Developer/Platforms/iPhoneOS.platform\n"
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

		File infoPlist = new File("../example/iOS/Example/Example/Example-Info.plist")
		File destinationInfoPlist = new File(projectDir, "Example/Info.plist")
		FileUtils.copyFile(infoPlist, destinationInfoPlist)

		xcodeBuildArchiveTask.plistHelper.setValueForPlist(destinationInfoPlist, "CFBundleIdentifier", "org.openbakery.gxp.Example")

		project.xcodebuild.target = "Example"
		project.xcodebuild.configuration = "Debug"
		XcodeProjectFile xcodeProjectFile = new XcodeProjectFile(project, new File("../example/iOS/Example/Example.xcodeproj/project.pbxproj"))
		xcodeProjectFile.parse()
		project.xcodebuild.projectSettings = xcodeProjectFile.getProjectSettings()
	}

	def setupProjectWithWatchApp(String name, String watchOSPlatformDirectory) {
        // stub WatchKit
		File stubDirectory = new File(watchOSPlatformDirectory, "Developer/SDKs/WatchOS.sdk/Library/Application Support/WatchKit")
		stubDirectory.mkdirs()
		File stub = new File(stubDirectory, "WK")
		FileUtils.writeStringToFile(stub, "fixture")

		setupProject()

		File appDirectory = new File(appDirectory, "Watch/${name}.app")
		appDirectory.mkdirs()

		File watchInfoPlist = new File("../example/iOS/Example/Example WatchKit Extension/Info.plist")
		File watchDestinationInfoPlist = new File(appDirectory, "Info.plist")
		FileUtils.copyFile(watchInfoPlist, watchDestinationInfoPlist)

        File framework = new File(appDirectory, "PlugIns/Watch.appex/Frameworks/Library.framework")
        framework.mkdirs()

        File binary = new File(framework,"Binary")
        FileUtils.writeStringToFile(binary, "bar")

		return appDirectory
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

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration()
		config.read(new FileReader(infoPlist))
		List icons = config.getList("ApplicationProperties.IconPaths")

		then:
		infoPlist.exists()
		config.getString("ApplicationProperties.ApplicationPath") == "Applications/Example.app"
		config.getString("ApplicationProperties.CFBundleIdentifier") == "org.openbakery.gxp.Example"
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
		xcodeBuildArchiveTask.commandRunner = new CommandRunner()
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


	def "copy swift framework with bitcode enabled"() {
		given:
		Xcodebuild xcodebuild = createXcodeBuild("7")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode
		mockSwiftLibs(xcodebuild)
		project.xcodebuild.bitcode = true

		File libswiftCore = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		File supportLibswiftDirectory = new File(projectDir, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")
		List<String> bitcodeStrip = bitcodeStripCommand(libswiftCore, xcodebuild)

		when:
		xcodeBuildArchiveTask.archive()

		then:
		libswiftCore.exists()
		supportLibswiftDirectory.list().length == 5
		supportLibswiftCore.exists()
		FileUtils.readFileToString(supportLibswiftCore).equals("bar")
		0 * commandRunner.run(bitcodeStrip)
	}

	def "copy swift framework with bitcode disabled"() {
		given:
		Xcodebuild xcodebuild = createXcodeBuild("9")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode
		mockSwiftLibs(xcodebuild)
		project.xcodebuild.bitcode = false

		File libswiftCore = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		File supportLibswiftDirectory = new File(projectDir, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")
		List<String> bitcodeStrip = bitcodeStripCommand(libswiftCore, xcodebuild)

		when:
		xcodeBuildArchiveTask.archive()

		then:
		libswiftCore.exists()
		supportLibswiftDirectory.list().length == 5
		supportLibswiftCore.exists()
		FileUtils.readFileToString(supportLibswiftCore).equals("bar")
		1 * commandRunner.run(bitcodeStrip)
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

		entitlements.text.contains("<key>application-identifier</key>\n\t<string>UNKNOWN00ID.org.openbakery.gxp.Example</string>")
		entitlements.text.contains("<array>\n\t\t<string>UNKNOWN00ID.org.openbakery.gxp.Example</string>")
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

		entitlements.text.contains("<key>application-identifier</key>\n\t<string>AAAAAAAAAAA.org.openbakery.gxp.Example</string>")
		entitlements.text.contains("<array>\n\t\t<string>AAAAAAAAAAA.org.openbakery.gxp.Example</string>")
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
		String command

		given:
		xcodeBuildArchiveTask.destinationResolver = new DestinationResolver(new SimulatorControlFake("simctl-list-xcode7.txt"))

		setupProject()

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.type = Type.iOS

		project.xcodebuild.useXcodebuildArchive = true


		when:
		xcodeBuildArchiveTask.archive()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith("xcodebuild -scheme myscheme -workspace myworkspace")
		command.contains("-configuration Debug")
		command.contains("CODE_SIGN_IDENTITY=")
		command.contains("CODE_SIGNING_REQUIRED=NO")
		command.contains("CODE_SIGNING_ALLOWED=NO")
		command.contains("-derivedDataPath " + new File(project.buildDir, "derivedData").absolutePath)
		command.contains("DSTROOT=" + new File(project.buildDir, "dst").absolutePath)
		command.contains("OBJROOT=" + new File(project.buildDir, "obj").absolutePath)
		command.contains("SYMROOT=" + new File(project.buildDir, "sym").absolutePath)
		command.contains("SHARED_PRECOMPS_DIR=" + new File(project.buildDir, "shared").absolutePath)
		command.endsWith("archive -archivePath " + new File(project.buildDir, "archive/Example.xcarchive").absolutePath)
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
		commandRunner.runWithResult(_, ["xcodebuild", "clean", "-showBuildSettings"]) >> "  PLATFORM_DIR = " + xcode.path + "/Contents/Developer/Platforms/iPhoneOS.platform\n"
		Xcodebuild xcodebuild = new Xcodebuild(new File("."), commandRunner, xcode, new XcodebuildParameters(), [])
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode

		File stubDirectory = new File(xcodebuild.platformDirectory, "Library/Application Support/MessagesApplicationExtensionStub")
		stubDirectory.mkdirs()
		File stub = new File(stubDirectory, "MessagesApplicationExtensionStub")
		FileUtils.writeStringToFile(stub, "fixture")

		setupProject()

		File extensionDirectory = new File(buildOutputDirectory, "Example.app/PlugIns/ExampleStickerPack.appex")
		extensionDirectory.mkdirs()
		File infoPlist = new File("../example/iOS/Example/ExampleStickerPack/Info.plist")
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

	def "copy watchkit support folder"() {
        given:
		Xcodebuild xcodebuild = createXcodeBuild("9")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode

		setupProjectWithWatchApp("Example", xcodebuild.watchOSPlatformDirectory)

		when:
		xcodeBuildArchiveTask.archive()

		File supportDirectory = new File(projectDir, "build/archive/Example.xcarchive/WatchKitSupport2")
		File supportStub = new File(supportDirectory, 'WK')

		then:
		supportDirectory.exists()
		supportDirectory.list().length == 1
		supportStub.exists()
	}

	def "copy watchkit swift framework"() {
		given:
		Xcodebuild xcodebuild = createXcodeBuild("9")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode

		project.xcodebuild.bitcode = false

		def watchAppDirectory = setupProjectWithWatchApp("Example", xcodebuild.watchOSPlatformDirectory)
		mockSwiftLibs(xcodebuild, watchAppDirectory)

		File watchosLibswiftWatchKit = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Watch/Example.app/Frameworks/libswiftWatchKit.dylib")
		File watchosSupportLibswiftDirectory = new File(projectDir, "build/archive/Example.xcarchive/SwiftSupport/watchos")
		File watchosSupportLibswiftWatchKit = new File(watchosSupportLibswiftDirectory, "libswiftWatchKit.dylib")
		List<String> watchosBitcodeStrip = bitcodeStripCommand(watchosLibswiftWatchKit, xcodebuild, "watchos")

		File iphoneosLibswiftWatchKit = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftWatchKit.dylib")
		File iphoneosSupportLibswiftDirectory = new File(projectDir, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File iphoneosSupportLibswiftWatchKit = new File(iphoneosSupportLibswiftDirectory, "libswiftWatchKit.dylib")
		List<String> iphoneosBitcodeStrip = bitcodeStripCommand(iphoneosLibswiftWatchKit, xcodebuild)

		when:
		xcodeBuildArchiveTask.archive()

		then:
		watchosLibswiftWatchKit.exists()
		watchosSupportLibswiftDirectory.list().length == 5
		watchosSupportLibswiftWatchKit.exists()
		FileUtils.readFileToString(watchosSupportLibswiftWatchKit).equals("bar")
		0 * commandRunner.run(watchosBitcodeStrip)

		iphoneosLibswiftWatchKit.exists()
		iphoneosSupportLibswiftDirectory.list().length == 5
		iphoneosSupportLibswiftWatchKit.exists()
		FileUtils.readFileToString(iphoneosSupportLibswiftWatchKit).equals("bar")
		1 * commandRunner.run(iphoneosBitcodeStrip)
	}

	def "copy frameworks into watch appex"() {
		given:
		Xcodebuild xcodebuild = createXcodeBuild("9")
		xcodeBuildArchiveTask.xcode = xcodebuild.xcode
		project.xcodebuild.bitcode = false

        def watchosBuildDir = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-watchos")
        TestHelper.createWatchOSOutput(watchosBuildDir, "Watch")

		setupProjectWithWatchApp("Example", xcodebuild.watchOSPlatformDirectory)

		when:
		xcodeBuildArchiveTask.archive()

		then:
        File frameworkPath = new File(projectDir, "build/archive/Example.xcarchive/Products/Applications/Example.app/Watch/Example.app/Plugins/Watch.appex/Frameworks/Library.framework")
		File binaryPath = new File(frameworkPath, "Binary")

        binaryPath.exists()
        FileUtils.readFileToString(binaryPath).equals("foo")
        !new File(frameworkPath, "Headers").exists()
        !new File(frameworkPath, "Modules").exists()
	}
}
