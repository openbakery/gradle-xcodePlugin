package org.openbakery.tools

import org.openbakery.CommandRunner
import org.openbakery.Version

/**
 * Created by rene on 27.06.16.
 */
class Xcodebuild {

	CommandRunner commandRunner

	String xcodePath
	Xcode xcode


	public Xcodebuild(CommandRunner commandRunner, Xcode xcode) {
		this.commandRunner = commandRunner
		this.xcode = xcode
	}


}
