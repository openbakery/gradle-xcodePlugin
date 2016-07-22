package org.openbakery

import org.gradle.api.DefaultTask
import org.openbakery.tools.Xcode

/**
 * User: rene
 * Date: 15.07.13
 * Time: 11:57
 */
abstract class AbstractXcodeBuildTask extends DefaultTask {

	CommandRunner commandRunner
	Xcode xcode

	AbstractXcodeBuildTask() {
		super()
		commandRunner = new CommandRunner()
		xcode = new Xcode(commandRunner, project.xcodebuild.xcodeVersion)
	}


}
