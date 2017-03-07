package org.openbakery.configuration

interface Configuration {

	Object get(String key)
	String getString(String key)
	List<String> getStringArray(key)

	Set<String> getKeys()

	boolean containsKey(String key)

}
