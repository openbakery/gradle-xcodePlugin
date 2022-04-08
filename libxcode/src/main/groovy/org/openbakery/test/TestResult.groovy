package org.openbakery.test

public class TestResult {
	public enum State {
		Passed,
		Failed,
		Skipped
	}


	String method;
	String output = "";
	TestResultAttachment[] attachments = [];
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

class TestResultAdditionalInfo {
	String name = ""
	TestResultAttachment[] testResultAttachments = []

	@Override
	public String toString() {
		return "TestResultAdditionalInfo{" +
			"name='" + name + '\'' +
			", testResultAttachments=" + Arrays.toString(testResultAttachments) +
			'}';
	}
}

class TestResultAttachment {
	String name = ""
	String id = ""

	@Override
	public String toString() {
		return "TestResultAttachment{" +
			"name='" + name + '\'' +
			", id='" + id + '\'' +
			'}';
	}
}
