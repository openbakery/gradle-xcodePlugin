package org.openbakery.signing

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.openbakery.CommandRunner
import org.openbakery.codesign.CodesignParameters

import javax.inject.Inject

class Signing {

	final DirectoryProperty signingDestinationRoot = project.layout.directoryProperty()
	final DirectoryProperty provisioningDestinationRoot = project.layout.directoryProperty()
	final Property<String> certificatePassword
	final Property<Integer> timeout = project.objects.property(Integer)
	final Property<SigningMethod> signingMethod = project.objects.property(SigningMethod)
	final RegularFileProperty certificate
	final RegularFileProperty keychain = project.layout.fileProperty()
	final RegularFileProperty keyChainFile = project.layout.fileProperty()
	final ListProperty<String> mobileProvisionList = project.objects.listProperty(String)

	@Internal
	final Provider<List<File>> registeredProvisioningFiles = project.objects.listProperty(File)

	@Internal
	Object keychainPathInternal

	public final static KEYCHAIN_NAME_BASE = "gradle-"

	String identity

	String plugin
	Object entitlementsFile

	Map<String, Object> entitlements

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
		this.commandRunner = new CommandRunner()
		this.keyChainFile.set(signingDestinationRoot.file(keychainName))
		this.timeout.set(3600)
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

	File getEntitlementsFile() {
		if (entitlementsFile != null) {
			if (entitlementsFile instanceof File) {
				return entitlementsFile
			}
			return project.file(entitlementsFile)

		}
		return null
	}

	boolean hasEntitlementsFile() {
		return entitlementsFile != null && entitlementsFile.exists()
	}

	void setEntitlementsFile(Object entitlementsFile) {
		this.entitlementsFile = entitlementsFile
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


}
