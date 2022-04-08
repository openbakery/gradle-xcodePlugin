package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.Xcode
import spock.lang.Specification

class SimulatorControl_Xcode6_Specification extends Specification {


	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode xcode
	String simctlCommand

	def setup() {
		xcode = new XcodeFake("11.0")
		simulatorControl = new SimulatorControl(commandRunner, xcode)
		simulatorControl
		simctlCommand = xcode.getSimctl()
		commandRunner.runWithResult([simctlCommand, "list"]) >> FileUtils.readFileToString(new File("../libtest/src/main/Resource/simctl-list-unavailable.txt"))
	}

	def cleanup() {
		simulatorControl = null
		commandRunner = null
		xcode = null
	}



	def "devices iOS7"() {

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(1));

		then:
		devices != null
		devices.size() == 6
		devices.get(0).name.equals("iPhone 4s")
		devices.get(0).identifier.equals("73C126C8-FD53-44EA-80A3-84F5F19508C0")
		devices.get(0).state.equals("Shutdown")
	}



	def "devices iOS8"() {

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

		then:
		devices != null
		devices.size() == 10
		devices.get(9).name.equals("Resizable iPad")
		devices.get(9).identifier.equals("50D9CBF1-608C-4866-9B5F-234D7FACBC16")
		devices.get(9).state.equals("Shutdown")
	}

	def "get runtimes"() {

		expect:
		List<Runtime> runtimes = simulatorControl.getRuntimes()

		runtimes != null
		runtimes.size() == 2


		SimulatorRuntime runtime = runtimes.get(0)
		runtime.name.equals("iOS 8.2")
		runtime.version.toString().equals("8.2")
		runtime.buildNumber.equals("12D508")
		runtime.identifier.equals("com.apple.CoreSimulator.SimRuntime.iOS-8-2")

		SimulatorRuntime runtime1 = runtimes.get(1)
		runtime1.name.equals("iOS 7.1")
		runtime1.version.toString().equals("7.1")
		runtime1.buildNumber.equals("11D167")
		runtime1.identifier.equals("com.apple.CoreSimulator.SimRuntime.iOS-7-1")
	}


	def "launch simulator"() {

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));
		simulatorControl.runDevice(devices.get(0))

		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Developer/usr/bin/instruments", "-w", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"])

	}



	def "device types"() {

		when:
		List<SimulatorDeviceType> deviceTypes = simulatorControl.getDeviceTypes()

		then:
		deviceTypes != null
		deviceTypes.size() == 10
		deviceTypes.get(0).name.equals("iPhone 4s")
		deviceTypes.get(0).identifier.equals("com.apple.CoreSimulator.SimDeviceType.iPhone-4s")
	}


	def "delete All"() {

		when:
		simulatorControl.deleteAll()

		then:
		1* commandRunner.runWithResult([simctlCommand, "delete", "73C126C8-FD53-44EA-80A3-84F5F19508C0"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "15F68098-3B21-411D-B553-1C3161C100E7"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "545260B4-C6B8-4D3A-9348-AD3B882D8D17"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "454F3900-7B07-422E-A731-D46C821888B5"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "F60A8735-97D9-48A8-9728-3CC53394F7FC"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "B8278DAC-97EE-4097-88CA-5650960882A5"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "0560469A-813F-4AF7-826C-4598802A7FFD"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "F029A31F-3CBF-422D-AEF4-D05675BAEDEF"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "6F2558A0-A789-443B-B142-7BA707E3C9E8"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "075026D3-C77E-40F9-944C-EBCB565E17D5"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "5C4434E1-81AC-4448-8237-26029A57E594"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "E85B0A4D-6B82-4F7C-B4CF-3C00E4EFF3D1"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "A7400DB8-CDF3-4E6F-AF87-EB2B296D82C5"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "29C34492-7006-41D7-B634-8703972F725C"])
		1* commandRunner.runWithResult([simctlCommand, "delete", "50D9CBF1-608C-4866-9B5F-234D7FACBC16"])
	}


	def "create All"() {

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([simctlCommand, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "Resizable iPhone", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPhone", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "Resizable iPad", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPad", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "Resizable iPhone", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPhone", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.run([simctlCommand, "create", "Resizable iPad", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPad", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])

	}


	def "erase All"() {

		when:
		simulatorControl.eraseAll()

		then:
		1 * commandRunner.run([simctlCommand, "erase", "73C126C8-FD53-44EA-80A3-84F5F19508C0"])
		1 * commandRunner.run([simctlCommand, "erase", "15F68098-3B21-411D-B553-1C3161C100E7"])
		1 * commandRunner.run([simctlCommand, "erase", "545260B4-C6B8-4D3A-9348-AD3B882D8D17"])
		1 * commandRunner.run([simctlCommand, "erase", "454F3900-7B07-422E-A731-D46C821888B5"])
		1 * commandRunner.run([simctlCommand, "erase", "F60A8735-97D9-48A8-9728-3CC53394F7FC"])
		1 * commandRunner.run([simctlCommand, "erase", "B8278DAC-97EE-4097-88CA-5650960882A5"])
		1 * commandRunner.run([simctlCommand, "erase", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"])
		1 * commandRunner.run([simctlCommand, "erase", "0560469A-813F-4AF7-826C-4598802A7FFD"])
		1 * commandRunner.run([simctlCommand, "erase", "F029A31F-3CBF-422D-AEF4-D05675BAEDEF"])
		1 * commandRunner.run([simctlCommand, "erase", "6F2558A0-A789-443B-B142-7BA707E3C9E8"])
		1 * commandRunner.run([simctlCommand, "erase", "075026D3-C77E-40F9-944C-EBCB565E17D5"])
		1 * commandRunner.run([simctlCommand, "erase", "5C4434E1-81AC-4448-8237-26029A57E594"])
		1 * commandRunner.run([simctlCommand, "erase", "E85B0A4D-6B82-4F7C-B4CF-3C00E4EFF3D1"])
		1 * commandRunner.run([simctlCommand, "erase", "A7400DB8-CDF3-4E6F-AF87-EB2B296D82C5"])
		1 * commandRunner.run([simctlCommand, "erase", "29C34492-7006-41D7-B634-8703972F725C"])
		1 * commandRunner.run([simctlCommand, "erase", "50D9CBF1-608C-4866-9B5F-234D7FACBC16"])

	}

	def launchSimulator() {

		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

		when:
		simulatorControl.runDevice(devices.get(0))


		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Developer/usr/bin/instruments", "-w", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"])
	}


}
