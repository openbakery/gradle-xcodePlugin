package org.openbakery.signing

import org.gradle.api.Project

/**
 *
 * @author René Pirringer
 *
 */
class Signing {

	public final static KEYCHAIN_NAME_BASE = "gradle-"
	public final static PROVISIONING_NAME_BASE = "gradle-"

	String identity
	String certificateURI
	String certificatePassword
	String mobileProvisionURI
	String keychainPassword = "This_is_the_default_keychain_password"
	File keychain
    Integer timeout


	/**
	 * internal parameters
	 */
	Object signingDestinationRoot
	Object keychainPathInternal
	final Project project
	final String keychainName =  KEYCHAIN_NAME_BASE + System.currentTimeMillis() +  ".keychain"


	Object mobileProvisionDestinationRoot
	Object mobileProvisionFile
	Object mobileProvisionFileLinkToLibrary
	final String mobileProvisionName =  PROVISIONING_NAME_BASE + System.currentTimeMillis() +  ".mobileprovision"




	public Signing(Project project) {
		this.project = project;

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

		this.mobileProvisionFileLinkToLibrary = {
			return new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/" + mobileProvisionName);
		}
	}

	void setKeychain(Object keychain) {
		this.keychain = project.file(keychain)
	}


	File getSigningDestinationRoot() {
		return project.file(signingDestinationRoot)
	}

	void setSigningDestinationRoot(Object keychainDestinationRoot) {
		this.destinationRoot = keychainDestinationRoot
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

	File getMobileProvisionFileLinkToLibrary() {
		return project.file(mobileProvisionFileLinkToLibrary)
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
}
