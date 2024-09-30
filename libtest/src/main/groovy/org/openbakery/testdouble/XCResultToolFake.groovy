package org.openbakery.testdouble

import org.openbakery.test.XCResultTool

import static java.util.Collections.emptyMap

class XCResultToolFake extends XCResultTool {

	XCResultToolFake() {
		super("", false)
	}

	@Override
	Map<String, Object> getObject(File file, String id) {
		return emptyMap()
	}

	@Override
	Map<String, Object> getObject(File file) {
		return emptyMap()
	}
}
