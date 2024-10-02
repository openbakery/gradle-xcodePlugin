/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery.codesign

import org.apache.commons.configuration2.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.configuration.Configuration
import org.openbakery.configuration.ConfigurationFromPlist
import org.openbakery.util.PlistHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.DateFormat

enum ProvisioningProfileType {
	Development, AdHoc, Enterprise, AppStore
}

class ProvisioningProfileReader {

	enum EntitlementAction {
		ADD, REPLACE, DELETE
	}

	public static final String APPLICATION_IDENTIFIER_PREFIX = '$(AppIdentifierPrefix)'
	protected CommandRunner commandRunner
	private PlistHelper plistHelper


	private static Logger logger = LoggerFactory.getLogger(ProvisioningProfileReader.class)

	XMLPropertyListConfiguration config


	File provisioningProfile
	private File provisioningPlist
	private File keychain

	ProvisioningProfileReader(File provisioningProfile, CommandRunner commandRunner) {
		this(provisioningProfile, commandRunner, null, new PlistHelper(commandRunner))
	}

	ProvisioningProfileReader(File provisioningProfile, CommandRunner commandRunner, PlistHelper plistHelper) {
		this(provisioningProfile, commandRunner, null, plistHelper)
	}

	ProvisioningProfileReader(File provisioningProfile, CommandRunner commandRunner, File keychain) {
		this(provisioningProfile, commandRunner, keychain, new PlistHelper(commandRunner))
	}

	ProvisioningProfileReader(File provisioningProfile, CommandRunner commandRunner, File keychain, PlistHelper plistHelper) {
		super()

		logger.debug("load provisioningProfile: {}", provisioningProfile)
		String text = load(provisioningProfile)
		logger.debug("provisioningProfile content:\n{}\n--- END ---", text)
		config = new XMLPropertyListConfiguration()
		config.read(new StringReader(text))

		this.commandRunner = commandRunner

		this.plistHelper = plistHelper

		this.keychain = keychain

		checkExpired()
	}

	static ProvisioningProfileReader getReaderForIdentifier(String bundleIdentifier, List<File> mobileProvisionFiles, CommandRunner commandRunner, PlistHelper plistHelper) {
		this.getReaderForIdentifier(bundleIdentifier, mobileProvisionFiles, commandRunner, null, plistHelper)
	}

	static ProvisioningProfileReader getReaderForIdentifier(String bundleIdentifier, List<File> mobileProvisionFiles, CommandRunner commandRunner, File keychain, PlistHelper plistHelper) {
		def provisionFileMap = [:]

		for (File mobileProvisionFile : mobileProvisionFiles) {
			if (mobileProvisionFile.exists()) {
				ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileProvisionFile, commandRunner, keychain, plistHelper)
				provisionFileMap.put(reader.getApplicationIdentifier(), reader)
			}
		}

		logger.debug("provisionFileMap: {}", provisionFileMap)

		for (entry in provisionFileMap) {
			if (entry.key.equalsIgnoreCase(bundleIdentifier)) {
				return entry.value
			}
		}

		// match wildcard
		for (entry in provisionFileMap) {
			if (entry.key.equals("*")) {
				return entry.value
			}

			if (entry.key.endsWith("*")) {
				String key = entry.key[0..-2].toLowerCase()
				if (bundleIdentifier.toLowerCase().startsWith(key)) {
					return entry.value
				}
			}
		}

		logger.info("No provisioning profile found for bundle identifier {}",  bundleIdentifier)
		logger.info("Available bundle identifier are {}" + provisionFileMap.keySet())

