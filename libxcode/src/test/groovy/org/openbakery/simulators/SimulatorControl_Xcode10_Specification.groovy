package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode
import spock.lang.Specification

class SimulatorControl_Xcode10_Specification extends Specification {

	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode xcode = Mock(Xcode);

	def SIMCTL = "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"

	def setup() {
		xcode.getSimctl() >> SIMCTL
		xcode.getPath() >> "/Applications/Xcode.app"
		xcode.getVersion() >> new Version("11.0")
		simulatorControl = new SimulatorControl(commandRunner, xcode)
	}

	def cleanup() {
	}


	void setupXcode10() {
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("../libtest/src/main/Resource/simctl-list-xcode10.txt"))
	}


	def "has iOS 12 runtimes"() {
		given:
		setupXcode10()

		when:
		List<SimulatorRuntime> runtimeList = simulatorControl.getRuntimes()

		then:
		runtimeList != null
		runtimeList.size() == 3
		runtimeList.get(0).name == "iOS 12.0"
	}

	def "has iOS 12 devices"() {
		given:
		setupXcode10()

		when:
		List<SimulatorRuntime> runtimeList = simulatorControl.getRuntimes()
		SimulatorRuntime runtime = runtimeList.get(0)
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtime)

		then:
		devices != null
		devices.size() == 22
		runtime.name == "iOS 12.0"
	}

	def "has tvOS 12 devices"() {
		given:
		setupXcode10()

		when:
		List<SimulatorRuntime> runtimeList = simulatorControl.getRuntimes()
		SimulatorRuntime runtime = runtimeList.get(1)
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtime)

		then:
		devices != null
		devices.size() == 3
		runtime.name == "tvOS 12.0"
	}


	def "has paired watchOS 12 devices"() {
		given:
		setupXcode10()
		List<SimulatorDevicePair> devicesPairs = simulatorControl.getDevicePairs()

		expect:
		devicesPairs.get(index).watch.name == watchName
		devicesPairs.get(index).phone.name == phoneName

		where:
		index | watchName                     | phoneName
		0     | "Apple Watch Series 2 - 38mm" | "iPhone 7"
		1     | "Apple Watch Series 2 - 42mm" | "iPhone 7 Plus"
		2     | "Apple Watch Series 3 - 38mm" | "iPhone X"
		3     | "Apple Watch Series 4 - 40mm" | "iPhone XS"
		4     | "Apple Watch Series 4 - 44mm" | "iPhone XS Max"
	}



	def "create iOS12 iPhone Simulator"() {
		given:
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-12-0"
		setupXcode10()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "create", name, identifier, runtime])

		where:
		name             | identifier
		"iPhone 4s"      | "com.apple.CoreSimulator.SimDeviceType.iPhone-4s"
		"iPhone 5"       | "com.apple.CoreSimulator.SimDeviceType.iPhone-5"
		"iPhone 5s"      | "com.apple.CoreSimulator.SimDeviceType.iPhone-5s"
		"iPhone 6"       | "com.apple.CoreSimulator.SimDeviceType.iPhone-6"
		"iPhone 6 Plus"  | "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus"
		"iPhone 6s"      | "com.apple.CoreSimulator.SimDeviceType.iPhone-6s"
		"iPhone 6s Plus" | "com.apple.CoreSimulator.SimDeviceType.iPhone-6s-Plus"
		"iPhone 7"       | "com.apple.CoreSimulator.SimDeviceType.iPhone-7"
		"iPhone 7 Plus"  | "com.apple.CoreSimulator.SimDeviceType.iPhone-7-Plus"
		"iPhone 8"       | "com.apple.CoreSimulator.SimDeviceType.iPhone-8"
		"iPhone 8 Plus"  | "com.apple.CoreSimulator.SimDeviceType.iPhone-8-Plus"
		"iPhone SE"      | "com.apple.CoreSimulator.SimDeviceType.iPhone-SE"
		"iPhone X"       | "com.apple.CoreSimulator.SimDeviceType.iPhone-X"
		"iPhone XR"      | "com.apple.CoreSimulator.SimDeviceType.iPhone-XR"
		"iPhone XS"      | "com.apple.CoreSimulator.SimDeviceType.iPhone-XS"
		"iPhone XS Max"  | "com.apple.CoreSimulator.SimDeviceType.iPhone-XS-Max"
	}


	def "create iOS12 iPad Simulator"() {
		given:
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-12-0"
		setupXcode10()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "create", name, identifier, runtime])

		where:
		name                                    | identifier
		"iPad 2"                                | "com.apple.CoreSimulator.SimDeviceType.iPad-2"
		"iPad Retina"                           | "com.apple.CoreSimulator.SimDeviceType.iPad-Retina"
		"iPad Air"                              | "com.apple.CoreSimulator.SimDeviceType.iPad-Air"
		"iPad Air 2"                            | "com.apple.CoreSimulator.SimDeviceType.iPad-Air-2"
		"iPad (5th generation)"                 | "com.apple.CoreSimulator.SimDeviceType.iPad--5th-generation-"
		"iPad Pro (9.7-inch)"                   | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--9-7-inch-"
		"iPad Pro (12.9-inch)"                  | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro"
		"iPad Pro (12.9-inch) (2nd generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--12-9-inch---2nd-generation-"
		"iPad Pro (10.5-inch)"                  | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--10-5-inch-"
		"iPad (6th generation)"                 | "com.apple.CoreSimulator.SimDeviceType.iPad--6th-generation-"
	}

	def "create iOS12 watchOS Simulator"() {
		given:
		def runtime = "com.apple.CoreSimulator.SimRuntime.watchOS-5-0"
		setupXcode10()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "create", name, identifier, runtime])

		where:
		name                          | identifier
		"Apple Watch - 38mm"          | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-38mm"
		"Apple Watch - 42mm"          | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-42mm"
		"Apple Watch Series 2 - 38mm" | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-2-38mm"
		"Apple Watch Series 2 - 42mm" | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-2-42mm"
		"Apple Watch Series 3 - 38mm" | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-3-38mm"
		"Apple Watch Series 3 - 42mm" | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-3-42mm"
		"Apple Watch Series 4 - 40mm" | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-4-40mm"
		"Apple Watch Series 4 - 44mm" | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-4-44mm"
	}

	def "pair iOS 12 watchOS Simulator with iOS Simulator"() {
		given:
		setupXcode10()

		when:
		simulatorControl.pair()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "pair", phoneIdentifier, watchIdentifier])

		where:
		phoneIdentifier                        | watchIdentifier
		"8BB9F685-624D-465B-915E-2806FADCF93D" | "C8515D29-1AC5-4BC4-8CC9-FA772355DD0D"
		"F8A96503-DA70-49CA-8539-50AD84248C5B" | "3B86ABF4-306E-4006-B088-EB954B9BEBC2"
		"FDEEC7D6-C2DD-4170-94FE-815F70A40D28" | "B32FCD21-3466-4518-AB52-4A4509F3FBBA"
		"4F6EC3E7-F20A-4EDD-9778-9A9892257CAA" | "69EDA44E-C1F5-42F6-B385-85C2548F9D6D"
		"23365ACC-5084-4F5E-86EA-2D4E2115C1F4" | "70A59E9C-50D4-4856-BE7D-9A78EE6EC50E"
	}


	def "create iOS12 tvOS Simulator"() {
		given:
		def runtime = "com.apple.CoreSimulator.SimRuntime.tvOS-12-0"
		setupXcode10()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "create", name, identifier, runtime])

		where:
		name                     | identifier
		"Apple TV"               | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-1080p"
		"Apple TV 4K"            | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-4K"
		"Apple TV 4K (at 1080p)" | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-1080p"
	}


}
