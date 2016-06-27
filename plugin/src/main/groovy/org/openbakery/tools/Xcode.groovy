package org.openbakery.tools

import org.openbakery.CommandRunner
import org.openbakery.Version

/**
 * Created by rene on 27.06.16.
 */
class Xcode {


	CommandRunner commandRunner

	String xcodePath
	Version version = null


	public Xcode(CommandRunner commandRunner) {
		this(commandRunner, null)
	}

	public Xcode(CommandRunner commandRunner, String version) {
		this.commandRunner = commandRunner
		if (version != null) {
			setVersionFromString(version)
		}
	}

	void setVersionFromString(String version) {
		Version versionToCompare = new Version(version)
		String installedXcodes = commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode")


		for (String xcode : installedXcodes.split("\n")) {
			File xcodeBuildFile = new File(xcode, "Contents/Developer/usr/bin/xcodebuild");
			if (xcodeBuildFile.exists()) {
				Version xcodeVersion = getXcodeVersion(xcodeBuildFile.absolutePath)
				if (xcodeVersion.suffix != null && versionToCompare.suffix != null) {
					if (xcodeVersion.suffix.equalsIgnoreCase(versionToCompare.suffix)) {
						xcodePath = xcode
						this.version = xcodeVersion
						return
					}
				} else if (xcodeVersion.toString().startsWith(versionToCompare.toString())) {
					xcodePath = xcode
					this.version = xcodeVersion
					return
				}
			}
		}
		throw new IllegalStateException("No Xcode found with build number " + version);

	}

	Version getXcodeVersion(String xcodebuildCommand) {
		String xcodeVersion = commandRunner.runWithResult(xcodebuildCommand, "-version");

		def VERSION_PATTERN = ~/Xcode\s([^\s]*)\nBuild\sversion\s([^\s]*)/
		def matcher = VERSION_PATTERN.matcher(xcodeVersion)
		if (matcher.matches()) {
			Version version = new Version(matcher[0][1])
			version.suffix = matcher[0][2]
			return version
		}
		return null
	}

	Version getVersion() {
		if (this.version == null) {
			this.version = getXcodeVersion(getXcodebuild())
		}
		return this.version
	}

	String getPath() {
		if (xcodePath == null) {
			String result = commandRunner.runWithResult("xcode-select", "-p")
			xcodePath = result - "/Contents/Developer"
		}
		return xcodePath
	}


	String getXcodebuild() {
		if (xcodePath != null) {
			return xcodePath + "/Contents/Developer/usr/bin/xcodebuild"
		}
		return "xcodebuild"
	}

	String getAltool() {
		return getPath() + "/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool"
	}

	String getXcrun() {
		return getPath() + "/Contents/Developer/usr/bin/xcrun"
	}

	String getSimctl() {
		return getPath() + "/Contents/Developer/usr/bin/simctl"
	}
}
