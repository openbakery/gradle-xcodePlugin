package org.openbakery.configuration

import org.apache.commons.configuration2.plist.XMLPropertyListConfiguration


class ConfigurationFromPlist implements Configuration {

	XMLPropertyListConfiguration configuration

	ConfigurationFromPlist(String plistPath) {
		this(new File(plistPath))
	}


	ConfigurationFromPlist(File plistFile) {
		if (!plistFile.exists()) {
			throw new FileNotFoundException(plistFile.path)
		}
		configuration = new XMLPropertyListConfiguration()
		configuration.read(new FileReader(plistFile))

	}

	@Override
	Object get(String key) {
		def result = configuration.getProperty(escapeKey(key))
		if (result instanceof BigInteger) {
			return result.intValue() // convert to int because the integer in the plist cannot be a bigInteger
		}
		return result
	}

	@Override
	String getString(String key) {
		def result = get(key)
		if (result instanceof String) {
			return result
		}
		if (result instanceof Boolean) {
			return result.toString()
		}
		if (result instanceof Number) {
			return result.toString()
		}
		return null
	}

	@Override
	List<String> getStringArray(Object key) {
		def result = configuration.getList(key)
		//def result = plistHelper.getValueFromPlist(plistFile, key)
		if (result instanceof List<String>) {
			return result
		}
		return []
	}

	@Override
	Set<String> getKeys() {
		def result = []
		configuration.getKeys().each {
			def unescapedKey = it.replace("..", ".")
			result << unescapedKey
		}
		return result as Set<String>
	}

	String escapeKey(String key) {
		return key.replace(".", "..")
	}

	@Override
	boolean containsKey(String key) {
		return configuration.containsKey(escapeKey(key))
	}

	@Override
	Set<String> getReplaceEntitlementsKeys() {
		return ["com.apple.developer.associated-domains"]
	}

	Set<String> getDeleteEntitlementsKeys() {
		return []
	}
}
