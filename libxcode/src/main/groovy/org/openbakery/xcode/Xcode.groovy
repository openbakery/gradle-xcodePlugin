package org.openbakery.xcode

import groovy.transform.CompileStatic
import org.openbakery.CommandRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class Xcode {
	private Version version = null
	private String xcodePath

	private final CommandRunner commandRunner

	public static final String XCODE_CONTENT_DEVELOPER = "Contents/Developer"
	public static final String ENV_DEVELOPER_DIR = "DEVELOPER_DIR"
	public static final String XCODE_CONTENT_XC_RUN = "/$XCODE_CONTENT_DEVELOPER/usr/bin/xcrun"
	public static
	final String XCODE_CONTENT_XCODE_BUILD = "$XCODE_CONTENT_DEVELOPER/usr/bin/xcodebuild"

	private final Logger logger = LoggerFactory.getLogger(Xcode.class)

	private static final Pattern VERSION_PATTERN = ~/Xcode\s([^\s]*)\nBuild\sversion\s([^\s]*)/

	Xcode(CommandRunner commandRunner) {
		this(commandRunner, null)
	}

	Xcode(CommandRunner commandRunner, String version) {
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
		File file = new File(xcodePath, XCODE_CONTENT_DEVELOPER)
		HashMap<String, String> result = new HashMap<String, String>()
		if (file.exists()) {
			result.put(ENV_DEVELOPER_DIR, file.absolutePath)
		}
		return result
	}

	void setVersionFromString(String version) throws IllegalArgumentException {
		if (version == null) {
			throw new IllegalArgumentException()
		}

		final Version requiredVersion = new Version(version)

		Optional<File> result = Optional.ofNullable(resolveInstalledXcodeVersionsList()
				.split("\n")
				.iterator()
				.collect { new File(it as File, XCODE_CONTENT_XCODE_BUILD) }
				.findAll { it.exists() }
				.find {
					Version candidate = getXcodeVersion(it.absolutePath)

					boolean versionStartWith = candidate.toString()
							.startsWith(requiredVersion.toString())

					boolean versionHasSuffix = (candidate.suffix != null
							&& requiredVersion.suffix != null
							&& candidate.suffix.equalsIgnoreCase(requiredVersion.suffix))

					return versionHasSuffix || versionStartWith
				})

		if (result.isPresent()) {
			selectXcode(result.get())
		} else {
			throw new IllegalStateException("No Xcode found with build number " + version)
		}
	}

	void selectXcode(File file) {
		String absolutePath = file.absolutePath
		Version xcodeVersion = getXcodeVersion(absolutePath)
		xcodePath = new File(absolutePath - XCODE_CONTENT_XCODE_BUILD)
		this.version = xcodeVersion
	}

	String resolveInstalledXcodeVersionsList() {
		return commandRunner.runWithResult("mdfind",
				"kMDItemCFBundleIdentifier=com.apple.dt.Xcode")
	}

	Version getXcodeVersion(String xcodeBuildCommand) {
		String xcodeVersion = commandRunner.runWithResult(xcodeBuildCommand,
				"-version")

		Matcher matcher = VERSION_PATTERN.matcher(xcodeVersion)
		if (matcher.matches()) {
			Version version = new Version(matcher.group(1))
			version.suffix = matcher.group(2)
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
			xcodePath = result - "/$XCODE_CONTENT_DEVELOPER"
		}
		return xcodePath
	}

	String getXcodebuild() {
		if (xcodePath != null) {
			return new File(xcodePath, XCODE_CONTENT_XCODE_BUILD).absolutePath
		}
		return "xcodebuild"
	}

	String getAltool() {
		return getPath() + "/Contents/Applications/Application Loader" +
				".app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool"
	}

	String getXcrun() {
		return getPath() + XCODE_CONTENT_XC_RUN
	}

	String getSimctl() {
		return getPath() + "/$XCODE_CONTENT_DEVELOPER/usr/bin/simctl"
	}

	@Override
	String toString() {
		return "Xcode{" +
				"xcodePath='" + xcodePath + '\'' +
				", version=" + version +
				'}'
	}
}
