package org.openbakery.codesign

import org.openbakery.configuration.Configuration
import org.openbakery.configuration.ConfigurationFromPlist
import org.openbakery.util.PlistHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EntitlementsHelper {

	private static Logger logger = LoggerFactory.getLogger(EntitlementsHelper.class)
	public static final String APPLICATION_IDENTIFIER_PREFIX = '$(AppIdentifierPrefix)'

	private PlistHelper plistHelper
	String applicationIdentifierPrefix
	String teamIdentifierPrefix

	enum EntitlementAction {
		ADD, REPLACE, DELETE
	}


	EntitlementsHelper(String applicationIdentifierPrefix, String teamIdentifierPrefix, PlistHelper plistHelper) {
		this.plistHelper = plistHelper
		this.applicationIdentifierPrefix = applicationIdentifierPrefix
		this.teamIdentifierPrefix = teamIdentifierPrefix
	}

	void update(
		File entitlementFile,
		String bundleIdentifier,
		List<String> keychainAccessGroups,
		Configuration configuration) {

		def applicationIdentifier = plistHelper.getValueFromPlist(entitlementFile, "application-identifier")
		logger.info("applicationIdentifier from entitlements: {}", applicationIdentifier)
		String bundleIdentifierPrefix = ""
		if (applicationIdentifier != null) {
			String[] tokens = applicationIdentifier.split("\\.")
			for (int i = 1; i < tokens.length; i++) {
				if (tokens[i] == "*") {
					break
				}
				if (bundleIdentifierPrefix.length() > 0) {
					bundleIdentifierPrefix += "."
				}
				bundleIdentifierPrefix += tokens[i]
			}
		}

		if (!bundleIdentifier.startsWith(bundleIdentifierPrefix)) {
			throw new IllegalStateException("In the provisioning profile a application identifier is specified with " + bundleIdentifierPrefix + " but the app uses a bundle identifier " + bundleIdentifier + " that does not match!")
		}

		if (teamIdentifierPrefix == null) {
			teamIdentifierPrefix = applicationIdentifierPrefix
		}


		setBundleIdentifierToEntitlementsForValue(entitlementFile, bundleIdentifier, applicationIdentifierPrefix, "application-identifier")
		setBundleIdentifierToEntitlementsForValue(entitlementFile, bundleIdentifier, applicationIdentifierPrefix, "com.apple.application-identifier")
		setBundleIdentifierToEntitlementsForValue(entitlementFile, bundleIdentifier, teamIdentifierPrefix, "com.apple.developer.ubiquity-kvstore-identifier")
		setBundleIdentifierToEntitlementsForValue(entitlementFile, bundleIdentifier, teamIdentifierPrefix, "com.apple.developer.ubiquity-container-identifiers")


		if (keychainAccessGroups != null && keychainAccessGroups.size() > 0) {
			def modifiedKeychainAccessGroups = []
			keychainAccessGroups.each() { group ->
				modifiedKeychainAccessGroups << group.replace(APPLICATION_IDENTIFIER_PREFIX, applicationIdentifierPrefix + ".")
			}
			plistHelper.setValueForPlist(entitlementFile, "keychain-access-groups", modifiedKeychainAccessGroups)
		} else {
			plistHelper.deleteValueFromPlist(entitlementFile, "keychain-access-groups")
		}


		// copy the missing values that are in configuration (xcent or signing.entitlments) to the entitlements for signing
		enumerateMissingEntitlements(entitlementFile, configuration) { key, value, action ->

			if (value instanceof String) {
				value = this.replaceVariables(value)
			} else if (value instanceof List) {
				value = this.replaceValuesInList((List) value)
			}


			switch (action) {
				case EntitlementAction.REPLACE:
					logger.info("replace in entitlement: {} with {}", key, value)
					plistHelper.setValueForPlist(entitlementFile, key, value)
					break
				case EntitlementAction.ADD:
					logger.info("add to entitlement: {} with {}", key, value)
					plistHelper.addValueForPlist(entitlementFile, key, value)
					break
				case EntitlementAction.DELETE:
					logger.info("delete to entitlement: {}", key)
					plistHelper.deleteValueFromPlist(entitlementFile, key)
					break
			}
		}

	}


	private void setBundleIdentifierToEntitlementsForValue(File entitlementFile, String bundleIdentifier, String prefix, String value) {
		def currentValue = plistHelper.getValueFromPlist(entitlementFile, value)

		if (currentValue == null) {
			return
		}

		if (currentValue instanceof List) {
			def modifiedValues = []
			currentValue.each { item ->
				if (item.toString().endsWith('*')) {
					modifiedValues << prefix + "." + bundleIdentifier
				}
			}
			plistHelper.setValueForPlist(entitlementFile, value, modifiedValues)

		} else {
			if (currentValue.toString().endsWith('*')) {
				plistHelper.setValueForPlist(entitlementFile, value, prefix + "." + bundleIdentifier)
			}
		}
	}

	private String replaceVariables(String value) {

		if (value.startsWith(APPLICATION_IDENTIFIER_PREFIX)) {
			return value.replace(APPLICATION_IDENTIFIER_PREFIX, applicationIdentifierPrefix + ".")
		}
		return value
	}


	private void enumerateMissingEntitlements(File entitlementFile, Configuration configuration, Closure closure) {
		if (configuration == null) {
			return
		}

		Configuration entitlements = new ConfigurationFromPlist(entitlementFile)
		Set<String> replaceKeys = configuration.getReplaceEntitlementsKeys()

		for (String key in configuration.getKeys()) {
			Object value = configuration.get(key) //plistHelper.getValueFromPlist(xcent, key)

			if (!entitlements.containsKey(key)) {
				closure(key, value, EntitlementAction.ADD)
			} else if (replaceKeys.contains(key)) {
				closure(key, value, EntitlementAction.REPLACE)
			}
		}

		for (String key in configuration.getDeleteEntitlementsKeys()) {
			closure(key, null, EntitlementAction.DELETE)
		}

	}


	private List replaceValuesInList(List list) {
		def result = []
		for (Object item : list) {
			if (item instanceof String) {
				result << replaceVariables((String) item)
			} else {
				result << item
			}
		}
		return result
	}

}
