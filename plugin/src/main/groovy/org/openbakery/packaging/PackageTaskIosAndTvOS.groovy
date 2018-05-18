package org.openbakery.packaging

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningFile
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.signing.SigningMethod
import org.openbakery.util.PathHelper
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild

@CompileStatic
class PackageTaskIosAndTvOS extends DefaultTask {

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

	@Input
	final Provider<Boolean> bitCode = project.objects.property(Boolean)

	final Provider<CommandRunner> commandRunner = project.objects.property(CommandRunner)
	final Provider<PlistHelper> plistHelper = project.objects.property(PlistHelper)

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

		onlyIf(new Spec<Task>() {
			@Override
			boolean isSatisfiedBy(Task task) {
				return buildType.get() == Type.iOS ||
						buildType.get() == Type.tvOS
			}
		})
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

	private boolean validateBitCodeSettings() {
		Boolean bitCodeValue = bitCode.get()
		SigningMethod method = signingMethod.get()

		if (method == SigningMethod.AppStore) {
			if (buildType.get() == Type.tvOS) {
				assert bitCodeValue: "Invalid configuration for the TvOS target " +
						"`AppStore` upload requires BitCode enabled."
			}
		} else {
			assert !bitCodeValue: "The BitCode setting (`xcodebuild.bitCode`) should be " +
					"enabled only for the `AppStore` signing method"
		}

		return bitCodeValue
	}

	private void generateExportOptionPlist() {
		File file = getExportOptionsPlistFile()

		plistHelper.get().create(file)

		// Signing  method
		addStringValueForPlist(PLIST_KEY_METHOD,
				signingMethod.get().value)

		// Provisioning profiles map list
		plistHelper.get().addDictForPlist(file,
				PLIST_KEY_PROVISIONING_PROFILE,
				getProvisioningMap())

		// Certificate name
		addStringValueForPlist(PLIST_KEY_SIGNING_CERTIFICATE,
				certificateFriendlyName.get())

		// BitCode
		plistHelper.get().addValueForPlist(file,
				PLIST_KEY_COMPILE_BITCODE,
				validateBitCodeSettings())

		// SigningMethod
		addStringValueForPlist(PLIST_KEY_SIGNING_STYLE,
				PLIST_VALUE_SIGNING_METHOD_MANUAL)
	}

	private void addStringValueForPlist(String key,
										String value) {
		assert key != null
		assert value != null

		plistHelper.get()
				.addValueForPlist(getExportOptionsPlistFile(), key, value)
	}

	private void packageIt() {
		Xcodebuild.packageIpa(commandRunner.get(),
				getArchiveFile(),
				getOutputDirectory(),
				getExportOptionsPlistFile())
	}

}
