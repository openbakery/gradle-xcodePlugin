package org.openbakery.packaging

import groovy.transform.CompileStatic
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.openbakery.AbstractXcodeBuildTask
import org.openbakery.XcodePlugin
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.signing.SigningMethod
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild

import java.util.Optional

@CompileStatic
class PackageTaskIosAndTvOS extends AbstractXcodeBuildTask {

	private ProvisioningProfileReader reader

	public static final String DESCRIPTION = "Package the IPA from the generate archive by using Xcodebuild"
	public static final String NAME = "packageWithXcodeBuild"

	private static final String PLIST_KEY_METHOD = "method"
	private static final String PLIST_KEY_SIGNING_STYLE = "signingStyle"
	private static final String PLIST_KEY_COMPILE_BITCODE = "compileBitcode"
	private static final String PLIST_KEY_PROVISIONING_PROFILE = "provisioningProfiles"
	private static final String PLIST_KEY_SIGNING_CERTIFICATE = "signingCertificate"
	private static final String PLIST_VALUE_SIGNING_METHOD_MANUAL = "manual"
	private static final String FILE_EXPORT_OPTIONS_PLIST = "exportOptions.plist"

	PackageTaskIosAndTvOS() {
		super()

		description = DESCRIPTION

		dependsOn(KeychainCreateTask.TASK_NAME)
		dependsOn(ProvisioningInstallTask.TASK_NAME)
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

	@Override
	File getProvisioningFile() {
		return super.getProvisioningFile()
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
	@Override
	String getSignatureFriendlyName() {
		return super.getSignatureFriendlyName()
	}

	@Input
	SigningMethod getMethod() {
		return Optional.ofNullable(getXcodeExtension().getSigning().method)
				.orElseThrow { new IllegalArgumentException("Invalid signing method") }
	}

	@Input
	String getScheme() {
		return Optional.ofNullable(getXcodeExtension().scheme)
				.orElseThrow { new IllegalArgumentException("Invalid scheme") }
	}

	@Input
	Map<String, String> getProvisioningMap() {
		setupProvisioningProfileReader()

		HashMap<String, String> map = new HashMap<>()
		map.put(reader.getApplicationIdentifier(), reader.getName())
		return map
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
		return new File(project.buildDir, FILE_EXPORT_OPTIONS_PLIST)
	}

	@TaskAction
	private void packageArchive() {
		assert getArchiveFile().exists() && getArchiveFile().isDirectory()
		generateExportOptionPlist()
		packageIt()
	}

	private void setupProvisioningProfileReader() {
		if (reader == null) {
			reader = new ProvisioningProfileReader(getProvisioningFile(),
					commandRunner)
		}
	}

	private void generateExportOptionPlist() {
		File file = getExportOptionsPlistFile()

		plistHelper.create(file)

		// Signing  method
		addStringValueForPlist(PLIST_KEY_METHOD,
				getMethod().value)

		// Provisioning profiles map list
		plistHelper.addDictForPlist(file,
				PLIST_KEY_PROVISIONING_PROFILE,
				getProvisioningMap())

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
