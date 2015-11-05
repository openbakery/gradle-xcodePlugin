package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.simulators.SimulatorControl
import org.openbakery.stubs.SimulatorControlStub
import spock.lang.Specification

/**
 * Created by rene on 07.10.15.
 */
class XcodeBuildPluginExtensionSpecification extends Specification {

	Project project
	File projectDir
	XcodeBuildPluginExtension extension;
	CommandRunner commandRunner = Mock(CommandRunner)

	File xcodebuild6_1
	File xcodebuild6_0
	File xcodebuild5_1


	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		extension = new XcodeBuildPluginExtension(project)
		extension.commandRunner = commandRunner
		extension.infoPlist = "Info.plist";


		xcodebuild6_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode6-1.app")
		xcodebuild6_0 = new File(System.getProperty("java.io.tmpdir"), "Xcode6.app")
		xcodebuild5_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode5.app")

		new File(xcodebuild6_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild6_0, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild5_1, "Contents/Developer/usr/bin").mkdirs()

		new File(xcodebuild6_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcodebuild6_0, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcodebuild5_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()

	}

	def cleanup() {
		FileUtils.deleteDirectory(xcodebuild6_1)
		FileUtils.deleteDirectory(xcodebuild6_0)
		FileUtils.deleteDirectory(xcodebuild5_1)
		FileUtils.deleteDirectory(projectDir)
	}

	HashMap<String, BuildTargetConfiguration> createProjectSettings() {
		HashMap<String, BuildTargetConfiguration> result = new HashMap<>()

		BuildTargetConfiguration appConfiguration = new BuildTargetConfiguration();
		BuildConfiguration release = new BuildConfiguration();
		BuildConfiguration debug = new BuildConfiguration();
		appConfiguration.buildSettings["Release"] = release
		appConfiguration.buildSettings["Debug"] = debug

		debug.infoplist = "ExampleWatchkit/Info.plist"
		debug.bundleIdentifier = "org.openbakery.Example"
		debug.productName = "ExampleWatchkit"
		debug.sdkRoot = "iphoneos"
		release.infoplist = "ExampleWatchkit/Info.plist"
		release.bundleIdentifier = "org.openbakery.Example"
		release.productName = "ExampleWatchkit"
		release.sdkRoot = "iphoneos"
		release.devices = Devices.UNIVERSAL


		BuildTargetConfiguration watchAppConfiguration = new BuildTargetConfiguration();
		BuildConfiguration watchAppConfigurationRelease = new BuildConfiguration();
		BuildConfiguration watchAppConfigurationDebug = new BuildConfiguration();
		watchAppConfiguration.buildSettings["Release"] = watchAppConfigurationRelease
		watchAppConfiguration.buildSettings["Debug"] = watchAppConfigurationDebug

		watchAppConfigurationDebug.infoplist = "ExampleWatchkit WatchKit App/Info.plist"
		watchAppConfigurationDebug.bundleIdentifier = "org.openbakery.Example.watchkitapp"
		watchAppConfigurationDebug.productName = "ExampleWatchkit WatchKit App"
		watchAppConfigurationDebug.sdkRoot = "watchos"
		watchAppConfigurationRelease.infoplist = "ExampleWatchkit WatchKit App/Info.plist"
		watchAppConfigurationRelease.bundleIdentifier = "org.openbakery.Example.watchkitapp"
		watchAppConfigurationRelease.productName = "ExampleWatchkit WatchKit App"
		watchAppConfigurationRelease.sdkRoot = "watchos"
		watchAppConfigurationRelease.devices = Devices.WATCH

		BuildTargetConfiguration extensionConfiguration = new BuildTargetConfiguration();
		BuildConfiguration extenstionConfigurationRelease = new BuildConfiguration();
		BuildConfiguration extenstionConfigurationDebug = new BuildConfiguration();
		extensionConfiguration.buildSettings["Release"] = extenstionConfigurationRelease
		extensionConfiguration.buildSettings["Debug"] = extenstionConfigurationDebug

		extenstionConfigurationDebug.infoplist = "ExampleWatchkit WatchKit Extension/Info.plist"
		extenstionConfigurationDebug.bundleIdentifier = "org.openbakery.Example.watchkitapp.watchkitextension"
		extenstionConfigurationDebug.productName = "ExampleWatchkit WatchKit Extension"
		extenstionConfigurationDebug.sdkRoot = "watchos"
		extenstionConfigurationRelease.infoplist = "ExampleWatchkit WatchKit Extension/Info.plist"
		extenstionConfigurationRelease.bundleIdentifier = "org.openbakery.Example.watchkitapp.watchkitextension"
		extenstionConfigurationRelease.productName = "ExampleWatchkit WatchKit Extension"
		extenstionConfigurationRelease.sdkRoot = "watchos"
		extenstionConfigurationRelease.devices = Devices.WATCH


		result.put("ExampleWatchkit", appConfiguration);
		result.put("ExampleWatchkit WatchKit App", watchAppConfiguration);
		result.put("ExampleWatchkit WatchKit Extension", extensionConfiguration);

		return result
	}

	def "xcode version"() {
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcodebuild5_1.absolutePath + "\n"  + xcodebuild6_0.absolutePath + "\n" + xcodebuild6_1.absolutePath
		commandRunner.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 5.1.1\nBuild version 5B1008")

		when:
		extension.version = '5B1008';

		then:
		extension.getXcodebuildCommand().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}


