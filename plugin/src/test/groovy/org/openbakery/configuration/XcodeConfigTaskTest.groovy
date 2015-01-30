package org.openbakery.configuration

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.Destination
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.XcodePlugin
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfigTaskTest {

	XcodeConfigTask xcodeConfigTask
	Project project
	GMockController mockControl
	CommandRunner commandRunnerMock



	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)


		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeConfigTask = project.getTasks().getByName(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		xcodeConfigTask.setProperty("commandRunner", commandRunnerMock)

		project.xcodebuild.target = "test"

	}

	@AfterMethod
	def cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
	}


	void mockFindSimctl() {
		def commandList = ["xcrun", "-sdk", "iphoneos", "-find", "simctl"]
		commandRunnerMock.runWithResult(commandList).returns("/Applications/Xcode.app/Contents/Developer/usr/bin/simctl").times(1)
	}

	void mockSimctlList() {
		def commandList = ["/Applications/Xcode.app/Contents/Developer/usr/bin/simctl", "list"]

		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/simctl-output.txt"))

		commandRunnerMock.runWithResult(commandList).returns(simctlOutput).times(1)
	}

	void mockXcodePath() {
		def commandList = ["xcode-select", "-p"]
		commandRunnerMock.runWithResult(commandList).returns("build").times(1)

		File simulatorDirectory = new File("build/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad");
		simulatorDirectory.mkdirs()
	}

	void mockConvertProjectFile() {
		def commandList = [
						"plutil",
						"-convert",
						"xml1",
						new File(project.projectDir, "Example.xcodeproj/project.pbxproj").absolutePath,
						"-o",
						new File(project.buildDir, "project.plist").absolutePath
		]

		commandRunnerMock.run(commandList).times(1)


	}

	void mockXcodeVersion() {
		def commandList = [
						"xcodebuild",
						"-version",
		]
		commandRunnerMock.runWithResult(commandList).returns("Xcode 6.0\nBuild version 6A313").times(1)
	}

	void mockXcode5Version() {
		def commandList = [
						"xcodebuild",
						"-version",
		]
		commandRunnerMock.runWithResult(commandList).returns("Xcode 5.0\nBuild version 5A123").times(1)
	}

	void mockDisplayName() {
		def commandList = [
											"/usr/libexec/PlistBuddy",
											new File("build/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad/Info.plist").getAbsolutePath(),
											"-c",
											"Print :displayName"
							]
		commandRunnerMock.runWithResult(commandList).returns("iPad").times(1)


	}

	void mockNewerEquivalentDevice(String result) {
		def commandList = [
						"/usr/libexec/PlistBuddy",
						new File("build/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad/Info.plist").getAbsolutePath(),
						"-c",
						"Print :newerEquivalentDevice"
		]

		if (result == null) {
			commandRunnerMock.runWithResult(commandList).raises(new CommandRunnerException(""));
		} else {
			commandRunnerMock.runWithResult(commandList).returns(result)
		}

	}




	@Test
	void testCreateDeviceList_parseDevices() {
		mockConvertProjectFile()
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()

		mockControl.play {
			xcodeConfigTask.configuration()
		}

		assert project.xcodebuild.availableSimulators.size() == 14 : "expected 14 elements in the availableSimulators list but was: " +  project.xcodebuild.availableSimulators.size()

	}



	@Test
	void testDeviceListXcode5() {
		mockConvertProjectFile()
		mockXcode5Version()
		mockXcodePath();
		mockDisplayName();
		mockNewerEquivalentDevice(null);

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk").mkdirs()


		mockControl.play {
			xcodeConfigTask.configuration()
		}

		assert project.xcodebuild.availableDestinations.size() == 1 : "expected 1 elements in the availableSimulators list but was: " +  project.xcodebuild.availableDestinations.size()

		Destination destination =  project.xcodebuild.availableDestinations[0];

		assert destination.name.equals("iPad");
		assert destination.platform.equals("iOS Simulator")
		assert destination.os.equals("7.0")

	}


	@Test
	void testDeviceListXcode5_mutibleSimualtors() {
		mockConvertProjectFile()
		mockXcode5Version()
		mockXcodePath();
		mockDisplayName();
		mockNewerEquivalentDevice(null);

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk").mkdirs()
		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.1.sdk").mkdirs()
		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator6.0.sdk").mkdirs()

		mockControl.play {
			xcodeConfigTask.configuration()
		}

		assert project.xcodebuild.availableDestinations.size() == 3 : "expected 1 elements in the availableSimulators list but was: " +  project.xcodebuild.availableDestinations.size()

		Destination destination =  project.xcodebuild.availableDestinations[0];

		assert destination.name.equals("iPad");
		assert destination.platform.equals("iOS Simulator")
		assert destination.os.equals("6.0")

		assert project.xcodebuild.availableDestinations[1].os.equals("7.0");
		assert project.xcodebuild.availableDestinations[2].os.equals("7.1");

	}


	@Test
	void testDeviceListXcode5_skipDevice() {
		mockConvertProjectFile()
		mockXcode5Version()

		mockNewerEquivalentDevice("iPhone Retina (3.5-inch)");
		mockXcodePath();
		mockDisplayName();

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator6.0.sdk").mkdirs()

		mockControl.play {
			xcodeConfigTask.configuration()
		}

		assert project.xcodebuild.availableDestinations.size() == 0


	}


	@Test
	void testDestinationFilter_PhoneAndPad() {
		mockConvertProjectFile()
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()

		mockControl.play {
			xcodeConfigTask.configuration()
		}


		project.xcodebuild.destination {
			platform = 'iOS Simulator'
			name = 'iPad 2'
			os = '7.0'
		}

		project.xcodebuild.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
			os = '7.0'
		}


		assert project.xcodebuild.availableDestinations.size() == 2 : "expected 2 elements in the availableSimulators list but was: " +  project.xcodebuild.availableDestinations.size()

		assert project.xcodebuild.availableDestinations.asList()[0].id != null: "id of the destination should not be null"
	}




	@Test
	void testProductName() {

		xcodeConfigTask.setProperty("commandRunner", new CommandRunner())

		project.xcodebuild.target = "ExampleTodayWidget"

		xcodeConfigTask.parseInfoFromProjectFile()

		assert project.xcodebuild.productName.equals("ExampleTodayWidget")

	}

	@Test
	void testProductType() {
		xcodeConfigTask.setProperty("commandRunner", new CommandRunner())

		project.xcodebuild.target = "ExampleTodayWidget"

		xcodeConfigTask.parseInfoFromProjectFile()

		assert project.xcodebuild.productType.equals("appex")

	}

	@Test
	void testBundleName() {
		xcodeConfigTask.setProperty("commandRunner", new CommandRunner())

		project.xcodebuild.target = "Example"

		xcodeConfigTask.parseInfoFromProjectFile()

		assert project.xcodebuild.bundleName.equals("Example")

	}



	@Test
	void testBundleNameWidget() {
		xcodeConfigTask.setProperty("commandRunner", new CommandRunner())

		project.xcodebuild.target = "ExampleTodayWidget"

		xcodeConfigTask.parseInfoFromProjectFile()

		assert project.xcodebuild.bundleName.equals("ExampleTodayWidget")

	}



	@Test
	void testDestinationFilterPhone() {
		mockConvertProjectFile()
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()


		project.xcodebuild.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
		}

		mockControl.play {
			xcodeConfigTask.configuration()
		}


		assert project.xcodebuild.availableDestinations.size() == 2 : "expected 2 elements in the availableSimulators list but was: " + project.xcodebuild.availableDestinations.size()

		assert project.xcodebuild.availableDestinations.asList()[0].id != null: "id of the destination should not be null"
	}

	@Test
	void testDestinationFilterPhoneiOS7() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			xcodeConfigTask.createDeviceList()
		}


		project.xcodebuild.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
			os = '7.0'
		}

		assert project.xcodebuild.availableDestinations.size() == 1 : "expected 1 elements in the availableSimulators list but was: " + project.xcodebuild.availableDestinations.size()

		assert project.xcodebuild.availableDestinations.asList()[0].id != null: "id of the destination should not be null"
	}

	@Test
	void testDestinationFilteriOS7() {
		mockConvertProjectFile()
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			xcodeConfigTask.configuration()
		}


		project.xcodebuild.destination {
			platform = 'iOS Simulator'
			os = '7.0'
		}

		assert project.xcodebuild.availableDestinations.size() == 6 : "expected 6 elements in the availableSimulators list but was: " + project.xcodebuild.availableDestinations.size()

		assert project.xcodebuild.availableDestinations.asList()[0].id != null: "id of the destination should not be null"
	}



	@Test
	void testDestinationFilterPhoneWildcard() {
		mockConvertProjectFile()
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			xcodeConfigTask.configuration()
		}


		project.xcodebuild.destination {
			platform = 'iOS Simulator'
			name = '.*iPhone.*'
		}

		assert project.xcodebuild.availableDestinations.size() == 7 : "expected 7 elements in the availableSimulators list but was: " + project.xcodebuild.availableDestinations.size()

		assert project.xcodebuild.availableDestinations.asList()[0].id != null: "id of the destination should not be null"
	}


	@Test
	void testDestinations_iPhoneOS_Build() {
		mockConvertProjectFile()
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()

		project.xcodebuild.sdk = 'iphoneos'

		project.xcodebuild.destination {
			platform = 'iphoneos'
			name = 'iPhone 5s'
			id = '60B5BBDA-6485-44B4-AB87-9C0421EF5D8F'
		}



		mockControl.play {
			xcodeConfigTask.configuration()
		}

		assert project.xcodebuild.availableDestinations.size() == 1 : "expected 1 elements in the availableSimulators list but was: " + project.xcodebuild.availableDestinations.size()


		assert project.xcodebuild.availableDestinations.asList()[0].id.equals("60B5BBDA-6485-44B4-AB87-9C0421EF5D8F")

	}


	@Test
	void testProductNameFromConfig() {

		project.xcodebuild.productName = 'MyFancyProductName'
		xcodeConfigTask.setProperty("commandRunner", new CommandRunner())

		project.xcodebuild.target = "Example"

		xcodeConfigTask.parseInfoFromProjectFile()

		assert project.xcodebuild.productName.equals("MyFancyProductName")

	}


}
