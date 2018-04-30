package org.openbakery.packaging

import groovy.transform.CompileStatic
import org.gradle.api.tasks.*
import org.openbakery.AbstractXcodeBuildTask
import org.openbakery.XcodePlugin
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.signing.SigningMethod
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Xcodebuild

import java.util.Optional

@CompileStatic
class PackageTaskIosAndTvOS extends AbstractXcodeBuildTask {

	private ProvisioningProfileReader reader

	public static final String TASK_NAME = "package"

	private static final String XC_ARCHIVE_EXTENSION = ".xcarchive"
	private static final String PLIST_KEY_METHOD = "method"
	private static final String PLIST_KEY_COMPILE_BITCODE = "compileBitcode"
	private static final String PLIST_KEY_PROVISIONING_PROFILE = "provisioningProfiles"
	private static final String PLIST_KEY_SIGNING_CERTIFICATE = "signingCertificate"

	PackageTaskIosAndTvOS() {
		super()

		dependsOn(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME)
		dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)

		finalizedBy(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME)
	}

	@Input
	String getOptionalBundleIdentifier() {
		return Optional.ofNullable(super.getBundleIdentifier())
				.orElseThrow { new IllegalArgumentException("Invalid bundle identifier") }
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
		return new File(PathHelper.resolveArchiveFolder(project),
				getScheme() + XC_ARCHIVE_EXTENSION)
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
				.orElseThrow {
			new IllegalArgumentException("Cannot resolve a valid provisioning " +
					"profile for bundle identifier : " +
					getOptionalBundleIdentifier())
		}, commandRunner)
	}

	private void generateExportOptionPlist() {
		File file = getExportOptionsPlistFile()

		plistHelper.create(file)
		plistHelper.addValueForPlist(file,
				PLIST_KEY_METHOD,
				getMethod().value)

		// provisioning profiles
		HashMap<String, String> map = new HashMap<>()
		map.put(reader.getApplicationIdentifier(), reader.getName())
		plistHelper.addDictForPlist(file,
				PLIST_KEY_PROVISIONING_PROFILE,
				map)

		// certificate
		plistHelper.addValueForPlist(file,
				PLIST_KEY_SIGNING_CERTIFICATE,
				getSignatureFriendlyName())

		// BitCode should be compiled only for AppStore builds
		plistHelper.addValueForPlist(file,
				PLIST_KEY_COMPILE_BITCODE,
				getMethod() == SigningMethod.AppStore)
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
