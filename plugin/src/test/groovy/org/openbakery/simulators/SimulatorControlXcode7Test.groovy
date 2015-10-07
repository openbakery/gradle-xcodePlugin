package org.openbakery.simulators

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * Created by rene on 30.06.15.
 */
class SimulatorControlXcode7Test {
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
				def expectedParameters = ["xcrun", "-sdk", "iphoneos", "-find", "simctl"]
				if (parameters.equals(expectedParameters)) {
					return "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
				}
				println "parameters expected: " + expectedParameters
				println "but was: " + parameters
			}


			commandRunnerMock.demand.runWithResult { parameters ->
				def expectedParameters = [SIMCTL, "list"]
				if (parameters.equals(expectedParameters)) {
					return FileUtils.readFileToString(new File("src/test/Resource/simctl-output-xcode7.txt"))
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

			assertThat(runtimes, notNullValue())
			assertThat(runtimes.size(), is(3))

			// iOS 7.1 (7.1 - 11D167) (com.apple.CoreSimulator.SimRuntime.iOS-7-1)
			SimulatorRuntime runtime = runtimes.get(0)
			assertThat(runtime.name, is(equalTo("iOS 7.1")))
			assertThat(runtime.version, is(equalTo("7.1")))
			assertThat(runtime.buildNumber, is(equalTo("11D167")))
			assertThat(runtime.identifier, is(equalTo("com.apple.CoreSimulator.SimRuntime.iOS-7-1")))


			runtime = runtimes.get(2)
			assertThat(runtime.name, is(equalTo("watchOS 2.0")))
			assertThat(runtime.version, is(equalTo("2.0")))
			assertThat(runtime.buildNumber, is(equalTo("13S5255c")))
			assertThat(runtime.identifier, is(equalTo("com.apple.CoreSimulator.SimRuntime.watchOS-2-0")))



			verify()
		}


		@Test
		void deviceTypes() {
			mockSimCtlList()
			proxy()

			List<SimulatorDeviceType> deviceTypes = simulatorControl.getDeviceTypes()
			assertThat(deviceTypes, notNullValue())
			assertThat(deviceTypes.size(), is(11))


			SimulatorDeviceType deviceType = deviceTypes.get(0)
			assertThat(deviceType.name, equalTo("iPhone 4s"))
			assertThat(deviceType.identifier, equalTo("com.apple.CoreSimulator.SimDeviceType.iPhone-4s"))

			deviceType = deviceTypes.get(10)

			assertThat(deviceType.name, equalTo("Apple Watch - 42mm"))
			assertThat(deviceType.identifier, equalTo("com.apple.CoreSimulator.SimDeviceType.Apple-Watch-42mm"))

		}

		@Test
		void devices_iOS7() {
			mockSimCtlList()
			proxy()

			List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()

			List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

			assertThat(devices, notNullValue())
			assertThat(devices.size(), is(6))

			SimulatorDevice device = devices.get(0)
			assertThat(device.name, equalTo("iPhone 4s"))
			assertThat(device.identifier, equalTo("DF327EC0-EBBE-479E-9408-7AC308C6E929"))
			assertThat(device.state, equalTo("Shutdown"))
		}

		@Test
		void devices_iOS9() {
			mockSimCtlList()
			proxy()

			List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()

			List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(1));

			assertThat(devices, notNullValue())
			assertThat(devices.size(), is(9))

			SimulatorDevice device = devices.get(8)
			assertThat(device.name, equalTo("iPad Air 2"))
			assertThat(device.identifier, equalTo("71B976E4-6790-4A66-8C1C-BFC85BD124D0"))
			assertThat(device.state, equalTo("Shutdown"))
		}


		@Test
		void deleteAll() {
			mockSimCtlList()

			ArrayList<String> deviceIds = new ArrayList<>(["A0DDD8A1-8807-4B65-BFF6-AA4798CC65CD",
																										 "D5048D12-E255-42EB-93D9-52E8C1F0FDCA",
																										 "8E417F90-8FB8-4F6A-91F4-D86E1CBE97F0",
																										 "B619BA1B-1AC1-46C6-9828-6A8D457007A0",
																										 "6E76E5D4-CDA9-48B5-B939-94DC54373C2F",
																										 "4538228E-B5E0-45E3-8C8D-04B2C3FBD924",
																										 "DE9DAA62-88D3-4186-A7BE-4DEA01ADFDAA",
																										 "B02DADB5-4483-48C1-971D-20D1247BC42B",
																										 "71B976E4-6790-4A66-8C1C-BFC85BD124D0",
																										 "E27AECDB-D1A3-4D31-95DB-CE13023BCF82",
																										 "C74CB109-2F05-46AA-93FB-7675DE1DE0A0"])



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

							[SIMCTL, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],
							[SIMCTL, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],
							[SIMCTL, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],
							[SIMCTL, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],
							[SIMCTL, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],
							[SIMCTL, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],
							[SIMCTL, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],
							[SIMCTL, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],
							[SIMCTL, "create", "iPad Air 2", "com.apple.CoreSimulator.SimDeviceType.iPad-Air-2", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"],

							[SIMCTL, "create", "Apple Watch - 38mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-38mm", "com.apple.CoreSimulator.SimRuntime.watchOS-2-0"],
							[SIMCTL, "create", "Apple Watch - 42mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-42mm", "com.apple.CoreSimulator.SimRuntime.watchOS-2-0"],

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

			ArrayList<String> expectedParameters = new ArrayList<>([[SIMCTL, "erase", "A0DDD8A1-8807-4B65-BFF6-AA4798CC65CD"],
																															[SIMCTL, "erase", "D5048D12-E255-42EB-93D9-52E8C1F0FDCA"],
																															[SIMCTL, "erase", "8E417F90-8FB8-4F6A-91F4-D86E1CBE97F0"],
																															[SIMCTL, "erase", "B619BA1B-1AC1-46C6-9828-6A8D457007A0"],
																															[SIMCTL, "erase", "6E76E5D4-CDA9-48B5-B939-94DC54373C2F"],
																															[SIMCTL, "erase", "4538228E-B5E0-45E3-8C8D-04B2C3FBD924"],
																															[SIMCTL, "erase", "DE9DAA62-88D3-4186-A7BE-4DEA01ADFDAA"],
																															[SIMCTL, "erase", "B02DADB5-4483-48C1-971D-20D1247BC42B"],
																															[SIMCTL, "erase", "71B976E4-6790-4A66-8C1C-BFC85BD124D0"],
																															[SIMCTL, "erase", "E27AECDB-D1A3-4D31-95DB-CE13023BCF82"],
																															[SIMCTL, "erase", "C74CB109-2F05-46AA-93FB-7675DE1DE0A0"]])



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
