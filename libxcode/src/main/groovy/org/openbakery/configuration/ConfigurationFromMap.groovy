package org.openbakery.configuration

class ConfigurationFromMap implements Configuration {

	Map<String, Object> configurationMap = null

	ConfigurationFromMap(Map<String, Object> configurationMap) {
		if (configurationMap == null) {
			throw new IllegalArgumentException("given configuration map is null")
		}
		this.configurationMap = configurationMap
	}


	@Override
	Object get(String key) {
		return configurationMap[key]
	}

	@Override
	String getString(String key) {

		def value = configurationMap[key]
		if (value instanceof String) {
			return value
		}
		if (value instanceof Boolean) {
			return value.toString()
		}
		if (value instanceof Number) {
			return value.toString()
		}

		return null
	}

	@Override
	List<String> getStringArray(Object key) {
		def value = configurationMap[key]
		if (value instanceof List<String, Object>) {
			return value
		}
		return []
	}

	@Override
	Set<String> getKeys() {
		return configurationMap.keySet()
	}

	@Override
	boolean containsKey(String key) {
		return configurationMap.containsKey(key)
	}

	@Override
	Set<String> getReplaceEntitlementsKeys() {
		// for now all keys are marked for replacement, because the configuration is now used for the entitlements
		// only.
		return configurationMap.keySet()
	}

	Set<String> getDeleteEntitlementsKeys() {
		return configurationMap.findAll{ it.value == null }.keySet()
	}
}
