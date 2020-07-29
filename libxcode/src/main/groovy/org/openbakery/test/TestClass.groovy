package org.openbakery.test

class TestClass {
	String name
	List results = []

	int number(TestResult.State state) {
		int number = 0;
		for (TestResult result in results) {
			if (result.state == state) {
				number++
			}
		}
		return number;
	}

	@Override
	public String toString() {
		return "TestClass{" +
						"name='" + name + '\'' +
						", results=" + results +
						'}';
	}
}
