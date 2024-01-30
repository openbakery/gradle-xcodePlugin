package org.openbakery.simulators

import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode
import spock.lang.Specification

import java.nio.file.Files

class SimulatorControl_XCode15_Specification extends Specification {

	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode xcode

	def setup() {
		xcode = new XcodeFake("15.0")
		simulatorControl = new SimulatorControl(commandRunner, xcode)

	}

	def cleanup() {
		simulatorControl = null
		commandRunner = null
		xcode = null
	}

	void mockSimctlList() {
		def file = new File("../libtest/src/main/Resource/simctl-list-xcode15.json")
		String json= Files.readString(file.toPath())
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
		0     | "iOS 14.5"    | new Version("14.5") | "18E182"    | "com.apple.CoreSimulator.SimRuntime.iOS-14-5"    | "iOS-14-5"      | true      | Type.iOS
		1     | "iOS 16.4"    | new Version("16.4") | "20E247"    | "com.apple.CoreSimulator.SimRuntime.iOS-16-4"    | "iOS-16-4"      | true      | Type.iOS
		2     | "iOS 17.0"    | new Version("17.0") | "21A328"    | "com.apple.CoreSimulator.SimRuntime.iOS-17-0"    | "iOS-17-0"      | true      | Type.iOS
	}

	def "parse result has 4 iOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		then:
		simulatorControl.getDevices(runtime).size() == 4
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
		0     | "iPhone 15"                             | "74BBFC84-7187-4DF3-A1D1-8B04ADAA4904" | "Shutdown" | true
		1     | "iPhone 15 Plus"                        | "25F81573-E7A6-4814-9B33-6601BCEFD31E" | "Shutdown" | true
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
		simulatorControl.deviceTypes.size() == 89
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
		32    | "com.apple.CoreSimulator.SimDeviceType.iPod-touch--7th-generation-" | "iPod touch (7th generation)" | "iPod-touch--7th-generation-"
		62    | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-1080p"           | "Apple TV 4K (at 1080p)"      | "Apple-TV-4K-1080p"
		78    | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-6-44mm"   | "Apple Watch Series 6 (44mm)" | "Apple-Watch-Series-6-44mm"

	}



	def "create creates iOS 17 devices"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-17-0"
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

	def "create creates only iOS 16.4 devices"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-16-4"
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([xcode.getSimctl(), "create", name, identifier, runtime])

		where:
		name                                    | identifier
		"iPad Air (5th generation)"             | "com.apple.CoreSimulator.SimDeviceType.iPad-Air-5th-generation"
		"iPad Pro (11-inch) (4th generation)"   | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-11-inch-4th-generation-8GB"
		"iPad Pro (12.9-inch) (2nd generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--12-9-inch---2nd-generation-"
		"iPad Pro (12.9-inch) (6th generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-12-9-inch-6th-generation-8GB"
		"iPad mini (6th generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation"
		"iPhone 8"                              | "com.apple.CoreSimulator.SimDeviceType.iPhone-8"
		"iPhone 8 Plus"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-8-Plus"
		"iPhone 11 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-11-Pro-Max"
		"iPhone 12 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max"
		"iPhone 14"                             | "com.apple.CoreSimulator.SimDeviceType.iPhone-14"
		"iPhone 14 Plus"                        | "com.apple.CoreSimulator.SimDeviceType.iPhone-14-Plus"
		"iPhone 14 Pro"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-14-Pro"
		"iPhone 14 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-14-Pro-Max"
		"iPhone SE (3rd generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPhone-SE-3rd-generation"
	}

}
