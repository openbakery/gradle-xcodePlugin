package org.openbakery.testdouble

import org.openbakery.CommandRunner
import org.openbakery.test.XCResultTool
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode

class XcodeFake extends Xcode {

	private String path = "/Applications/Xcode.app"

	private String versionString
	private String xcodeVersionString

	public XcodeFake(String versionString) {
		super(new CommandRunner())
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
			"/Applications/Xcode-12.2.app"
	}

	Version getXcodeVersion(String xcodeBuildCommand) {
		if (xcodeBuildCommand.startsWith("/Applications/Xcode-11.7.app")) {
			return new Version("11.7.0")
		}
		return new Version(versionString)
	}


	Map<String, String> getXcodeSelectEnvironmentValue(String version) {
		File file = new File("/Applications/Xcode-${getVersion().major}.app", XCODE_CONTENT_DEVELOPER)
		HashMap<String, String> result = new HashMap<String, String>()
		result.put(DEVELOPER_DIR, file.absolutePath)
		return result
	}

	@Override
	public String toString() {
		return "XcodeFake{" +
			"path='" + path + '\'' +
			", versionString='" + versionString + '\'' +
			", xcodeVersionString='" + xcodeVersionString + '\'' +
			'}';
	}

	@Override
	XCResultTool getXCResultTool() {
		return new XCResultToolFake()
	}
}
