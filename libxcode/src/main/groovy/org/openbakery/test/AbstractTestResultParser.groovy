package org.openbakery.test

import groovy.xml.MarkupBuilder
import org.openbakery.xcode.Destination
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractTestResultParser {
	private static Logger logger = LoggerFactory.getLogger(AbstractTestResultParser.class)

	def testResults = new HashMap<Destination, ArrayList<TestClass>>()

	void store(File outputDirectory) {
		logger.debug("store to test-result.xml")
		FileWriter writer = new FileWriter(new File(outputDirectory, "test-results.xml"))

		def xmlBuilder = new MarkupBuilder(writer)

		xmlBuilder.testsuites() {
			for (result in testResults) {
				String name = result.key.toPrettyString()

				def resultList = result.value
				int success = 0;
				int errors = 0;
				if (resultList != null) {
					success = number(resultList, TestResult.State.Passed);
					errors = number(resultList, TestResult.State.Failed);
				}

				testsuite(name: name, tests: success, errors: errors, failures: "0", skipped: "0") {

					for (TestClass testClass in resultList) {

						for (TestResult testResult in testClass.results) {
							logger.debug("testResult: {}", testResult)
							testcase(classname: testClass.name, name: testResult.method,
								time: new BigDecimal(testResult.duration).toPlainString()) {
								if (testResult.state == TestResult.State.Failed) {
									error(type: "failure", message: "${testResult.output}")
								}
								for(TestResultAttachment attachment in testResult.attachments) {
									'system-out'("[[ATTACHMENT|${attachment.name}]]")
								}
							}
						}
					}
				}
			}
		}
	}

	Destination findDestinationForIdentifier(List<Destination> destinations, String identifier) {
		for (destination in destinations) {
			if (destination.id == identifier) {
				return destination
			}
		}
		return null
	}

	public int number(TestResult.State state) {
		return number(testResults, state)
	}

	public int number(HashMap<Destination, ArrayList<TestClass>> results, TestResult.State state) {
		int success = 0;
		for (List list in results.values()) {
			success += number(list, state)
		}
		return success;
	}

	int number(java.util.List results, TestResult.State state) {
		int number = 0;
		for (TestClass testClass in results) {
			number += testClass.number(state)
		}
		return number
	}

	int numberSuccessInResultList(java.util.List results) {
		int success = 0;
		for (TestClass testClass in results) {
			success += testClass.numberSuccess()
		}
		return success
	}

	int numberErrorsInResultList(java.util.List results) {
		int errors = 0;
		for (TestClass testClass in results) {
			errors += testClass.numberErrors()
		}
		return errors
	}

	public int numberSuccess() {
		return numberSuccess(testResults)
	}


	public int numberSuccess(HashMap<Destination, ArrayList<TestClass>> results) {
		int success = 0;
		for (java.util.List list in results.values()) {
			success += numberSuccessInResultList(list);
		}
		return success;
	}

	public int numberErrors() {
		return numberErrors(testResults)
	}

	public int numberErrors(HashMap<Destination, ArrayList<TestClass>> results) {
		int errors = 0;
		for (java.util.List list in results.values()) {
			errors += numberErrorsInResultList(list);
		}
		return errors;
	}
}
