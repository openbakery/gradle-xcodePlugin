package org.openbakery

import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.openbakery.XcodeService.XcodeApp
import org.openbakery.xcode.Version

import javax.inject.Inject
import java.util.regex.Matcher
import java.util.regex.Pattern

class XcodeService {

	final Property<CommandRunner> commandRunnerProperty
	final ListProperty<XcodeApp> installedXcodes

	private static final String CONTENT_DEVELOPER = "Contents/Developer"
	private static final String CONTENT_XCODE_BUILD = "$CONTENT_DEVELOPER/usr/bin/xcodebuild"
	private static final Pattern VERSION_PATTERN = ~/Xcode\s([^\s]*)\nBuild\sversion\s([^\s]*)/

	@Inject
	XcodeService(Project project) {
		commandRunnerProperty = project.objects.property(CommandRunner)

		installedXcodes = project.objects.listProperty(XcodeApp)
		installedXcodes.set(commandRunnerProperty.map(new Transformer<List<XcodeApp>, CommandRunner>() {
			@Override
			List<XcodeApp> transform(CommandRunner commandRunner) {
				return commandRunner.runWithResult("mdfind",
						"kMDItemCFBundleIdentifier=com.apple.dt.Xcode")
						.split("\n")
						.collect { new File(it) }
						.collect { new XcodeApp(it, getXcodeVersion(commandRunner, it)) }
			}
		}))
	}

	public XcodeApp getInstallationForVersion(final String version) {
		return installedXcodes.map(new Transformer<XcodeApp, List<XcodeApp>>() {
			@Override
			XcodeApp transform(List<XcodeApp> xcodeApps) {
				return xcodeApps.find { it.version.toString().startsWith(version) }
			}
		}).getOrNull()
	}

	private Version getXcodeVersion(CommandRunner commandRunner,
									File file) {
		String xcodeVersion = commandRunner.runWithResult(
				new File(file, CONTENT_XCODE_BUILD).absolutePath,
				"-version")

		Matcher matcher = VERSION_PATTERN.matcher(xcodeVersion)
		if (matcher.matches()) {
			Version version = new Version(matcher.group(1))
			version.suffix = matcher.group(2)

			return version
		}

		return null
	}

	static class XcodeApp implements Serializable {
		private final File file
		private final Version version

		XcodeApp(File file,
				 Version version) {
			this.file = file
			this.version = version
		}

		File getFile() {
			return file
		}

		File getContentXcodeBuildFile() {
			return new File(file, CONTENT_XCODE_BUILD)
		}

		File getContentDeveloperFile() {
			return new File(file, CONTENT_DEVELOPER)
		}

		Version getVersion() {
			return version
		}
	}

}