	def "xcodeVersion select last"() {
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >>  xcodebuild6_1.absolutePath + "\n"  + xcodebuild6_0.absolutePath + "\n" + xcodebuild5_1.absolutePath
		commandRunner.runWithResult(xcodebuild6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
		commandRunner.runWithResult(xcodebuild6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
		commandRunner.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 5.1.1\nBuild version 5B1008"


		when:
		extension.version = '5B1008';

		then:
		extension.getXcodebuildCommand().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}


	def "xcodeVersion select not found"() {

		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcodebuild6_1.absolutePath + "\n"  + xcodebuild6_0.absolutePath + "\n" + xcodebuild5_1.absolutePath
		commandRunner.runWithResult(xcodebuild6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
		commandRunner.runWithResult(xcodebuild6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
		commandRunner.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 5.1.1\nBuild version 5B1008"


		when:
		extension.version = '5B1009';

		then:
		thrown(IllegalStateException)
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
		parent.bundleIdentifier == "org.openbakery.Example"
	}



	void mockValueFromPlist(String key, String value) {
		File infoPlist = new File(project.projectDir, "Info.plist")
		def commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", "Print :" + key]
		commandRunner.runWithResult(commandList) >> value
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

	def xcodeVersionSimple() {

		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >>  xcodebuild5_1.absolutePath + "\n"  + xcodebuild6_0.absolutePath + "\n" + xcodebuild6_1.absolutePath
		commandRunner.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 5.1.1\nBuild version 5B1008"

		when:
		extension.version = '5.1';

		then:
		extension.getXcodebuildCommand().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}

	def "xcode Version Simple 1"() {

		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcodebuild5_1.absolutePath + "\n" + xcodebuild6_0.absolutePath + "\n" + xcodebuild6_1.absolutePath
		commandRunner.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 5.1.1\nBuild version 5B1008"

		when:
		extension.version = '5.1.1';

		then:
		extension.getXcodebuildCommand().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}


	def "xcodeVersion Simple not found"() {
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcodebuild6_1.absolutePath + "\n" + xcodebuild6_0.absolutePath + "\n" + xcodebuild5_1.absolutePath
		commandRunner.runWithResult(xcodebuild6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
		commandRunner.runWithResult(xcodebuild6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
		commandRunner.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 5.1.1\nBuild version 5B1008"

		when:
		extension.version = '5.1.2';

		then:
		thrown(IllegalStateException)
	}

	def "testXcodePath_notSet"() {
		commandRunner.runWithResult("xcode-select", "-p") >> "/Applications/Xcode.app/Contents/Developer"

		when:
		extension.xcodePath = null

		then:
		extension.xcodePath.equals("/Applications/Xcode.app")

	}


	def "available destinations for OS X"() {

		when:
		extension.type = Type.OSX

		then:
		extension.getAvailableDestinations().size() == 1
	}

	def "os x has no simulator"() {

		when:
		extension.type = Type.OSX
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

	def "available destinations default"() {
		given:
		extension.availableSimulators << createDestination("iPad", "iPad Air", "9.0")
		extension.availableSimulators << createDestination("iPhone", "iPhone 4s", "9.0")


		when:
		def destinations = extension.getAvailableDestinations()

		then:
		destinations.size() == 2

	}


	def "available destinations match"() {
		given:
		extension.availableSimulators << createDestination("iPad", "iPad Air", "9.0")
		extension.availableSimulators << createDestination("iPhone", "iPhone 4s", "9.0")

		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPad Air'
			os = "9.0"
		}

		when:
		def destinations = extension.getAvailableDestinations()

		then:
		destinations.size() == 1

	}


	def "available destinations not match"() {
		given:
		extension.availableSimulators << createDestination("iPad", "iPad Air", "9.0")
		extension.availableSimulators << createDestination("iPhone", "iPhone 4s", "9.0")
		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPad Air'
			os = "8.0"
		}


		when:
		extension.getAvailableDestinations()

		then:
		thrown(IllegalStateException)

	}



	def "available destinations match simple single"() {
		given:
		extension.simulatorControl = new SimulatorControlStub("simctl-list-xcode7.txt")
		extension.availableSimulators << createDestination("iPad", "iPad Air", "7.0")
		extension.availableSimulators << createDestination("iPad", "iPad Air", "9.0")
		extension.availableSimulators << createDestination("iPad", "iPad Air", "8.0")
		extension.availableSimulators << createDestination("iPhone", "iPhone 4s", "9.0")

		extension.destination = 'iPad Air'

		when:
		def destinations = extension.getAvailableDestinations()

		then:
		destinations.size() == 1
		destinations[0].name == "iPad Air"
		destinations[0].os == "9.0"

	}

	def "available destinations match simple multiple"() {
		given:
		extension.simulatorControl = new SimulatorControlStub("simctl-list-xcode7.txt")
		extension.availableSimulators << createDestination("iPad", "iPad Air", "7.0")
		extension.availableSimulators << createDestination("iPad", "iPad Air", "9.0")
		extension.availableSimulators << createDestination("iPad", "iPad Air", "8.0")
		extension.availableSimulators << createDestination("iPhone", "iPhone 4s", "9.0")
		extension.availableSimulators << createDestination("iPhone", "iPhone 5s", "9.0")
		extension.availableSimulators << createDestination("iPhone", "iPhone 6s", "9.0")

		extension.destination = ['iPad Air', 'iPhone 4s']

		when:
		def destinations = extension.getAvailableDestinations()

		then:
		destinations.size() == 2

	}

}
