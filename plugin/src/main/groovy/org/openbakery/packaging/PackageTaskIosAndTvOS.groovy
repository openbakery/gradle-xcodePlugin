package org.openbakery.packaging

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.openbakery.AbstractXcodeBuildTask
import org.openbakery.XcodePlugin
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningFile
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.signing.SigningMethod
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild

@CompileStatic
class PackageTaskIosAndTvOS extends AbstractXcodeBuildTask {

	@Input
	public final Property<SigningMethod> signingMethod = project.objects.property(SigningMethod)

	@Input
	final Provider<String> scheme = project.objects.property(String)

	@Input
	final Provider<Type> buildType = project.objects.property(Type)

	@Input
	final ListProperty<ProvisioningFile> registeredProvisioningFiles = project.objects.listProperty(ProvisioningFile)

	@Input
	final Property<String> certificateFriendlyName = project.objects.property(String)

	private ProvisioningProfileReader reader

	public static final String DESCRIPTION = "Package the archive with Xcode-build"
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
		SigningMethod method = signingMethod.get()

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
	@CompileStatic(TypeCheckingMode.SKIP)
	Map<String, String> getProvisioningMap() {
		return registeredProvisioningFiles.get()
				.collectEntries { [it.getApplicationIdentifier(), it.getName()] }
	}

	@InputDirectory
	File getArchiveFile() {
		return PathHelper.resolveArchiveFile(project, scheme.get())
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

	private void generateExportOptionPlist() {
		File file = getExportOptionsPlistFile()

		plistHelper.create(file)

		// Signing  method
		addStringValueForPlist(PLIST_KEY_METHOD,
				signingMethod.get().value)

		// Provisioning profiles map list
		plistHelper.addDictForPlist(file,
				PLIST_KEY_PROVISIONING_PROFILE,
				getProvisioningMap())

		// Certificate name
		addStringValueForPlist(PLIST_KEY_SIGNING_CERTIFICATE,
				certificateFriendlyName.get())

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
