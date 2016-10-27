package org.openbakery.test

/**
 * Created by rene on 27.10.16.
 */
class TestResult {
	String method;
	boolean success;
	String output = "";
	float duration;


	@Override
	public java.lang.String toString() {
		return "TestResult{" +
						"method='" + method + '\'' +
						", success=" + success +
						", output='" + output + '\'' +
						'}';
	}
}


