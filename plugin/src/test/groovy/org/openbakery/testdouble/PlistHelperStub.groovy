package org.openbakery.testdouble

import org.openbakery.util.PlistHelper

/**
 * Created by rene on 01.09.15.
 */
class PlistHelperStub extends PlistHelper {


	HashMap<String, String> plistValues = new HashMap<>();

	ArrayList<String> plistCommands = new ArrayList<>();


	PlistHelperStub(Map<String, String> values) {
		this()
		this.plistValues.putAll(values)
	}

	PlistHelperStub() {
		super(null, null)
	}

	@Override
	def getValueFromPlist(Object plist, Object key) {
		if (plistValues.containsKey(key)) {
			return plistValues.get(key)
		}

		String uniqueKey = getUniqueKey(plist, key)
		if (plistValues.containsKey(uniqueKey)) {
			return plistValues.get(uniqueKey)
		}


		return null;
	}

	private String getUniqueKey(plist, key) {
		if (plist instanceof File) {
			return ((File) plist).absolutePath + "_" + key;
		}
		return plist.toString() + "_" + key;
	}

	@Override
	void setValueForPlist(def Object plist, String key, List values) {
		plistValues.put(getUniqueKey(plist, key), values)
	}

	@Override
	void setValueForPlist(def Object plist, String key, String value) {
		plistValues.put(getUniqueKey(plist, key), value)
	}

	@Override
	void commandForPlist(def Object plist, String command) {
		plistCommands.add(command)
	}
}
