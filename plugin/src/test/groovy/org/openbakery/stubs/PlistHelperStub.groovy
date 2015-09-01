package org.openbakery.stubs

import org.openbakery.PlistHelper

/**
 * Created by rene on 01.09.15.
 */
class PlistHelperStub extends PlistHelper {


	Map<String, String> values


	PlistHelperStub(Map<String, String> values) {
		super(null, null)
		this.values = values
	}

	@Override
	def getValueFromPlist(Object plist, Object key) {
		if (values.containsKey(key)) {
			return values.get(key)
		}
		return null;
	}
}
