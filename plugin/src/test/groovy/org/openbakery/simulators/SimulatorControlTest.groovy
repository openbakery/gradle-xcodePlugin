package org.openbakery.simulators

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.junit.Before
import org.junit.Test


/**
 * Created by rene on 30.04.15.
 */
class SimulatorControlTest {

	private static final String SIMCTL = "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"

	SimulatorControl simulatorControl
	def commandRunnerMock
	Project project


	@Before
	void setUp() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir.mkdirs()
		project.apply plugin: org.openbakery.XcodePlugin


		simulatorControl = new SimulatorControl(project)
		commandRunnerMock = new MockFor(CommandRunner)

	}


	void verify() {
		commandRunnerMock.verify simulatorControl.commandRunner
	}


	void mockSimCtlList() {

		commandRunnerMock.demand.runWithResult { parameters ->
			def expectedParameters = ["xcrun", "-sdk", XcodePlugin.SDK_IPHONEOS, "-find", "simctl"]
			if (parameters.equals(expectedParameters)) {
				return "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
			}
			println "parameters expected: " + expectedParameters
			println "but was: " + parameters
		}


		commandRunnerMock.demand.runWithResult { parameters ->
			def expectedParameters = [SIMCTL, "list"]
			if (parameters.equals(expectedParameters)) {
				return FileUtils.readFileToString(new File("src/test/Resource/simctl-unavailable-output.txt"))
			}
			println "parameters expected: " + expectedParameters
			println "but was: " + parameters
		}


	}

	void proxy() {
		simulatorControl.commandRunner = commandRunnerMock.proxyInstance()
	}

	@Test
	void runtimes() {
		mockSimCtlList()
		proxy()
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()

		assert runtimes != null
		assert runtimes.size() == 2

		// iOS 7.1 (7.1 - 11D167) (com.apple.CoreSimulator.SimRuntime.iOS-7-1)
		SimulatorRuntime runtime = runtimes.get(0)
		assert runtime.name.equals("iOS 7.1")
		assert runtime.version.equals("7.1")
		assert runtime.buildNumber.equals("11D167")
		assert runtime.identifier.equals("com.apple.CoreSimulator.SimRuntime.iOS-7-1")

		verify()
	}


	@Test
	void deviceTypes() {
		mockSimCtlList()
		proxy()

		List<SimulatorDeviceType> deviceTypes = simulatorControl.getDeviceTypes()
		assert deviceTypes != null
		assert deviceTypes.size() == 10

		SimulatorDeviceType deviceType = deviceTypes.get(0)
		assert deviceType.name.equals("iPhone 4s")
		assert deviceType.identifier.equals("com.apple.CoreSimulator.SimDeviceType.iPhone-4s")
	}

	@Test
	void devices_iOS7() {
		mockSimCtlList()
		proxy()

		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()

		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

		assert devices != null
		assert devices.size() == 6

		SimulatorDevice device = devices.get(0)
		assert device.name.equals("iPhone 4s")
		assert device.identifier.equals("73C126C8-FD53-44EA-80A3-84F5F19508C0")
		assert device.state.equals("Shutdown")
	}

	@Test
	void devices_iOS8() {
		mockSimCtlList()
		proxy()

		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()

		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(1));

		assert devices != null
		assert devices.size() == 10

		SimulatorDevice device = devices.get(9)
		assert device.name.equals("Resizable iPad")
		assert device.identifier.equals("50D9CBF1-608C-4866-9B5F-234D7FACBC16")
		assert device.state.equals("Shutdown")
	}


	@Test
	void deleteAll() {
		mockSimCtlList()

		ArrayList<String> deviceIds = new ArrayList<>(["73C126C8-FD53-44EA-80A3-84F5F19508C0",
																									 "15F68098-3B21-411D-B553-1C3161C100E7",
																									 "545260B4-C6B8-4D3A-9348-AD3B882D8D17",
																									 "454F3900-7B07-422E-A731-D46C821888B5",
																									 "F60A8735-97D9-48A8-9728-3CC53394F7FC",
																									 "B8278DAC-97EE-4097-88CA-5650960882A5",
																									 "E06E8144-D4AB-4616-A19E-9A489FB0CC17",
																									 "0560469A-813F-4AF7-826C-4598802A7FFD",
																									 "F029A31F-3CBF-422D-AEF4-D05675BAEDEF",
																									 "6F2558A0-A789-443B-B142-7BA707E3C9E8",
																									 "075026D3-C77E-40F9-944C-EBCB565E17D5",
																									 "5C4434E1-81AC-4448-8237-26029A57E594",
																									 "E85B0A4D-6B82-4F7C-B4CF-3C00E4EFF3D1",
																									 "A7400DB8-CDF3-4E6F-AF87-EB2B296D82C5",
																									 "29C34492-7006-41D7-B634-8703972F725C",
																									 "50D9CBF1-608C-4866-9B5F-234D7FACBC16"])



		for (String id in deviceIds) {
			commandRunnerMock.demand.runWithResult { parameters ->
				deviceIds.remove(parameters[2])
				println "delete called with " + parameters[2]
				return ""
			}
		}

		proxy()

		simulatorControl.deleteAll()
		assert deviceIds.size() == 0
	}

	@Test
	void createAll() {
		mockSimCtlList()


		ArrayList<String> expectedParameters = new ArrayList<>([
						[SIMCTL, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "Resizable iPhone", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPhone", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "Resizable iPad", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPad", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"],
						[SIMCTL, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "Resizable iPhone", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPhone", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"],
						[SIMCTL, "create", "Resizable iPad", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPad", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"]
		]);

		for (String p in expectedParameters) {
			commandRunnerMock.demand.runWithResult { parameters ->
				if (!expectedParameters.remove(parameters)) {
					println "unexpected call: " + parameters
				}
				return ""
			}
		}

		proxy()


		simulatorControl.createAll()
		assert expectedParameters.size() == 0

	}


	@Test
	void eraseAll() {
		mockSimCtlList()

		ArrayList<String> expectedParameters = new ArrayList<>(
						[[SIMCTL, "erase", "73C126C8-FD53-44EA-80A3-84F5F19508C0"],
						 [SIMCTL, "erase", "15F68098-3B21-411D-B553-1C3161C100E7"],
						 [SIMCTL, "erase", "545260B4-C6B8-4D3A-9348-AD3B882D8D17"],
						 [SIMCTL, "erase", "454F3900-7B07-422E-A731-D46C821888B5"],
						 [SIMCTL, "erase", "F60A8735-97D9-48A8-9728-3CC53394F7FC"],
						 [SIMCTL, "erase", "B8278DAC-97EE-4097-88CA-5650960882A5"],
						 [SIMCTL, "erase", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"],
						 [SIMCTL, "erase", "0560469A-813F-4AF7-826C-4598802A7FFD"],
						 [SIMCTL, "erase", "F029A31F-3CBF-422D-AEF4-D05675BAEDEF"],
						 [SIMCTL, "erase", "6F2558A0-A789-443B-B142-7BA707E3C9E8"],
						 [SIMCTL, "erase", "075026D3-C77E-40F9-944C-EBCB565E17D5"],
						 [SIMCTL, "erase", "5C4434E1-81AC-4448-8237-26029A57E594"],
						 [SIMCTL, "erase", "E85B0A4D-6B82-4F7C-B4CF-3C00E4EFF3D1"],
						 [SIMCTL, "erase", "A7400DB8-CDF3-4E6F-AF87-EB2B296D82C5"],
						 [SIMCTL, "erase", "29C34492-7006-41D7-B634-8703972F725C"],
						 [SIMCTL, "erase", "50D9CBF1-608C-4866-9B5F-234D7FACBC16"]])



		for (def p in expectedParameters) {
			commandRunnerMock.demand.runWithResult { parameters ->
				if (!expectedParameters.remove(parameters)) {
					println "unexpected call: " + parameters
				}
				return ""
			}
		}

		proxy()

		simulatorControl.eraseAll()
		assert expectedParameters.size() == 0
	}
}