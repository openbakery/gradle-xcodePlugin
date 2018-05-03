package org.openbakery.packaging

import groovy.transform.CompileStatic
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.openbakery.AbstractXcodeBuildTask
import org.openbakery.XcodePlugin
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.signing.SigningMethod
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild

import java.util.Optional

@CompileStatic
class PackageTaskIosAndTvOS extends AbstractXcodeBuildTask {

	private ProvisioningProfileReader reader

	public static final String NAME = "packageWithXcodeBuild"

	private static final String PLIST_KEY_METHOD = "method"
	private static final String PLIST_KEY_SIGNING_STYLE = "signingStyle"
	private static final String PLIST_KEY_COMPILE_BITCODE = "compileBitcode"
	private static final String PLIST_KEY_PROVISIONING_PROFILE = "provisioningProfiles"
	private static final String PLIST_KEY_SIGNING_CERTIFICATE = "signingCertificate"
	private static final String PLIST_VALUE_SIGNING_METHOD_MANUAL = "manual"


	PackageTaskIosAndTvOS() {
		super()

		dependsOn(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME)
		dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)

		finalizedBy(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME)

		onlyIf(new Spec<Task>() {
			@Override
			boolean isSatisfiedBy(Task task) {
				return getXcodeExtension().getType() == Type.iOS ||
						getXcodeExtension().getType() == Type.tvOS
			}
		})
	}

	@Input
	boolean getBitCode() {
		boolean result = getXcodeExtension().bitcode
		SigningMethod method = getMethod()

		if (method == SigningMethod.AppStore
				&& getXcodeExtension().type == Type.tvOS) {
			assert result: "Invalid configuration for the TvOS target " +
					"`AppStore` upload requires BitCode enabled."
		} else if (method != SigningMethod.AppStore) {
			assert !result: "The BitCode setting (`xcodebuild.bitcode`) should be enabled only " +
					"for the `AppStore` signing method"
		}

		return result
	}

	@Input
	String getBundleIdentifier() {
		return super.getBundleIdentifier()
	}

	@Input
	SigningMethod getMethod() {
		return Optional.ofNullable(getXcodeExtension().getSigning().method)
				.orElseThrow { new IllegalArgumentException("Invalid signing method") }
	}

	@Input
	String getScheme() {
		return Optional.ofNullable(getXcodeExtension().scheme)
				.orElseThrow { new IllegalArgumentException("Invalid signing method") }
	}

	@InputDirectory
	File getArchiveFile() {
		return PathHelper.resolveArchiveFile(project, scheme)
	}

	@OutputDirectory
	File getOutputDirectory() {
		File file = PathHelper.resolvePackageFolder(project)
		file.mkdirs()
		return file
	}

	@OutputFile
	File getExportOptionsPlistFile() {
		return new File(project.buildDir, "exportOptions.plist")
	}

	@TaskAction
	private void packageArchive() {
		assert getArchiveFile().exists() && getArchiveFile().isDirectory()
		setupProvisioningProfileReader()
		generateExportOptionPlist()
		packageIt()
	}

	private void setupProvisioningProfileReader() {
		reader = new ProvisioningProfileReader(getProvisioningFile()
				, commandRunner)
	}

	private void generateExportOptionPlist() {
		File file = getExportOptionsPlistFile()

		plistHelper.create(file)

		// Signing  method
		addStringValueForPlist(PLIST_KEY_METHOD,
				getMethod().value)

		// Provisioning profiles map list
		HashMap<String, String> map = new HashMap<>()
		map.put(reader.getApplicationIdentifier(), reader.getName())
		plistHelper.addDictForPlist(file,
				PLIST_KEY_PROVISIONING_PROFILE,
				map)

		// Certificate name
		addStringValueForPlist(PLIST_KEY_SIGNING_CERTIFICATE,
				getSignatureFriendlyName())

		// BitCode
		plistHelper.addValueForPlist(file,
				PLIST_KEY_COMPILE_BITCODE,
				getBitCode())

		// SigningMethod
		addStringValueForPlist(PLIST_KEY_SIGNING_STYLE,
				PLIST_VALUE_SIGNING_METHOD_MANUAL)
	}

	private void addStringValueForPlist(String key,
										String value) {
		assert key != null
		assert value != null

		plistHelper.addValueForPlist(getExportOptionsPlistFile(),
				key,
				value)
	}

	private void packageIt() {
		Xcodebuild xcodeBuild = new Xcodebuild(project.projectDir,
				commandRunner,
				xcode,
				parameters,
				getDestinations())

		xcodeBuild.packageIpa(getArchiveFile(),
				getOutputDirectory(),
				getExportOptionsPlistFile())
	}
}
