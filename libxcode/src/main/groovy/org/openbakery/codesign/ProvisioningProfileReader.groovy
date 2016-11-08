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

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.helpers.PlistHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.DateFormat

class ProvisioningProfileReader {

	public static final String APPLICATION_IDENTIFIER_PREFIX = '$(AppIdentifierPrefix)'
	protected CommandRunner commandRunner
	private PlistHelper plistHelper


	private static Logger logger = LoggerFactory.getLogger(ProvisioningProfileReader.class)

	XMLPropertyListConfiguration config


	private File provisioningProfile
	private File provisioningPlist


	ProvisioningProfileReader(File provisioningProfile, CommandRunner commandRunner) {
		this(provisioningProfile, commandRunner, new PlistHelper(commandRunner))
	}

	ProvisioningProfileReader(File provisioningProfile, CommandRunner commandRunner, PlistHelper plistHelper) {
		super()

		String text = load(provisioningProfile)
		config = new XMLPropertyListConfiguration()
		config.load(new StringReader(text))

		this.commandRunner = commandRunner

		this.plistHelper = plistHelper

		checkExpired()
	}

	static File getProvisionFileForIdentifier(String bundleIdentifier, List<File> mobileProvisionFiles, CommandRunner commandRunner, PlistHelper plistHelper) {
		def provisionFileMap = [:]

		for (File mobileProvisionFile : mobileProvisionFiles) {
			ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileProvisionFile, commandRunner, plistHelper)
			provisionFileMap.put(reader.getApplicationIdentifier(), mobileProvisionFile)
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

			try {
				commandRunner.run(["security",
													 "cms",
													 "-D",
													 "-i",
													 provisioningProfile.getCanonicalPath(),
													 "-o",
													 provisioningPlist.absolutePath
				])
			} catch (CommandRunnerException ex) {
				if (!provisioningPlist.exists()) {
					throw new IllegalStateException("provisioning plist does not exist: " + provisioningPlist)
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

	void extractEntitlements(File entitlementFile, String bundleIdentifier, List<String> keychainAccessGroups) {
		String entitlements = commandRunner.runWithResult([
						"/usr/libexec/PlistBuddy",
						"-x",
						getPlistFromProvisioningProfile().absolutePath,
						"-c",
						"Print Entitlements"])

		//String entitlements = plistHelper.commandForPlist(getPlistFromProvisioningProfile(), "Print Entitlements")
		FileUtils.writeStringToFile(entitlementFile, entitlements.toString())



		def applicationIdentifier = plistHelper.getValueFromPlist(entitlementFile, "application-identifier")
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
				plistHelper.setValueForPlist(entitlementFile, value, prefix + "."  + bundleIdentifier)
			}
		}
	}

	public boolean isAdHoc() {
		def provisionedDevices = config.getList("ProvisionedDevices")
		return !provisionedDevices.empty
	}

}
