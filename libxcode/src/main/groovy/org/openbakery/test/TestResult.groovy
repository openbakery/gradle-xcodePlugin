package org.openbakery.test

public class TestResult {
	public enum State {
		Passed,
		Failed,
		Skipped
	}


	String method;
	String output = "";
	float duration;
	State state = State.Passed


	@Override
	public String toString() {
		return "TestResult{" +
						"method='" + method + '\'' +
						", state=" + state +
						", output='" + output + '\'' +
						'}';
	}
}


