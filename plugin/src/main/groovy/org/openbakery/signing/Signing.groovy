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
	String keychainPassword = "This_is_the_default_keychain_password"
	File keychain
	Integer timeout = 3600
	String plugin
	Object entitlementsFile


	/**
	 * internal parameters
	 */
	Object signingDestinationRoot
	Object keychainPathInternal
	final Project project
	final String keychainName =  KEYCHAIN_NAME_BASE + System.currentTimeMillis() +  ".keychain"
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


	File getEntitlementsFile() {
		if (entitlementsFile != null) {
			return project.file(entitlementsFile)
		}
		return null
	}

	void setEntitlementsFile(Object entitlementsFile) {
		this.entitlementsFile = entitlementsFile
	}

	String getIdentity() {

		def IDENTITY_PATTERN = ~/\s*\d+\)\s*(\w+)\s*\"(.*)\"/

		if (this.identity == null) {
			String identities = commandRunner.runWithResult(["security", "find-identity", "-v", "-p", "codesigning", getKeychainPathInternal().absolutePath])

			def matcher = IDENTITY_PATTERN.matcher(identities)
			String identity = null
			if (matcher.find()) {
				identity = matcher[0][1]
			}

			if (!matcher.find()) {
				// only use the identify if only one was found!!!
				// otherwise leave it to the default value null
				this.identity = identity
			}


		}
		return this.identity
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
