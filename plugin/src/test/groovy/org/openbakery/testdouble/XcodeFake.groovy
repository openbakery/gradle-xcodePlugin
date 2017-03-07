package org.openbakery.testdouble

import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode

class XcodeFake extends Xcode {

	private String path = "/Applications/Xcode.app"

	private versionString = "7.3.1"


	public XcodeFake() {
		super(null)
	}

	Version getVersion() {
		return new Version(versionString)
	}

	String getPath() {
		return path
	}

	String getXcodebuild() {
		return "xcodebuild"
	}

}
