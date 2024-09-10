package org.openbakery.simulators

import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode
import spock.lang.Specification

import java.nio.file.Files

class SimulatorControl_Xcode16_Specification extends Specification {

	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode xcode

	def setup() {
		xcode = new XcodeFake("16.0")
		simulatorControl = new SimulatorControl(commandRunner, xcode)

	}

	def cleanup() {
		simulatorControl = null
		commandRunner = null
		xcode = null
	}

	void mockSimctlList() {
		def file = new File("../libtest/src/main/Resource/simctl-list-xcode16.json")
		String json = Files.readString(file.toPath())
		commandRunner.runWithResult([xcode.getSimctl(), "list", "--json"]) >> json
	}


	def "list uses json format when xcode 14"() {
		given:
		def commandList

		when:
		try {
			simulatorControl.parse()
		} catch (Exception ignored) {
		}

		then:
		1 * commandRunner.runWithResult(_) >> { arguments -> commandList = arguments[0] }
		commandList == [
			xcode.getSimctl(),
			"list",
			"--json"
		]
	}

	def "parse result has one runtimes"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()

		then:
		simulatorControl.getRuntimes().size() == 3
	}


	def "parse result proper runtime data"() {
		given:
		mockSimctlList()
		simulatorControl.parse()

		expect:
		simulatorControl.getRuntimes()[index].name == name
		simulatorControl.getRuntimes()[index].version == version
		simulatorControl.getRuntimes()[index].version == version
		simulatorControl.getRuntimes()[index].buildNumber == buildNumber
		simulatorControl.getRuntimes()[index].identifier == identifier
		simulatorControl.getRuntimes()[index].shortIdentifier == shortIdentifier
		simulatorControl.getRuntimes()[index].available == available
		simulatorControl.getRuntimes()[index].type == type

		where:
		index | name          | version             | buildNumber | identifier                                       | shortIdentifier | available | type
		0     | "iOS 17.5"    | new Version("17.5") | "21F79"     | "com.apple.CoreSimulator.SimRuntime.iOS-17-5"    | "iOS-17-5"      | true      | Type.iOS
		1     | "iOS 18.0"    | new Version("18.0") | "22A3351"     | "com.apple.CoreSimulator.SimRuntime.iOS-18-0"  | "iOS-18-0"      | true      | Type.iOS
	}

	def "parse result has 15 iOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		then:
		simulatorControl.getDevices(runtime).size() == 15
	}

	def "parse result has 0 tvOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)

		then:
		runtime == null
	}


	def "parse result has 0 watchOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)

		then:
		runtime == null
	}



	def "parse creates the iOS devices with the proper data"() {
		given:
		mockSimctlList()
		simulatorControl.parse()

		expect:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.iOS)
		simulatorControl.getDevices(runtime)[index].name == name
		simulatorControl.getDevices(runtime)[index].identifier == identifier
		simulatorControl.getDevices(runtime)[index].state == state
		simulatorControl.getDevices(runtime)[index].available == available

		where:
		index | name                                    | identifier                             | state      | available
		0     | "iPhone SE (3rd generation)"            | "3062F87E-E482-4389-94D9-F4949BAED4A4" | "Shutdown" | true
		7     | "iPhone 16"                             | "F3F45655-01A8-46C3-840C-660DB808F1F8" | "Shutdown" | true
		8     | "iPhone 16 Plus"                        | "6D818EC4-539B-4F66-8FE0-A5BFA97C2EC4" | "Shutdown" | true
	}

	def "has no pairs"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)

		then:
		simulatorControl.getDevicePairs().size() == 0
	}


	def "has 89 devices types"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()

		then:
		simulatorControl.deviceTypes.size() == 102
	}



	def "has devices types values"() {
		given:
		mockSimctlList()

		expect:
		simulatorControl.deviceTypes[index].identifier == identifier
		simulatorControl.deviceTypes[index].name == name
		simulatorControl.deviceTypes[index].shortIdentifier == shortIdentifier

		where:
		index | identifier                                                          | name                          | shortIdentifier
		0     | "com.apple.CoreSimulator.SimDeviceType.iPhone-6s"                   | "iPhone 6s"                   | "iPhone-6s"
		5     | "com.apple.CoreSimulator.SimDeviceType.iPhone-8"                    | "iPhone 8"                    | "iPhone-8"
		36    | "com.apple.CoreSimulator.SimDeviceType.iPod-touch--7th-generation-" | "iPod touch (7th generation)" | "iPod-touch--7th-generation-"
		72    | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-1080p"           | "Apple TV 4K (at 1080p)"      | "Apple-TV-4K-1080p"
		88    | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-6-44mm"   | "Apple Watch Series 6 (44mm)" | "Apple-Watch-Series-6-44mm"
	}


	def "create creates iOS 18 devices"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-18-0"
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([xcode.getSimctl(), "create", name, identifier, runtime])

		where:
		name                                    | identifier
		"iPhone SE (3rd generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPhone-SE-3rd-generation"
		"iPhone 16"                             | "com.apple.CoreSimulator.SimDeviceType.iPhone-16"
		"iPhone 16 Plus"                        | "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Plus"
		"iPhone 16 Pro"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro"
		"iPhone 16 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro-Max"
		"iPad Air (5th generation)"             | "com.apple.CoreSimulator.SimDeviceType.iPad-Air-5th-generation"
		"iPad (10th generation)"                | "com.apple.CoreSimulator.SimDeviceType.iPad-10th-generation"
		"iPad mini (6th generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation"
		"iPad Pro (11-inch) (4th generation)"   | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-11-inch-4th-generation-8GB"
		"iPad Pro (12.9-inch) (6th generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-12-9-inch-6th-generation-8GB"
	}

	def "create creates only iOS 17-5 devices"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-17-5"
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([xcode.getSimctl(), "create", name, identifier, runtime])

		where:
		name                                    | identifier
		"iPhone SE (3rd generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPhone-SE-3rd-generation"
		"iPhone 15"                             | "com.apple.CoreSimulator.SimDeviceType.iPhone-15"
		"iPhone 15 Plus"                        | "com.apple.CoreSimulator.SimDeviceType.iPhone-15-Plus"
		"iPhone 15 Pro"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-15-Pro"
		"iPhone 15 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-15-Pro-Max"
		"iPad Air (5th generation)"             | "com.apple.CoreSimulator.SimDeviceType.iPad-Air-5th-generation"
		"iPad (10th generation)"                | "com.apple.CoreSimulator.SimDeviceType.iPad-10th-generation"
		"iPad mini (6th generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation"
		"iPad Pro (11-inch) (4th generation)"   | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-11-inch-4th-generation-8GB"
		"iPad Pro (12.9-inch) (6th generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-12-9-inch-6th-generation-8GB"
	}

}
