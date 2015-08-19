package org.openbakery.signing

import org.gradle.api.Project
import org.openbakery.CommandRunner

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
	String keychainPassword
	File keychain
	Integer timeout


	/**
	 * internal parameters
	 */
	Signing parent
	Object signingDestinationRoot
	Object keychainPathInternal
	final Project project
	final String keychainName =  KEYCHAIN_NAME_BASE + System.currentTimeMillis() +  ".keychain"
	CommandRunner commandRunner


	Object mobileProvisionDestinationRoot
	List<File> mobileProvisionFile = new ArrayList<File>()

	public Signing(Project project) {
		this(project, null)
	}


	public Signing(Project project, Signing parent) {
		this.project = project;
		this.parent = parent
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


	String getIdentity() {

		if (this.identity == null && this.parent != null) {
			return this.parent.getIdentity()
		}

		def IDENTITY_PATTERN = ~/\s*\d+\)\s*(\w+)\s*\"(.*)\"/

		if (this.identity == null) {
			String identities = commandRunner.runWithResult(["security", "find-identity", "-v", "-p", "codesigning", getKeychainPathInternal().absolutePath])

			def matcher = IDENTITY_PATTERN.matcher(identities)
			String identity = null
			if (matcher.find()) {
				identity = matcher[0][2]
			}

			if (!matcher.find()) {
				// only use the identify if only one was found!!!
				// otherwise leave it to the default value null
				this.identity = identity
			}


		}
		return this.identity
	}



	String getCertificateURI() {
		if (this.certificateURI != null) {
			return this.certificateURI
		}
		if (this.parent != null) {
			return this.parent.certificateURI
		}
		return null
	}

	String getCertificatePassword() {
		if (this.certificatePassword != null) {
			return this.certificatePassword
		}
		if (this.parent != null) {
			return this.parent.certificatePassword
		}
		return null
	}

	List<String> getMobileProvisionURI() {
		if (this.mobileProvisionURI != null) {
			return this.mobileProvisionURI
		}
		if (this.parent != null) {
			return this.parent.mobileProvisionURI
		}
		return null
	}

	String getKeychainPassword() {
		if (this.keychainPassword != null) {
			return this.keychainPassword
		}
		if (this.parent != null) {
			return this.parent.keychainPassword
		}
		return "This_is_the_default_keychain_password"
	}

	Integer getTimeout() {
		if (this.timeout != null) {
			return this.timeout
		}
		if (this.parent != null) {
			return this.parent.timeout
		}
		return null
	}


	File getKeychain() {
		if (this.keychain != null) {
			return this.keychain
		}
		if (this.parent != null) {
			return this.parent.keychain
		}
		return null
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
