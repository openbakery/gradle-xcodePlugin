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

	final DirectoryProperty signingDestinationRoot = project.layout.directoryProperty()
	final DirectoryProperty provisioningDestinationRoot = project.layout.directoryProperty()
	final Property<String> certificatePassword
	final Property<String> certificateFriendlyName
	final Property<Integer> timeout = project.objects.property(Integer)
	final Property<SigningMethod> signingMethod = project.objects.property(SigningMethod)
	final RegularFileProperty certificate
	final RegularFileProperty entitlementsFile
	final RegularFileProperty keychain = project.layout.fileProperty()
	final RegularFileProperty keyChainFile = project.layout.fileProperty()
	final ListProperty<String> mobileProvisionList = project.objects.listProperty(String)

	@Internal
	final Provider<List<File>> registeredProvisioningFiles = project.objects.listProperty(File)

	@Internal
	final Provider<List<ProvisioningFile>> registeredProvisioning = project.objects.listProperty(ProvisioningFile)

	@Internal
	Object keychainPathInternal

	@Internal
	final RegularFileProperty xcConfigFile

	public static final String KEYCHAIN_NAME_BASE = "gradle-"
	private static final Pattern PATTERN = ~/^\s{4}friendlyName:\s(?<friendlyName>[^\n]+)/

	String identity

	String plugin

	/**
	 * internal parameters
	 */
	private final Project project
	private
	final String keychainName = KEYCHAIN_NAME_BASE + System.currentTimeMillis() + ".keychain"
	CommandRunner commandRunner

	List<File> mobileProvisionFile = new ArrayList<File>()

	@Inject
	Signing(Project project) {
		this.project = project
		this.signingDestinationRoot.set(project.layout.buildDirectory.dir("codesign"))
		this.provisioningDestinationRoot.set(project.layout.buildDirectory.dir("provision"))

		this.certificate = project.layout.fileProperty()
		this.certificatePassword = project.objects.property(String)
		this.certificateFriendlyName = project.objects.property(String)
		this.certificateFriendlyName.set(certificate.map(new Transformer<String, RegularFile>() {
			@Override
			String transform(RegularFile regularFile) {
				return getSignatureFriendlyName(regularFile.asFile)
			}
		}))

		this.commandRunner = new CommandRunner()
		this.entitlementsFile = project.layout.fileProperty()

		this.keyChainFile.set(signingDestinationRoot.file(keychainName))
		this.timeout.set(3600)

		this.xcConfigFile = project.layout.fileProperty()
		this.xcConfigFile.set(project.layout.buildDirectory.file(PathHelper.FOLDER_ARCHIVE + "/" + PathHelper.GENERATED_XCARCHIVE_FILE_NAME))
//		PathHelper.resolveXcConfigFile(project))
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

	void setMobileProvisionURI(List<String> list) {
		list.each { this.mobileProvisionList.add(it) }
	}

	void setMobileProvisionURI(String value) {
		this.mobileProvisionList.add(value)
	}

	boolean hasEntitlementsFile() {
		return entitlementsFile.present
	}

	String getIdentity() {
		return this.identity
	}

	public void entitlements(Map<String, Object> entitlements) {
		this.entitlements = entitlements

	}

	void setCertificateURI(String uri) {
		certificate.set(new File(new URI(uri)))
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

	CodesignParameters getCodesignParameters() {
		CodesignParameters result = new CodesignParameters()
		result.signingIdentity = getIdentity()
		result.mobileProvisionFiles = registeredProvisioningFiles.getFiles().asList().clone() as List<File>
		result.keychain = getKeychain()
		if (entitlements != null) {
			result.entitlements = entitlements.clone()
		}
		result.entitlementsFile = getEntitlementsFile()
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
}
