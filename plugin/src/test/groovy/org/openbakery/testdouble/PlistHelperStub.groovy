package org.openbakery.testdouble

import org.openbakery.helpers.PlistHelper

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
		super(null)
	}

	@Override
	def getValueFromPlist(File plist, String key) {
		if (plistValues.containsKey(key)) {
			return plistValues.get(key)
		}

		String uniqueKey = getUniqueKey(plist, key)
		if (plistValues.containsKey(uniqueKey)) {
			return plistValues.get(uniqueKey)
		}


		return null;
	}

	private String getUniqueKey(File plist, key) {
		return plist.absolutePath + "_" + key
	}

	@Override
	void setValueForPlist(File plist, String key, List values) {
		plistValues.put(getUniqueKey(plist, key), values)
	}

	@Override
	void setValueForPlist(File plist, String key, String value) {
		plistValues.put(getUniqueKey(plist, key), value)
	}

	@Override
	void commandForPlist(File plist, String command) {
		plistCommands.add(command)
	}
}
