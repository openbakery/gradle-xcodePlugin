package org.openbakery.testdouble

import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode

class XcodeFake extends Xcode {

	private String path = "/Applications/Xcode.app"

	private String versionString
	private String xcodeVersionString

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


	String resolveInstalledXcodeVersionsList() {
		return "/Applications/Xcode-11.7.app\n" +
			"/Applications/Xcode-12.app"
	}

	Version getXcodeVersion(String xcodeBuildCommand) {
		if (xcodeBuildCommand.startsWith("/Applications/Xcode-11.7.app")) {
			return new Version("11.7.0")
		}
		return new Version(versionString)
	}

}
