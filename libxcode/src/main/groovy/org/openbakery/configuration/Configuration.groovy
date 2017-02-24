package org.openbakery.configuration

/**
 * Created by rene on 23.02.17.
 */
interface Configuration {

	Object get(String key)
	String getString(String key)
	List<String> getStringArray(key)

	Set<String> getKeys()

	boolean containsKey(String key)

}
