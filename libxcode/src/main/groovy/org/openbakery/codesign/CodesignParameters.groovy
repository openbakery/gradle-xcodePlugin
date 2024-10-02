package org.openbakery.codesign

import org.openbakery.CommandRunner
import org.openbakery.xcode.Type
import org.openbakery.log.LoggerFactory
import org.openbakery.log.Logger

class CodesignParameters extends BaseCodesignParameters {

	private Logger logger = LoggerFactory.getInstance().getLogger(CodesignParameters.class.name)

	String signingIdentity
	List<File> mobileProvisionFiles
	File keychain
	Security security
	Type type
	/*
	 	user this entitlements file for codesigning, nothing is extracted from the mobile provisioning profile
	 */
	File entitlementsFile
	Map<String, Object> entitlements
	Map<String, Map<String, Object>> bundleEntitlements

	CodesignParameters() {
		security = new Security(new CommandRunner())
	}

	String getSigningIdentity() {
		if (signingIdentity == null) {
			if (keychain != null && keychain.exists()) {
				signingIdentity = security.getIdentity(keychain)
			}
		}
		return signingIdentity
	}


	void mergeMissing(CodesignParameters parameters) {
		if (signingIdentity == null) {
			signingIdentity = parameters.signingIdentity
		}

		if (keychain == null) {
			keychain = parameters.keychain
		}

		if ((mobileProvisionFiles == null || mobileProvisionFiles.isEmpty()) &&  parameters.mobileProvisionFiles != null) {
			mobileProvisionFiles = parameters.mobileProvisionFiles.clone()
		}

		if (type == null) {
			type = parameters.type
		}

		if (entitlementsFile == null) {
			entitlementsFile = parameters.entitlementsFile
		}

		if (entitlements == null) {
			entitlements = parameters.entitlements
		}

		if (bundleEntitlements == null) {
			bundleEntitlements = parameters.bundleEntitlements
		}

	}

	Map<String, Object>getEntitlements(String bundleIdentifier) {
		if (bundleEntitlements == null) {
			logger.info("bundleEntitlements is null")
			return null
		}

		logger.info("bundleEntitlements {}", bundleEntitlements)
		logger.info("bundleEntitlements class name{}", bundleEntitlements.getClass().name)

		for (key in bundleEntitlements.keySet()) {
			if (key.equalsIgnoreCase(bundleIdentifier)) {
				Map<String, Object> result = bundleEntitlements.get(key)
				logger.info("getEntitlements for {}", result)
				return result
			}
		}

		logger.info("bundleEntitlements does not contain key: {}", bundleIdentifier)
		logger.info("bundleEntitlements keys are: {}", bundleEntitlements.keySet())
		return null
	}

}
