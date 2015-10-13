package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import spock.lang.Specification

/**
 * Created by rene on 07.09.15.
 */
class SimulatorControlSpecification extends Specification {

	Project project
	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner);

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin:org.openbakery.XcodePlugin

		project.xcodebuild.xcodePath = '/Applications/Xcode.app'

		projectDir.mkdirs()

		simulatorControl = new SimulatorControl(project)
		simulatorControl.commandRunner = commandRunner

		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/simctl", "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-unavailable-output.txt"))
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "get runtimes"() {

		expect:
		List<Runtime> runtimes = simulatorControl.getRuntimes()

		runtimes != null
		runtimes.size() == 2

		SimulatorRuntime runtime = runtimes.get(0)
		runtime.name.equals("iOS 7.1")
		runtime.version.equals("7.1")
		runtime.buildNumber.equals("11D167")
		runtime.identifier.equals("com.apple.CoreSimulator.SimRuntime.iOS-7-1")
	}




	def "launch simulator"() {

		when:

		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));
		simulatorControl.runDevice(devices.get(0))

		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Developer/usr/bin/instruments", "-w", "iPhone 4s (7.1 Simulator)"])

	}



}


