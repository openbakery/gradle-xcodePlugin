package org.openbakery.signing

import org.gradle.api.Project
import org.openbakery.CommandRunner
import org.openbakery.codesign.CodesignParameters

/**
 *
 * @author René Pirringer
 *
 */
class Signing {

	public final static KEYCHAIN_NAME_BASE = "gradle-"

	SigningMethod method
	String identity
	String certificateURI
	String certificatePassword
	List<String> mobileProvisionURI = null
	String keychainPassword = "This_is_the_default_keychain_password"
	File keychain
	Integer timeout = 3600
	String plugin
	Object entitlementsFile

	Map<String, Object> entitlements

	/**
	 * internal parameters
	 */
	Object signingDestinationRoot
	Object keychainPathInternal
	final Project project
	final String keychainName = KEYCHAIN_NAME_BASE + System.currentTimeMillis() + ".keychain"
	CommandRunner commandRunner


	Object mobileProvisionDestinationRoot
	List<File> mobileProvisionFile = new ArrayList<File>()


	public Signing(Project project) {
		this.project = project;
		this.commandRunner = new CommandRunner()

		this.signingDestinationRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("codesign")
		}

		this.keychainPathInternal = {
			if (this.keychain != null) {
				return this.keychain
			}
			return new File(this.signingDestinationRoot, keychainName)
		}

		this.mobileProvisionDestinationRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("provision")
		}

	}

	void setKeychain(Object keychain) {
		if (keychain instanceof String && keychain.matches("^~/.*")) {
			keychain = keychain.replaceFirst("~", System.getProperty('user.home'))
		}
		this.keychain = project.file(keychain)
	}

	public void setMethod(String method) {
		this.method = SigningMethod.fromString(method)
				.orElseThrow { new IllegalArgumentException("Method : $method is not a valid export method") }
	}

	File getSigningDestinationRoot() {
		return project.file(signingDestinationRoot)
	}

	void setSigningDestinationRoot(Object keychainDestinationRoot) {
		this.signingDestinationRoot = keychainDestinationRoot
	}

	File getKeychainPathInternal() {
		return project.file(keychainPathInternal)
	}

	File getMobileProvisionDestinationRoot() {
		return project.file(mobileProvisionDestinationRoot)
	}

	void setMobileProvisionDestinationRoot(Object mobileProvisionDestinationRoot) {
		this.mobileProvisionDestinationRoot = mobileProvisionDestinationRoot
	}

	void setMobileProvisionURI(Object mobileProvisionURI) {
		if (mobileProvisionURI instanceof List) {
			this.mobileProvisionURI = mobileProvisionURI;
		} else {
			this.mobileProvisionURI = new ArrayList<String>();
			this.mobileProvisionURI.add(mobileProvisionURI.toString());
		}
	}


	void addMobileProvisionFile(File mobileProvision) {
		if (!mobileProvision.exists()) {
			throw new IllegalArgumentException("given mobile provision file does not exist: " + mobileProvision.absolutePath)
		}
		mobileProvisionFile.add(mobileProvision)
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
				", certificateURI='" + certificateURI + '\'' +
				", certificatePassword='" + certificatePassword + '\'' +
				", mobileProvisionURI='" + mobileProvisionURI + '\'' +
				'}';
	}

	CodesignParameters getCodesignParameters() {
		CodesignParameters result = new CodesignParameters()
		result.signingIdentity = getIdentity()
		result.mobileProvisionFiles = getMobileProvisionFile().clone()
		result.keychain = getKeychain()
		if (entitlements != null) {
			result.entitlements = entitlements.clone()
		}
		result.entitlementsFile = getEntitlementsFile()
		return result
	}


}
