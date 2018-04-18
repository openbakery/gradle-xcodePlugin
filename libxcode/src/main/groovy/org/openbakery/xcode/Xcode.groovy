package org.openbakery.xcode

import org.openbakery.CommandRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Xcode {
	private static Logger logger = LoggerFactory.getLogger(Xcode.class)


	CommandRunner commandRunner

	String xcodePath
	Version version = null

	public static final String XCODE_CONTENT_DEVELOPER = "Contents/Developer"
	public static final String ENV_DEVELOPER_DIR = "DEVELOPER_DIR"
	public static final String XCODE_CONTENT_XCODEBUILD = "Contents/Developer/usr/bin/xcodebuild"

	public Xcode(CommandRunner commandRunner) {
		this(commandRunner, null)
	}

	public Xcode(CommandRunner commandRunner, String version) {
		logger.debug("create xcode with version {}", version)
		this.commandRunner = commandRunner
		if (version != null) {
			setVersionFromString(version)
		}
	}

	/**
	 * Provide the environments values to provide to the command line runner to select
	 * a Xcode version without using `xcode-select -s` who requires `sudo`.
	 *
	 * @param version : The required Xcode version
	 * @return A map of environment variables to pass to the command runner
	 */
	Map<String, String> getXcodeSelectEnvValue(String version) {
		setVersionFromString(version)

		HashMap<String, String> result = new HashMap<String, String>()
		result.put(ENV_DEVELOPER_DIR, new File(xcodePath, XCODE_CONTENT_DEVELOPER).absolutePath)
		return result
	}

	void setVersionFromString(String version) {
		Optional<Version> requiredVersion = Optional.ofNullable(version)
				.map { new Version(it) }

		String installedXcodes = commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode")

		Iterator<File> files = installedXcodes.split("\n").iterator()
				.collect { new File(it, XCODE_CONTENT_XCODEBUILD) }
				.findAll { it.exists() }
				.iterator()

		for (File xcodeBuildFile : files) {
			Version xcodeVersion = getXcodeVersion(xcodeBuildFile.absolutePath)
			if (xcodeVersion.suffix != null && requiredVersion.get().suffix != null) {
				if (xcodeVersion.suffix.equalsIgnoreCase(requiredVersion.get().suffix)) {
					xcodePath = new File(xcodeBuildFile.absolutePath - XCODE_CONTENT_XCODEBUILD)
					this.version = xcodeVersion
					return
				}
			} else if (xcodeVersion.toString().startsWith(requiredVersion.get().toString())) {
				xcodePath = new File(xcodeBuildFile.absolutePath - XCODE_CONTENT_XCODEBUILD)
				this.version = xcodeVersion
				return
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

	@Override
	public String toString() {
		return "Xcode{" +
				"xcodePath='" + xcodePath + '\'' +
				", version=" + version +
				'}';
	}

}
