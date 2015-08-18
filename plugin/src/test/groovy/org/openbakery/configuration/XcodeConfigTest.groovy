package org.openbakery.configuration

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.Destination
import org.openbakery.XcodePlugin
import org.openbakery.internal.XcodeBuildSpec
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfigTest {

	XcodeConfig xcodeConfig
	XcodeBuildSpec buildSpec
	Project project
	GMockController mockControl
	CommandRunner commandRunnerMock


	@Before
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)


		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		buildSpec = new XcodeBuildSpec(project)
		buildSpec.target = "Example"

		project.apply plugin: org.openbakery.XcodePlugin


		xcodeConfig = new XcodeConfig(project, buildSpec)
		xcodeConfig.setProperty("commandRunner", commandRunnerMock)


	}

	@After
	void cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
		FileUtils.deleteDirectory(new File("build/Contents"))
	}


	void mockFindSimctl() {
		def commandList = ["xcrun", "-sdk", XcodePlugin.SDK_IPHONEOS, "-find", "simctl"]
		commandRunnerMock.runWithResult(commandList).returns("/Applications/Xcode.app/Contents/Developer/usr/bin/simctl").times(1)
	}

	void mockSimctlList() {
		mockSimctlList("src/test/Resource/simctl-output.txt")
	}

	void mockSimctlList(String filePath) {
		def commandList = ["/Applications/Xcode.app/Contents/Developer/usr/bin/simctl", "list"]

		String simctlOutput = FileUtils.readFileToString(new File(filePath))

		commandRunnerMock.runWithResult(commandList).returns(simctlOutput).times(1)
	}

	void mockXcodePath() {
		//def commandList = ["xcode-select", "-p"]
		//commandRunnerMock.runWithResult(commandList).returns("build").times(1)

		project.xcodebuild.xcodePath = "build";
		File simulatorDirectory = new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad");
		simulatorDirectory.mkdirs()
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
						"build/Contents/Developer/usr/bin/xcodebuild",
						"-version",
		]
		commandRunnerMock.runWithResult(commandList).returns("Xcode 5.0\nBuild version 5A123").times(1)
	}

	void mockDisplayName() {
		def commandList = [
											"/usr/libexec/PlistBuddy",
											new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad/Info.plist").getAbsolutePath(),
											"-c",
											"Print :displayName"
							]
		commandRunnerMock.runWithResult(commandList).returns("iPad").times(1)


	}

	void mockNewerEquivalentDevice(String result) {
		def commandList = [
						"/usr/libexec/PlistBuddy",
						new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad/Info.plist").getAbsolutePath(),
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
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()

		mockControl.play {
			xcodeConfig.configuration()
		}

		assert project.xcodebuild.availableSimulators.size() == 14 : "expected 14 elements in the availableSimulators list but was: " +  project.xcodebuild.availableSimulators.size()

	}

	@Test
	void testCreateDeviceList_parseDevices_withUnavilableDevices() {
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList("src/test/Resource/simctl-unavailable-output.txt")

		mockControl.play {
			xcodeConfig.configuration()
		}

		assert project.xcodebuild.availableSimulators.size() == 16 : "expected 16 elements in the availableSimulators list but was: " +  project.xcodebuild.availableSimulators.size()

	}


	@Test
	void testDeviceListXcode5() {
		mockXcode5Version()
		mockXcodePath();
		mockDisplayName();
		mockNewerEquivalentDevice(null);

		new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk").mkdirs()


		mockControl.play {
			xcodeConfig.configuration()
		}

		assert project.xcodebuild.availableDestinations.size() == 1 : "expected 1 elements in the availableSimulators list but was: " +  project.xcodebuild.availableDestinations.size()

		Destination destination =  project.xcodebuild.availableDestinations[0];

		assert destination.name.equals("iPad");
		assert destination.platform.equals("iOS Simulator")
		assert destination.os.equals("7.0")

	}


	@Test
	void testDeviceListXcode5_multipleSimulators() {

		mockXcode5Version()
		mockXcodePath();
		mockDisplayName();
		mockNewerEquivalentDevice(null);

		new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk").mkdirs()
		new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.1.sdk").mkdirs()
		new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator6.0.sdk").mkdirs()

		mockControl.play {
			xcodeConfig.configuration()
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
		mockXcode5Version()

		mockNewerEquivalentDevice("iPhone Retina (3.5-inch)");
		mockXcodePath();
		mockDisplayName();

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator6.0.sdk").mkdirs()

		mockControl.play {
			xcodeConfig.configuration()
		}

		assert project.xcodebuild.availableDestinations.size() == 0


	}


	@Test
	void testDestinationFilter_PhoneAndPad() {
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()

		mockControl.play {
			xcodeConfig.configuration()
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
	void testDestinationFilterPhone() {
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()


		project.xcodebuild.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
		}

		mockControl.play {
			xcodeConfig.configuration()
		}


		assert project.xcodebuild.availableDestinations.size() == 2 : "expected 2 elements in the availableSimulators list but was: " + project.xcodebuild.availableDestinations.size()

		assert project.xcodebuild.availableDestinations.asList()[0].id != null: "id of the destination should not be null"
	}

	@Test
	void testDestinationFilterPhoneiOS7() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			xcodeConfig.createDeviceList()
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
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			xcodeConfig.configuration()
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
		mockXcodeVersion()
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			xcodeConfig.configuration()
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
		mockXcodeVersion()

		project.xcodebuild.sdk = 'iphoneos'
		buildSpec.sdk = 'iphoneos'

		project.xcodebuild.destination {
			platform = 'iphoneos'
			name = 'iPhone 5s'
			id = '60B5BBDA-6485-44B4-AB87-9C0421EF5D8F'
		}



		mockControl.play {
			xcodeConfig.configuration()
		}

		assert project.xcodebuild.availableDestinations.size() == 1 : "expected 1 elements in the availableSimulators list but was: " + project.xcodebuild.availableDestinations.size()


		assert project.xcodebuild.availableDestinations.asList()[0].id.equals("60B5BBDA-6485-44B4-AB87-9C0421EF5D8F")

	}

	@Test
	void testNonExistingTarget () {
		buildSpec.target = "test"

		mockControl.play {
			try {
				xcodeConfig.configuration()
				fail("Expected IllegalArgumentException was not thrown")
			} catch (IllegalArgumentException ex) {
				assert ex.getMessage().equals("Target 'test' not found in project")
			}
		}

	}
}
