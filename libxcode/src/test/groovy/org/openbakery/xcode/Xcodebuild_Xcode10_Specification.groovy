package org.openbakery.xcode

import org.openbakery.CommandRunner
import org.openbakery.output.OutputAppender
import org.openbakery.testdouble.SimulatorControlFake
import spock.lang.Specification
import org.openbakery.testdouble.XcodeFake

class Xcodebuild_Xcode10_Specification extends Specification {

	//Project project

	CommandRunner commandRunner = Mock(CommandRunner)

	Xcodebuild xcodebuild

	OutputAppender outputAppender = new OutputAppender() {
		public void append(String output) {
		}
	}
	XcodebuildParameters parameters
	DestinationResolver destinationResolver

	def setup() {
		parameters = new XcodebuildParameters()
		parameters.dstRoot = new File("build/dst")
		parameters.objRoot = new File("build/obj")
		parameters.symRoot = new File("build/sym")
		parameters.sharedPrecompsDir = new File("build/shared")
		parameters.derivedDataPath = new File("build/derivedData")
		parameters.configuration = "Debug"
		parameters.simulator = true
		parameters.type = Type.iOS
		parameters.destination = [
						new Destination("iPhone XR")
		]
// iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389
		destinationResolver = new DestinationResolver(new SimulatorControlFake("simctl-list-xcode10.txt"))
		xcodebuild = new Xcodebuild(new File("buildDirectory"), commandRunner, new XcodeFake("10.0"), parameters, destinationResolver.getDestinations(parameters))
		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false
	}

	def cleanup() {
	}

	def addDerivedDataPathParameters(def command) {
		command << "-derivedDataPath" << new File("build/derivedData").absolutePath
	}

	def addDefaultDirectoriesParameters(def command) {
		command << "DSTROOT=" + new File("build/dst").absolutePath
		command << "OBJROOT=" + new File("build/obj").absolutePath
		command << "SYMROOT=" + new File("build/sym").absolutePath
		command << "SHARED_PRECOMPS_DIR=" + new File("build/shared").absolutePath
	}

	def createCommandWithDisabledCodesign(String... commands) {
		def command = []
		command.addAll(commands)
		addDisabledCodesigningParameters(command)
		return command
	}

	def addDisabledCodesigningParameters(def command) {
		command << "CODE_SIGN_IDENTITY="
		command << "CODE_SIGNING_REQUIRED=NO"
		command << "CODE_SIGNING_ALLOWED=NO"
	}

	def "test command for iOS device has parallel testing disabled"() {
			def commandList
			def expectedCommandList

			def destination = new Destination()
			destination.id = '93552145-0476-432B-B9E4-99BDF458F557'

			parameters.destination  = [ destination ]
			parameters.simulator = false


			when:
			xcodebuild.executeTest(outputAppender, null)

			then:
			1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


			interaction {
				expectedCommandList = createCommandWithDisabledCodesign('script', '-q', '/dev/null',
						"xcodebuild",
						"-scheme", 'myscheme',
						"-workspace", "myworkspace",
						"-configuration", 'Debug')

				expectedCommandList << "-destination" << "platform=iOS Simulator,id=93552145-0476-432B-B9E4-99BDF458F557"
				addDerivedDataPathParameters(expectedCommandList)
				addDefaultDirectoriesParameters(expectedCommandList)
				expectedCommandList << "-enableCodeCoverage" << "yes"
			}
			Collections.indexOfSubList(commandList, expectedCommandList) == 0
			commandList.removeLast() == 'test'
			commandList.removeLast() == '-disable-concurrent-destination-testing'
		}

}
