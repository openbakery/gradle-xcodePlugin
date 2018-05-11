package org.openbakery.signing

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.openbakery.CommandRunner
import org.openbakery.codesign.CodesignParameters

import javax.inject.Inject

class Signing {

	final DirectoryProperty signingDestinationRoot = project.layout.directoryProperty()
	final Property<String> certificatePassword
	final Property<Integer> timeout = project.objects.property(Integer)
	final RegularFileProperty certificate
	final RegularFileProperty keychain = project.layout.fileProperty()
	final RegularFileProperty keyChainFile = project.layout.fileProperty()
	final ListProperty<String> mobileProvisionURI = project.objects.listProperty(String)
//	final ConfigurableFileCollection registeredProvisioningFiles = project.files()

	@Internal
	Object keychainPathInternal

	public final static KEYCHAIN_NAME_BASE = "gradle-"

	String identity

//	List<String> mobileProvisionURI = null

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

	private SigningMethod method

	@Inject
	Signing(Project project) {
		this.project = project
		this.signingDestinationRoot.set(project.layout.buildDirectory.dir("codesign"))
		this.signingDestinationRoot.asFile.get().mkdirs()
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
		this.method = SigningMethod.fromString(method)
				.orElseThrow {
			new IllegalArgumentException("Method : $method is not a valid export method")
		}
	}

	SigningMethod getMethod() {
		return method
	}

	File getKeychainPathInternal() {
		return project.file(keychainPathInternal)
	}

//	void setMobileProvisionURI(Object mobileProvisionURI) {
//		if (mobileProvisionURI instanceof List) {
//			this.mobileProvisionURI = mobileProvisionURI;
//		} else {
//			this.mobileProvisionURI = new ArrayList<String>();
//			this.mobileProvisionURI.add(mobileProvisionURI.toString());
//		}
//	}

	@Deprecated
	void setMobileProvisionURI(List<String> list) {
		list.each { this.mobileProvisionURI.add(it) }
	}

	@Deprecated
	void setMobileProvisionURI(String string) {
		this.mobileProvisionURI.add(string)
	}

//	void addMobileProvisionFile(File mobileProvision) {
//		if (!mobileProvision.exists()) {
//			throw new IllegalArgumentException("given mobile provision file does not exist: " + mobileProvision.absolutePath)
//		}
//		mobileProvisionFile.add(mobileProvision)
//	}


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
					", mobileProvisionURI='" + mobileProvisionURI + '\'' +
					", keychain='" + keychain + '\'' +
					'}';
		}
		return "Signing{" +
				" identity='" + identity + '\'' +
				", certificateURI='" + certificate.getOrNull() + '\'' +
				", certificatePassword='" + certificatePassword.getOrNull() + '\'' +
				", mobileProvisionURI='" + mobileProvisionURI + '\'' +
				'}';
	}

	CodesignParameters getCodesignParameters() {
		CodesignParameters result = new CodesignParameters()
		result.signingIdentity = getIdentity()
		result.mobileProvisionFiles = mobileProvisionFile.clone()
		result.keychain = getKeychain()
		if (entitlements != null) {
			result.entitlements = entitlements.clone()
		}
		result.entitlementsFile = getEntitlementsFile()
		return result
	}


}
