package org.openbakery.tools

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import spock.lang.Specification

/**
 * Created by rene on 27.06.16.
 */
class XcodebuildSpecification extends Specification {

	//Project project

	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode xcode = Mock(Xcode)

	Xcodebuild xcodebuild




	def setup() {
		xcodebuild = new Xcodebuild(commandRunner, xcode)
	}

	def cleanup() {
	}


	def "xcodebuild has command runner"() {
		expect:
		xcodebuild.commandRunner != null
	}


	def "xcodebuild has xcode "() {
		expect:
		xcodebuild.xcode != null
	}


}
