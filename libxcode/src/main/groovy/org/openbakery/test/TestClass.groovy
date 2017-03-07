package org.openbakery.test

class TestClass {
	String name
	List results = []

	int numberSuccess() {
		int success = 0;
		for (TestResult result in results) {
			if (result.success) {
				success++
			}
		}
		return success;
	}

	int numberErrors() {
		int errors = 0;
		for (TestResult result in results) {
			if (!result.success) {
				errors++
			}
		}
		return errors;
	}

	@Override
	public java.lang.String toString() {
		return "TestClass{" +
						"name='" + name + '\'' +
						", results=" + results +
						'}';
	}
}