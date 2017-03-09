package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Devices
import org.openbakery.xcode.Type
import spock.lang.Specification

class XcodeBuildPluginExtensionSpecification extends Specification {

	Project project
	File projectDir
	XcodeBuildPluginExtension extension;
	CommandRunner commandRunner = Mock(CommandRunner)

	File xcodebuild7_1_1
	File xcodebuild6_1
	File xcodebuild6_0
	File xcodebuild5_1


	File workspaceDirectory

	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		extension = new XcodeBuildPluginExtension(project)
		extension.commandRunner = commandRunner
		extension.infoPlist = "Info.plist";


		xcodebuild7_1_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode7.1.1.app")
		xcodebuild6_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode6-1.app")
		xcodebuild6_0 = new File(System.getProperty("java.io.tmpdir"), "Xcode6.app")
		xcodebuild5_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode5.app")

		new File(xcodebuild7_1_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild6_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild6_0, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild5_1, "Contents/Developer/usr/bin").mkdirs()

		new File(xcodebuild7_1_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcodebuild6_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcodebuild6_0, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcodebuild5_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()

	}

	def cleanup() {
		FileUtils.deleteDirectory(xcodebuild6_1)
		FileUtils.deleteDirectory(xcodebuild6_0)
		FileUtils.deleteDirectory(xcodebuild5_1)
		if (workspaceDirectory != null) {
			FileUtils.deleteDirectory(workspaceDirectory)
		}
		FileUtils.deleteDirectory(projectDir)
	}

	HashMap<String, BuildTargetConfiguration> createProjectSettings() {
		HashMap<String, BuildTargetConfiguration> result = new HashMap<>()

		BuildTargetConfiguration appConfiguration = new BuildTargetConfiguration()
		BuildConfiguration release = new BuildConfiguration("Example")
		BuildConfiguration debug = new BuildConfiguration("Example")
		appConfiguration.buildSettings["Release"] = release
		appConfiguration.buildSettings["Debug"] = debug

		debug.infoplist = "ExampleWatchkit/Info.plist"
		debug.bundleIdentifier = "org.openbakery.test.Example"
		debug.productName = "ExampleWatchkit"
		debug.sdkRoot = "iphoneos"
		release.infoplist = "ExampleWatchkit/Info.plist"
		release.bundleIdentifier = "org.openbakery.test.Example"
		release.productName = "ExampleWatchkit"
		release.sdkRoot = "iphoneos"
		release.devices = Devices.UNIVERSAL


		BuildTargetConfiguration watchAppConfiguration = new BuildTargetConfiguration()
		BuildConfiguration watchAppConfigurationRelease = new BuildConfiguration("ExampleWatchkit WatchKit App")
		BuildConfiguration watchAppConfigurationDebug = new BuildConfiguration("ExampleWatchkit WatchKit App")
		watchAppConfiguration.buildSettings["Release"] = watchAppConfigurationRelease
		watchAppConfiguration.buildSettings["Debug"] = watchAppConfigurationDebug

		watchAppConfigurationDebug.infoplist = "ExampleWatchkit WatchKit App/Info.plist"
		watchAppConfigurationDebug.bundleIdentifier = "org.openbakery.test.Example.watchkitapp"
		watchAppConfigurationDebug.productName = "ExampleWatchkit WatchKit App"
		watchAppConfigurationDebug.sdkRoot = "watchos"
		watchAppConfigurationRelease.infoplist = "ExampleWatchkit WatchKit App/Info.plist"
		watchAppConfigurationRelease.bundleIdentifier = "org.openbakery.test.Example.watchkitapp"
		watchAppConfigurationRelease.productName = "ExampleWatchkit WatchKit App"
		watchAppConfigurationRelease.sdkRoot = "watchos"
		watchAppConfigurationRelease.devices = Devices.WATCH

		BuildTargetConfiguration extensionConfiguration = new BuildTargetConfiguration()
		BuildConfiguration extenstionConfigurationRelease = new BuildConfiguration("ExampleWatchkit WatchKit Extension")
		BuildConfiguration extenstionConfigurationDebug = new BuildConfiguration("ExampleWatchkit WatchKit Extension")
		extensionConfiguration.buildSettings["Release"] = extenstionConfigurationRelease
		extensionConfiguration.buildSettings["Debug"] = extenstionConfigurationDebug

		extenstionConfigurationDebug.infoplist = "ExampleWatchkit WatchKit Extension/Info.plist"
		extenstionConfigurationDebug.bundleIdentifier = "org.openbakery.test.Example.watchkitapp.watchkitextension"
		extenstionConfigurationDebug.productName = "ExampleWatchkit WatchKit Extension"
		extenstionConfigurationDebug.sdkRoot = "watchos"
		extenstionConfigurationRelease.infoplist = "ExampleWatchkit WatchKit Extension/Info.plist"
		extenstionConfigurationRelease.bundleIdentifier = "org.openbakery.test.Example.watchkitapp.watchkitextension"
		extenstionConfigurationRelease.productName = "ExampleWatchkit WatchKit Extension"
		extenstionConfigurationRelease.sdkRoot = "watchos"
		extenstionConfigurationRelease.devices = Devices.WATCH


		result.put("ExampleWatchkit", appConfiguration)
		result.put("ExampleWatchkit WatchKit App", watchAppConfiguration)
		result.put("ExampleWatchkit WatchKit Extension", extensionConfiguration)

		return result
	}








