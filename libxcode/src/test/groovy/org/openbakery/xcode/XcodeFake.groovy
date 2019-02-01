package org.openbakery.xcode

import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode

/**
 * User: rene
 * Date: 21/10/16
 */
class XcodeFake extends Xcode {

	private String path = "/Applications/Xcode.app"

	private String versionString

	public XcodeFake(String versionString) {
		super(null)
		this.versionString = versionString
	}

	public XcodeFake() {
		this("7.3.1")
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
