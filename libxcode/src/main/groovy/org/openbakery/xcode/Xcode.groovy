package org.openbakery.xcode

import groovy.transform.CompileStatic
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting
import org.openbakery.CommandRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class Xcode {
	private Version version = null
	private String xcodePath

	@VisibleForTesting
	private CommandRunner commandRunner

	public static final String DEVELOPER_DIR = "DEVELOPER_DIR"
	public static final String XCODE_ACTION_XC_SELECT = "xcode-select"
	public static final String XCODE_CONTENT_DEVELOPER = "Contents/Developer"
	public static final String XCODE_CONTENT_XC_RUN = "/$XCODE_CONTENT_DEVELOPER/usr/bin/xcrun"
	public static final String XCODE_CONTENT_XCODE_BUILD = "$XCODE_CONTENT_DEVELOPER/usr/bin/xcodebuild"

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

	CommandRunner getCommandRunner() {
		return commandRunner
	}

	/**
	 * Provide the environments values to provide to the command line runner to select
	 * a Xcode version without using `xcode-select -s` who requires `sudo`.
	 *
	 * @param version : The required Xcode version
	 * @return A map of environment variables to pass to the command runner
	 */
	Map<String, String> getXcodeSelectEnvironmentValue(String version) {
		setVersionFromString(version)
		File file = new File(xcodePath, XCODE_CONTENT_DEVELOPER)
		HashMap<String, String> result = new HashMap<String, String>()
		if (file.exists()) {
			result.put(DEVELOPER_DIR, file.absolutePath)
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
			.findAll {
				it.exists()
			}
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
		logger.debug("selectXcode {}", file)
		String absolutePath = file.absolutePath
		Version xcodeVersion = getXcodeVersion(absolutePath)
		xcodePath = new File(absolutePath - XCODE_CONTENT_XCODE_BUILD)
		this.version = xcodeVersion
		logger.debug("Xcode version is {}", this.version)
	}

	String resolveInstalledXcodeVersionsList() {
		return commandRunner.runWithResult("mdfind",
			"kMDItemCFBundleIdentifier=com.apple.dt.Xcode")
	}

	Version getXcodeVersion(String xcodeBuildCommand) {
		String xcodeVersion = commandRunner.runWithResult(xcodeBuildCommand,
			"-version")

		if (xcodeVersion == null) {
			return new Version("0")
		}

		Matcher matcher = VERSION_PATTERN.matcher(xcodeVersion)
		if (matcher.find()) {
			def versionString = matcher.group(1)
			logger.debug("versionString {}", versionString)
			Version version = new Version(versionString)
			version.suffix = matcher.group(2)
			return version
		}
		return new Version("0")
	}

	Version getVersion() {
		if (this.version == null) {
			this.version = getXcodeVersion(getXcodebuild())
		}
		return this.version
	}

	String getPath() {
		if (xcodePath == null) {
			String result = commandRunner.runWithResult(XCODE_ACTION_XC_SELECT
				, "-p")
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
		if (this.getVersion().major < 11) {
			return getPath() + "/Contents/Applications/Application Loader" +
           ".app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool"
		}
		return getPath() + "/$XCODE_CONTENT_DEVELOPER/usr/bin/altool"
	}

	String getXcrun() {
		return getPath() + XCODE_CONTENT_XC_RUN
	}

	String getXcresulttool() {
		def xcresulttoolPath = getPath() + "/$XCODE_CONTENT_DEVELOPER/usr/bin/xcresulttool"
		if(new File(xcresulttoolPath).exists()) {
			return xcresulttoolPath
		}
		return "xcrun xcresulttool"
	}

	String getSimctl() {
		return getPath() + "/$XCODE_CONTENT_DEVELOPER/usr/bin/simctl"
	}

	// returns the default toolchain directory
	// if you want to get the project specific toolchain directory use the method in Xcodebuild
	String getToolchainDirectory() {
		return getPath() + "/$XCODE_CONTENT_DEVELOPER/Toolchains/XcodeDefault.xctoolchain"
	}

	String getBuildVersion() {
		return getVersion().suffix
	}

	@Override
	String toString() {
		return "Xcode{" +
			"xcodePath='" + xcodePath + '\'' +
			", version=" + version +
			'}'
	}
}
