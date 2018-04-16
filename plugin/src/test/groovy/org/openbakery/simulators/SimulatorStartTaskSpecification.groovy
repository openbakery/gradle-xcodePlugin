package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.xcode.Destination
import org.openbakery.XcodePlugin
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.xcode.DestinationResolver
import spock.lang.Specification

class SimulatorStartTaskSpecification extends Specification {

	Project project
	File projectDir
	SimulatorControl simulatorControl = Mock(SimulatorControl)
	DestinationResolver destinationResolver = Mock(DestinationResolver)
	SimulatorStartTask task


	def devices9_1 = [
					new SimulatorDevice("iPhone 4s (8C8C43D3-B53F-4091-8D7C-6A4B38051389) (Shutdown)"),
					new SimulatorDevice("iPhone 5 (2EEDED93-1568-4D46-84A2-6E2AE723ECC6) (Shutdown)"),
					new SimulatorDevice("iPhone 5s (DE72B92C-D8E2-4CCE-A5E1-9174B6A03209) (Shutdown)"),
					new SimulatorDevice("iPhone 6 (CD613910-6F4E-41A3-B0CB-1EBC16449F42) (Shutdown)"),
					new SimulatorDevice("iPhone 6 Plus (364A3F52-DED2-4BBD-B40A-B0F3F374E51F) (Shutdown)"),
					new SimulatorDevice("iPhone 6s (A3F2EB56-1EA8-4527-8BA3-BEE76AAB6D01) (Shutdown)"),
					new SimulatorDevice("iPhone 6s Plus (3EACF222-830E-4BF1-ADB3-75E98AB99480) (Shutdown)"),
					new SimulatorDevice("iPad 2 (D72F7CC6-8426-4E0A-A234-34747B1F30DD) (Shutdown)"),
					new SimulatorDevice("iPad Retina (DAE7925F-8FC7-42B0-A1F0-7173C3F40114) (Shutdown)"),
					new SimulatorDevice("iPad Air (4F432AFB-370C-4741-B7D7-803F3E223C36) (Shutdown)"),
					new SimulatorDevice("iPad Air 2 (8064C333-D8F0-43A7-83B4-DFA79071A870) (Shutdown)"),
					new SimulatorDevice("iPad Pro (744F7B28-373D-4666-B4DF-8438D1109663) (Shutdown)")
	]

	def destinations = [
	        new Destination("iOS Simulator", "iPhone 4s", "iOS 9"),
	]

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin:org.openbakery.XcodePlugin

		projectDir.mkdirs()

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_START_TASK_NAME)
		task.simulatorControl = simulatorControl
		task.destinationResolver = destinationResolver

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "create"() {
		expect:
		task instanceof SimulatorStartTask
		task.simulatorControl instanceof SimulatorControl
	}


	def "run"() {
		given:
		simulatorControl.getDevice(_) >> Optional.ofNullable(devices9_1[0])
		destinationResolver.getDestinations(_) >> destinations

		when:
		task.run()


		then:
		1 *simulatorControl.killAll()
		1 *simulatorControl.runDevice(devices9_1[0])
		1 *simulatorControl.waitForDevice(devices9_1[0])

	}


	def "run with specified device"() {
		given:

		destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode7.txt"))
		task.destinationResolver = destinationResolver

		Destination destination
		project.xcodebuild.destination = 'iPhone 6s'

		when:
		task.run()

		then:
		1 *simulatorControl.getDevice((Destination)_) >> {	arguments ->
			destination = arguments[0]
			return devices9_1[0]
		}
		destination != null
		destination.name == 'iPhone 6s'

	}

}
