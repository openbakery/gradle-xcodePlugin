package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
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

	HashMap<String, BuildConfiguration> createProjectSettings() {
		HashMap<String, BuildConfiguration> result = new HashMap<>()

		BuildConfiguration appConfiguration = new BuildConfiguration();
		appConfiguration.release = new BuildSettings();
		appConfiguration.debug = new BuildSettings();

		appConfiguration.debug.infoplist = "ExampleWatchkit/Info.plist"
		appConfiguration.debug.bundleIdentifier = "org.openbakery.Example"
		appConfiguration.debug.productName = "ExampleWatchkit"
		appConfiguration.debug.sdkRoot = "iphoneos"
		appConfiguration.release.infoplist = "ExampleWatchkit/Info.plist"
		appConfiguration.release.bundleIdentifier = "org.openbakery.Example"
		appConfiguration.release.productName = "ExampleWatchkit"
		appConfiguration.release.sdkRoot = "iphoneos"
		appConfiguration.release.devices = Devices.UNIVERSAL


		BuildConfiguration watchAppConfiguration = new BuildConfiguration();
		watchAppConfiguration.release = new BuildSettings();
		watchAppConfiguration.debug = new BuildSettings();
		watchAppConfiguration.debug.infoplist = "ExampleWatchkit WatchKit App/Info.plist"
		watchAppConfiguration.debug.bundleIdentifier = "org.openbakery.Example.watchkitapp"
		watchAppConfiguration.debug.productName = "ExampleWatchkit WatchKit App"
		watchAppConfiguration.debug.sdkRoot = "watchos"
		watchAppConfiguration.release.infoplist = "ExampleWatchkit WatchKit App/Info.plist"
		watchAppConfiguration.release.bundleIdentifier = "org.openbakery.Example.watchkitapp"
		watchAppConfiguration.release.productName = "ExampleWatchkit WatchKit App"
		watchAppConfiguration.release.sdkRoot = "watchos"
		watchAppConfiguration.release.devices = Devices.WATCH

		BuildConfiguration extenstionConfiguration = new BuildConfiguration();
		extenstionConfiguration.release = new BuildSettings();
		extenstionConfiguration.debug = new BuildSettings();
		extenstionConfiguration.debug.infoplist = "ExampleWatchkit WatchKit Extension/Info.plist"
		extenstionConfiguration.debug.bundleIdentifier = "org.openbakery.Example.watchkitapp.watchkitextension"
		extenstionConfiguration.debug.productName = "ExampleWatchkit WatchKit Extension"
		extenstionConfiguration.debug.sdkRoot = "watchos"
		extenstionConfiguration.release.infoplist = "ExampleWatchkit WatchKit Extension/Info.plist"
		extenstionConfiguration.release.bundleIdentifier = "org.openbakery.Example.watchkitapp.watchkitextension"
		extenstionConfiguration.release.productName = "ExampleWatchkit WatchKit Extension"
		extenstionConfiguration.release.sdkRoot = "watchos"
		extenstionConfiguration.release.devices = Devices.WATCH


		result.put("ExampleWatchkit", appConfiguration);
		result.put("ExampleWatchkit WatchKit App", watchAppConfiguration);
		result.put("ExampleWatchkit WatchKit Extension", extenstionConfiguration);

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


		BuildSettings parent = extension.getParent(extension.projectSettings["ExampleWatchkit WatchKit App"].debug)

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

}