		return null
	}

	String load(File provisioningProfile) {
		this.provisioningProfile = provisioningProfile

		if (!this.provisioningProfile.exists()) {
			logger.warn("The specified provisioning profile does not exist: " + this.provisioningProfile.absolutePath)
			return null
		}

		StringBuffer result = new StringBuffer()

		boolean append = false
		for (String line : this.provisioningProfile.text.split("\n")) {
			if (line.startsWith("<!DOCTYPE plist PUBLIC")) {
				append = true
			}

			if (line.startsWith("</plist>")) {
				result.append("</plist>")
				return result.toString()
			}

			if (append) {
				result.append(line)
				result.append("\n")
			}


		}
		return ""
	}


	boolean checkExpired() {

		Date expireDate = config.getProperty("ExpirationDate")
		if (expireDate.before(new Date())) {
			DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault())
			throw new IllegalArgumentException("The Provisioning Profile has expired on " + formatter.format(expireDate) )
		}
	}


	String getUUID() {
		return config.getString("UUID")
	}

	String getApplicationIdentifierPrefix() {
		return config.getString("ApplicationIdentifierPrefix")
	}

	String getTeamIdentifierPrefix() {
		return config.getString("TeamIdentifier")
	}

	File getPlistFromProvisioningProfile() {
		if (provisioningPlist == null) {
			// unpack provisioning profile to plain plist
			String basename = FilenameUtils.getBaseName(provisioningProfile.path)
			// read temporary plist file
			File tmpDir = new File(System.getProperty("java.io.tmpdir"))
			provisioningPlist = new File(tmpDir, "provision_" + basename + ".plist")
			provisioningPlist.deleteOnExit()

			if (this.keychain) {
				logger.debug("Using keychain " + keychain)
			} else {
				logger.debug("Using default keychain")
			}

			List<String> commands = []
			commands << "security"
			commands << "cms"
			commands << "-D"
			commands << "-i"
			commands << provisioningProfile.getCanonicalPath()
			commands << "-o"
			commands << provisioningPlist.absolutePath

			if (this.keychain) {
				commands << "-k"
				commands << keychain.absolutePath
			}

			try {
				commandRunner.run(commands)
			} catch (CommandRunnerException ex) {
				if (!provisioningPlist.exists() || provisioningPlist.length() == 0) {
					throw new IllegalStateException("provisioning plist does not exist or is empty: " + provisioningPlist)
				}
			}
		}
		return provisioningPlist
	}

	String getApplicationIdentifier() {

		String value

		if (this.provisioningProfile.path.endsWith(".mobileprovision")) {
			value = config.getProperty("Entitlements.application-identifier")
		} else {
			value = plistHelper.getValueFromPlist(getPlistFromProvisioningProfile(), "Entitlements:com.apple.application-identifier")
		}

		String prefix = getApplicationIdentifierPrefix() + "."
		if (value.startsWith(prefix)) {
			return value.substring(prefix.length())
		}
		return value
	}

	/* xcent is the archive entitlements */
	void extractEntitlements(File entitlementFile, String bundleIdentifier, List<String> keychainAccessGroups, Configuration configuration) {
		logger.info("extractEntitlements for " + bundleIdentifier)
		File plistFromProvisioningProfile = getPlistFromProvisioningProfile()
		String entitlements = commandRunner.runWithResult([
						"/usr/libexec/PlistBuddy",
						"-x",
						plistFromProvisioningProfile.absolutePath,
						"-c",
						"Print Entitlements"])

		if (StringUtils.isEmpty(entitlements)) {
			logger.debug("No entitlements found in {}", plistFromProvisioningProfile)
			return
		}
		//String entitlements = plistHelper.commandForPlist(getPlistFromProvisioningProfile(), "Print Entitlements")
		FileUtils.writeStringToFile(entitlementFile, entitlements.toString())



		def applicationIdentifier = plistHelper.getValueFromPlist(entitlementFile, "application-identifier")
		logger.info("applicationIdentifier from entitlements: {}", applicationIdentifier)
		String bundleIdentifierPrefix = ""
		if (applicationIdentifier != null) {
			String[] tokens = applicationIdentifier.split("\\.")
			for (int i=1; i<tokens.length; i++) {
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


		String applicationIdentifierPrefix = getApplicationIdentifierPrefix()
		String teamIdentifierPrefix = getTeamIdentifierPrefix()
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
				value = this.replaceValuesInList((List)value)
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
					logger.info("delete from entitlement: {}", key)
					plistHelper.deleteValueFromPlist(entitlementFile, key)
					break
			}
		}


		if (logger.isDebugEnabled()) {
			String entitlementsContent = FileUtils.readFileToString(entitlementFile)
			logger.debug("entitlements content\n{}", entitlementsContent)
		}
	}

	private List replaceValuesInList(List list) {
		def result = []
		for (Object item : list) {
			if (item instanceof String) {
				result << replaceVariables((String)item)
			} else {
				result << item
			}
		}
		return result
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
		Set<String>replaceKeys = configuration.getReplaceEntitlementsKeys()

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

	private void setBundleIdentifierToEntitlementsForValue(File entitlementFile, String bundleIdentifier, String prefix, String value) {
		logger.debug("")
		logger.debug("setBundleIdentifierToEntitlementsForValue")
		logger.debug("value            " + value)
		def currentValue = plistHelper.getValueFromPlist(entitlementFile, value)

		if (currentValue == null) {
			logger.debug("nothing to do")
			return
		}
		logger.debug("entitlementFile  " + entitlementFile)
		logger.debug("bundleIdentifier " + bundleIdentifier)
		logger.debug("prefix           " + prefix)


		if (currentValue instanceof List) {
			def modifiedValues = []
			currentValue.each { item ->
				logger.debug("item " + item)
				if (item.toString().endsWith('*')) {
					modifiedValues << prefix + "." + bundleIdentifier
				} else {
					modifiedValues << item.toString()
				}
			}
			logger.debug("set to           " + modifiedValues)
			plistHelper.setValueForPlist(entitlementFile, value, modifiedValues)

		} else {
			if (currentValue.toString().endsWith('*')) {
				logger.debug("set to           " +  prefix + "."  + bundleIdentifier)
				plistHelper.setValueForPlist(entitlementFile, value, prefix + "."  + bundleIdentifier)
			}
		}
	}

	public boolean isAdHoc() {
		def provisionedDevices = config.getList("ProvisionedDevices")
		return !provisionedDevices.empty
	}

	ProvisioningProfileType getProfileType() {
		def getTaskAllow = config.getBoolean("Entitlements.get-task-allow", false)
		def hasDevices = !config.getList("ProvisionedDevices").empty
		def isEnterprise = config.getBoolean("ProvisionsAllDevices", false)

        if (hasDevices) {
			return getTaskAllow ? ProvisioningProfileType.Development : ProvisioningProfileType.AdHoc
		}

		return isEnterprise ? ProvisioningProfileType.Enterprise : ProvisioningProfileType.AppStore
	}
}
