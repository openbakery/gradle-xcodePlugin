package org.openbakery.stubs

import org.openbakery.Version
import org.openbakery.tools.Xcode

/**
 * Created by rene on 27.06.16.
 */
class XcodeFake extends Xcode {


	public XcodeFake() {
		super(null)
	}

	Version getVersion() {
		return new Version("7.3.1")
	}

	String getPath() {
		return "/Applications/Xcode.app"
	}

	String getXcodebuild() {
		return "xcodebuild"
	}

}