	def "workspace nil"() {
		when:
		extension

		then:
		extension.workspace == null;
	}



	def "workspace"() {
		File workspace = new File(project.projectDir , "Test.xcworkspace")

		when:
		workspace.mkdirs()

		then:
		extension.workspace == "Test.xcworkspace";
	}



	def "application bundle for widget"() {
		when:
		File projectDir =  new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		XcodeProjectFile xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "Example.xcodeproj/project.pbxproj"))
		extension.projectSettings = xcodeProjectFile.getProjectSettings()
		extension.type = Type.iOS
		extension.simulator = false
		extension.target = "ExampleTodayWidget"
		extension.productName = "ExampleTodayWidget"
		extension.productType = "appex"
		extension.infoPlist = "../../example/iOS/Example/ExampleTodayWidget/Info.plist"


		String applicationBundle = extension.getApplicationBundle().absolutePath;

		then:
		applicationBundle.endsWith("build/sym/Debug-iphoneos/ExampleTodayWidget.appex")

	}

	def "application bundle"() {
		when:
		File projectDir =  new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.commandRunner = new CommandRunner()
		XcodeProjectFile xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "Example.xcodeproj/project.pbxproj"))
		extension.projectSettings = xcodeProjectFile.getProjectSettings()

		extension.type = Type.iOS
		extension.simulator = false
		extension.target = "Example"
		extension.productName = "Example"
		extension.infoPlist = "../../example/iOS/Example/Example/Example-Info.plist"

		String applicationBundle = extension.getApplicationBundle().absolutePath;

		then:
		applicationBundle.endsWith("build/sym/Debug-iphoneos/Example.app")

	}

	def "application bundle for watchos"() {
		when:

		File projectDir =  new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.projectSettings = createProjectSettings()
		extension.type = Type.iOS
		extension.target = "ExampleWatchkit WatchKit App"
		extension.simulator = false
		extension.productName = "ExampleWatchkit WatchKit App"
		extension.productType = "app"
		extension.infoPlist = "../../example/iOS/ExampleWatchkit/ExampleWatchkit WatchKit App/Info.plist"


		String applicationBundle = extension.getApplicationBundle().absolutePath;

		then:
		applicationBundle.endsWith("build/sym/Debug-iphoneos/ExampleWatchkit.app")

	}

	def "application bundle for watch find parent"() {
		when:
		File projectDir =  new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.projectSettings = createProjectSettings()
		extension.type = Type.iOS
		extension.target = "ExampleWatchkit WatchKit App"
		extension.simulator = false


		BuildConfiguration parent = extension.getParent(extension.projectSettings["ExampleWatchkit WatchKit App"].buildSettings["Debug"])

		then:
		parent.bundleIdentifier == "org.openbakery.test.Example"
	}



	void mockValueFromPlist(String key, String value) {
		PlistHelperStub plistHelperStub = new PlistHelperStub()
		File infoPlist = new File(project.projectDir, "Info.plist")
		plistHelperStub.setValueForPlist(infoPlist, key, value)
		extension.plistHelper = plistHelperStub
	}

	def "test Default Bundle Name Empty"() {
		extension.productName = "TestApp1"

		mockValueFromPlist("CFBundleName", "");

		when:
		String bundleName = extension.getBundleName();

		then:
		bundleName.equals("TestApp1")

	}


	def "test Default Bundle Name Value"() {
		mockValueFromPlist("CFBundleName", "TestApp2");

		when:
		String bundleName = extension.getBundleName();

		then:
		bundleName.equals("TestApp2")
	}




	def "os x has no simulator"() {

		when:
		extension.type = Type.macOS
		then:
		extension.simulator == false

	}

	Destination createDestination(String id, String name, String osVersion) {
		Destination destination = new Destination()
		destination.platform = "iOS Simulator"
		destination.name = name
		destination.arch = "i386"
		destination.id = id
		destination.os = osVersion
		return destination
	}






	def "get build configuration for bundle identifier"() {
		given:
		extension.projectSettings = createProjectSettings()

		when:
		def configuration = extension.getBuildConfiguration("org.openbakery.test.Example")

		then:
		configuration != null
		configuration.bundleIdentifier == 'org.openbakery.test.Example'
		configuration.infoplist == "ExampleWatchkit/Info.plist"

	}

	def "get build configuration for bundle identifier with missing info plist"() {
		given:
		HashMap<String, BuildTargetConfiguration> projectSettings = new HashMap<>()
		BuildTargetConfiguration buildTargetConfiguration = new BuildTargetConfiguration()
		BuildConfiguration release = new BuildConfiguration("ExampleWatchkit")
		buildTargetConfiguration.buildSettings["Debug"] = release
		projectSettings.put("ExampleWatchkit", buildTargetConfiguration)


		extension.projectSettings = projectSettings

		when:
		def configuration = extension.getBuildConfiguration("org.openbakery.test.Example")

		then:
		configuration == null
	}


	def "test simulator as string: true"() {
		when:
		extension.type = Type.iOS;
		extension.simulator = 'true'
		then:
		extension.simulator == true
	}

	def "test simulator as string: false"() {
		when:
		extension.type = Type.iOS;
		extension.simulator = 'false'
		then:
		extension.simulator == false
	}


	def "test simulator as string: YES"() {
		when:
		extension.type = Type.iOS;
		extension.simulator = 'yEs'
		then:
		extension.simulator == true
	}

	def "test simulator as string: NO"() {
		when:
		extension.type = Type.iOS;
		extension.simulator = 'No'
		then:
		extension.simulator == false
	}

	def "test simulator true"() {
		when:
		extension.type = Type.iOS;
		extension.simulator = true
		then:
		extension.simulator == true
	}

	def "test simulator false"() {
		when:
		extension.type = Type.iOS;
		extension.simulator = false
		then:
		extension.simulator == false
	}

	def "test environment not crash for strings"() {
		when:
		extension.type = Type.iOS;
		extension.environment = 'TestEnvironmentVariable=trueeee'
		then:
		extension.environment.size() == 1
	}

	def "test environment not crash for Maps"() {
		when:
		extension.type = Type.iOS;
		extension.environment = ['TestEnvironmentFirstVariable' :  'trueeee', 'TestEnvironmentSecondVariable' :  'falseee']
		then:
		extension.environment.size() == 2
	}


	def "get binary iOS"() {
		when:
		File projectDir =  new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		XcodeProjectFile xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "Example.xcodeproj/project.pbxproj"))
		extension.projectSettings = xcodeProjectFile.getProjectSettings()
		extension.type = Type.iOS
		extension.simulator = false
		extension.target = "ExampleTodayWidget"
		extension.productType = "appex"
		extension.infoPlist = "../../example/iOS/Example/ExampleTodayWidget/Info.plist"

		then:
		extension.getBinary().toString().endsWith("Debug-iphoneos/ExampleTodayWidget.app/ExampleTodayWidget")
	}


	def "get binary OS X"() {
		when:
		File projectDir =  new File("../example/OSX/ExampleOSX")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		XcodeProjectFile xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "ExampleOSX.xcodeproj/project.pbxproj"))
		extension.projectSettings = xcodeProjectFile.getProjectSettings()
		extension.type = Type.macOS
		extension.simulator = false
		extension.target = "ExampleOSX"
		extension.productType = "app"
		extension.infoPlist = "../../example/iOS/ExampleOSX/Info.plist"

		then:
		extension.getBinary().toString().endsWith("build/sym/Debug/ExampleOSX.app/Contents/MacOS/ExampleOSX")
	}


	def "get project file"() {
		when:
		File projectDir =  new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)

		then:
		extension.projectFile.canonicalFile == new File("../example/iOS/Example/Example.xcodeproj").canonicalFile
	}

	def "set project file"() {
		when:
		File projectDir =  new File("../example/iOS")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.projectFile = "../example/iOS/Example/Example.xcodeproj"

		then:
		extension.projectFile.canonicalFile == new File("../example/iOS/Example/Example.xcodeproj").canonicalFile
	}


	def "XcodebuildParameters are created with proper values"() {
		when:
		File projectDir =  new File("../example/macOS/ExampleOSX")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.type = Type.macOS
		extension.simulator = false
		extension.target = "ExampleOSX"
		extension.scheme = "ExampleScheme"

		extension.workspace = "workspace"
		extension.configuration = "configuration"
		extension.additionalParameters = "additionalParameters"

		extension.dstRoot = new File(projectDir, "dstRoot")
		extension.objRoot = new File(projectDir, "objRoot")
		extension.symRoot = new File(projectDir, "symRoot")
		extension.sharedPrecompsDir = new File(projectDir, "sharedPrecompsDir")
		extension.derivedDataPath = new File(projectDir, "derivedDataPath")
		extension.arch = ['i386']


		def parameters = extension.getXcodebuildParameters()

		then:
		parameters.type == Type.macOS
		parameters.simulator == false
		parameters.target == "ExampleOSX"
		parameters.scheme == "ExampleScheme"
		parameters.workspace == "workspace"
		parameters.configuration == "configuration"
		parameters.additionalParameters == "additionalParameters"
		parameters.dstRoot.canonicalPath == new File(projectDir, "dstRoot").canonicalPath
		parameters.objRoot.canonicalPath == new File(projectDir, "objRoot").canonicalPath
		parameters.symRoot.canonicalPath == new File(projectDir, "symRoot").canonicalPath
		parameters.sharedPrecompsDir.canonicalPath == new File(projectDir, "sharedPrecompsDir").canonicalPath
		parameters.derivedDataPath.canonicalPath == new File(projectDir, "derivedDataPath").canonicalPath

		parameters.arch.size() == 1
		parameters.arch[0] == "i386"

		parameters.devices ==  Devices.UNIVERSAL
	}

	def "XcodebuildParameters get workspace from project"() {

		when:
		File projectDir =  new File("../example/iOS/SwiftExample")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.type = Type.iOS

		workspaceDirectory = new File(projectDir, "SwiftExample.xcworkspace")
		workspaceDirectory.mkdirs()

		def parameters = extension.getXcodebuildParameters()

		then:
		parameters.workspace == "SwiftExample.xcworkspace"

	}


}
