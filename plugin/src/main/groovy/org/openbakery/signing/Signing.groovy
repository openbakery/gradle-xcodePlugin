package org.openbakery.signing

import org.gradle.api.Project

/**
 *
 * @author René Pirringer
 *
 */
class Signing {

	public final static KEYCHAIN_NAME_BASE = "gradle-"


	String identity
	String certificateURI
	String certificatePassword
	List<String> mobileProvisionURI = null
	String keychainPassword = "This_is_the_default_keychain_password"
	File keychain
	Integer timeout
	String plugin


	/**
	 * internal parameters
	 */
	Object signingDestinationRoot
	Object keychainPathInternal
	final Project project
	final String keychainName =  KEYCHAIN_NAME_BASE + System.currentTimeMillis() +  ".keychain"


	Object mobileProvisionDestinationRoot
	List<File> mobileProvisionFile = new ArrayList<File>()




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

	void setMobileProvisionURI(Object mobileProvisionURI) {
		if (mobileProvisionURI instanceof List) {
			this.mobileProvisionURI = mobileProvisionURI;
		} else {
			this.mobileProvisionURI = new ArrayList<String>();
			this.mobileProvisionURI.add(mobileProvisionURI.toString());
		}
	}

	void setMobileProvisionFile(Object mobileProvision) {

		File fileToAdd = null

		if (mobileProvision instanceof File) {
			fileToAdd = mobileProvision
		} else {
			fileToAdd = new File(mobileProvision.toString());
		}

		if (!fileToAdd.exists()) {
			throw new IllegalArgumentException("given mobile provision file does not exist: " +	fileToAdd.absolutePath)
		}
		mobileProvisionFile.add(fileToAdd)
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
