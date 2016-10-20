package org.openbakery.testdouble

import org.apache.commons.io.FileUtils
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode

/**
 * Created by rene on 27.06.16.
 */
class XcodeFake extends Xcode {

	String path = "/Applications/Xcode.app"
	String toolchainDirectory = null

	String versionString = "7.3.1"


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

	String loadBuildSettings() {
		File buildSettings = new File("src/test/Resource/xcodebuild-showBuildSettings.txt");
		return FileUtils.readFileToString(buildSettings)
	}

	@Override
	String getToolchainDirectory() {
		if (toolchainDirectory == null) {
			toolchainDirectory = new File(path, "Contents/Developer/Toolchains/XcodeDefault.xctoolchain").absolutePath
		}
		return toolchainDirectory
	}
}
