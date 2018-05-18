package org.openbakery.extension

import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.openbakery.CommandRunner
import org.openbakery.codesign.CodesignParameters
import org.openbakery.signing.ProvisioningFile
import org.openbakery.signing.SigningMethod
import org.openbakery.util.PathHelper

import javax.inject.Inject
import java.util.regex.Matcher
import java.util.regex.Pattern

class Signing {

	final DirectoryProperty provisioningDestinationRoot = project.layout.directoryProperty()
	final DirectoryProperty signingDestinationRoot = project.layout.directoryProperty()
	final ListProperty<String> mobileProvisionList = project.objects.listProperty(String)
	final Property<Integer> timeout = project.objects.property(Integer)
	final Property<SigningMethod> signingMethod = project.objects.property(SigningMethod)
	final Property<String> certificateFriendlyName = project.objects.property(String)
	final Property<String> certificatePassword = project.objects.property(String)
	final RegularFileProperty certificate = project.layout.fileProperty()
	final RegularFileProperty entitlementsFile = project.layout.fileProperty()
	final RegularFileProperty keychain = project.layout.fileProperty()
	final RegularFileProperty keyChainFile = project.layout.fileProperty()

	@Internal
	final Provider<List<File>> registeredProvisioningFiles = project.objects.listProperty(File)

	@Internal
	final Provider<List<ProvisioningFile>> registeredProvisioning = project.objects.listProperty(ProvisioningFile)

	@Internal
	final RegularFileProperty xcConfigFile = project.layout.fileProperty()

	@Deprecated
	final Property<Map<String, Object>> entitlementsMap = project.objects.property(Map)

	@Internal
	Object keychainPathInternal

	String identity
	String plugin

	public static final String KEYCHAIN_NAME_BASE = "gradle-"
	private static final Pattern PATTERN = ~/^\s{4}friendlyName:\s(?<friendlyName>[^\n]+)/

	/**
	 * internal parameters
	 */
	private final Project project
	private final CommandRunner commandRunner

	@Inject
	Signing(Project project,
			CommandRunner commandRunner) {

		this.commandRunner = commandRunner
		this.project = project

		this.signingDestinationRoot.set(project.layout.buildDirectory.dir("codesign"))
		this.provisioningDestinationRoot.set(project.layout.buildDirectory.dir("provision"))
		this.certificateFriendlyName.set(certificate.map(new Transformer<String, RegularFile>() {
			@Override
			String transform(RegularFile regularFile) {
				return getSignatureFriendlyName(regularFile.asFile)
			}
		}))

		this.keyChainFile.set(signingDestinationRoot.file(KEYCHAIN_NAME_BASE
				+ System.currentTimeMillis()
				+ ".keychain"))
		this.timeout.set(3600)

		this.xcConfigFile.set(project.layout
				.buildDirectory
				.file(PathHelper.FOLDER_ARCHIVE + "/" + PathHelper.GENERATED_XCARCHIVE_FILE_NAME))
	}

	void setKeychain(Object keychain) {
		if (keychain instanceof String && keychain.matches("^~/.*")) {
			keychain = keychain.replaceFirst("~", System.getProperty('user.home'))
		}
		this.keychain.set(project.file(keychain))
	}

	public void setMethod(String method) {
		signingMethod.set(SigningMethod.fromString(method)
				.orElseThrow {
			new IllegalArgumentException("Method : $method is not a valid export method")
		})
	}

	File getKeychainPathInternal() {
		return project.file(keychainPathInternal)
	}

	void entitlements(Map<String, Object> entitlements) {
		if (!this.entitlementsMap.present) {
			this.entitlementsMap.set(entitlements)
		} else {
			this.entitlementsMap.get() << entitlements
		}
	}

	boolean hasEntitlementsFile() {
		return entitlementsFile.present
	}

	String getIdentity() {
		return this.identity
	}

	@Deprecated
	void setEntitlementsFile(String value) {
		entitlementsFile.set(new File(value))
	}

	@Deprecated
	void setMobileProvisionURI(String value) {
		this.mobileProvisionList.add(value)
	}

	@Deprecated
	void setMobileProvisionURI(String... values) {
		values.each { this.mobileProvisionList.add(it) }
	}

	CodesignParameters getCodesignParameters() {
		CodesignParameters result = new CodesignParameters()
		result.signingIdentity = getIdentity()
		if (registeredProvisioningFiles.present) {
			result.mobileProvisionFiles = new ArrayList<File>(registeredProvisioningFiles.get()
					.asList()
					.toArray() as ArrayList<File>)
		}
		result.keychain = getKeychain().asFile.getOrNull() as File
		result.signingIdentity = identity
		result.entitlements = entitlementsMap.getOrNull()
		result.entitlementsFile = entitlementsFile.asFile.getOrNull()

		return result
	}

	String getSignatureFriendlyName(File file) {
		return Optional.ofNullable(getKeyContent(file)
				.split(System.getProperty("line.separator"))
				.find { PATTERN.matcher(it).matches() })
				.map { PATTERN.matcher(it) }
				.filter { Matcher it -> it.matches() }
				.map { Matcher it ->
			return it.group("friendlyName")
		}
		.orElseThrow {
			new IllegalArgumentException("Failed to resolve the code signing identity from the certificate ")
		}
	}

	private String getKeyContent(File file) {
		return commandRunner.runWithResult(["openssl",
											"pkcs12",
											"-nokeys",
											"-in",
											file.absolutePath,
											"-passin",
											"pass:" + certificatePassword.get()])
	}

	@Override
	public String toString() {
		if (this.keychain != null) {
			return "Signing{" +
					" identity='" + identity + '\'' +
					", mobileProvisionURI='" + mobileProvisionList.get() + '\'' +
					", keychain='" + keychain + '\'' +
					'}';
		}
		return "Signing{" +
				" identity='" + identity + '\'' +
				", certificateURI='" + certificate.getOrNull() + '\'' +
				", certificatePassword='" + certificatePassword.getOrNull() + '\'' +
				", mobileProvisionURI='" + mobileProvisionList.get() + '\'' +
				'}';
	}
}
