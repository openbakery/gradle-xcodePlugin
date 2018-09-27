package org.openbakery.test

import groovy.xml.MarkupBuilder
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.openbakery.xcode.Destination
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class TestResultParser {

	private static Logger logger = LoggerFactory.getLogger(TestResultParser.class)


	def TEST_CASE_PATTERN = ~/^Test Case '(.*)'(.*)/

	def TEST_CLASS_PATTERN = ~/-\[([\w\.]*)\s(\w*)\]/

	def TEST_FAILED_PATTERN = ~/.*\*\* TEST FAILED \*\*/
	def TEST_SUCCEEDED_PATTERN = ~/.*\*\* TEST SUCCEEDED \*\*/

	def DURATION_PATTERN = ~/^\w+\s\((\d+\.\d+).*/


	File testSummariesDirectory
	List<Destination> destinations

	List<File> testSummariesFiles

	def testResults = new HashMap<Destination, ArrayList<TestClass>>()

	public TestResultParser(File testSummariesDirectory, List<Destination> destinations) {
		this.testSummariesDirectory = testSummariesDirectory
		this.destinations = destinations

		testSummariesFiles = new ArrayList<>();

		def testSummaries = testSummariesDirectory.list(
						[accept: { d, f -> f ==~ /.*TestSummaries.plist/ }] as FilenameFilter
		)
		if (testSummaries != null) {
			testSummaries.toList().each {
				File summary = new File(testSummariesDirectory, it)
				testSummariesFiles.add(summary)
			}
		}

	}


	public TestResultParser(File manifest) {
		//this.manifest = manifest
		//this.testSummariesDirectory = testSummariesDirectory
		//this.destinations = destinations
	}


	public void parseAndStore(File outputDirectory) {
		parse()
		store(outputDirectory)
	}

	private void parse() {
		testSummariesFiles.toList().each {
			def testResult = new XMLPropertyListConfiguration(it)
			def identifier = testResult.getString("RunDestination.TargetDevice.Identifier")

			Destination destination = findDestinationForIdentifier(destinations, identifier)
			if (destination != null) {
				def resultList = processTestSummary(testResult.getList("TestableSummaries"))
				testResults.put(destination, resultList)
			}
		}
	}


	private void store(File outputDirectory) {
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
					success = numberSuccessInResultList(resultList);
					errors = numberErrorsInResultList(resultList);
				}

				testsuite(name: name, tests: success, errors: errors, failures: "0", skipped: "0") {

					for (TestClass testClass in resultList) {

						for (TestResult testResult in testClass.results) {
							logger.debug("testResult: {}", testResult)
							testcase(classname: testClass.name, name: testResult.method, time: testResult.duration) {
								if (!testResult.success) {
									error(type: "failure", message: "", testResult.output)
								}
								'system-out'(testResult.output)
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


	HashMap<Destination, ArrayList<TestClass>> parseResult(File outputFile) {
		def testResults = new HashMap<Destination, ArrayList<TestClass>>()
		logger.debug("parse result from: {}", outputFile)
		if (!outputFile.exists()) {
			logger.info("No xcodebuild output file found!");
			return testResults;
		}
		boolean overallTestSuccess = true;

		def resultList = []

		int testRun = 0;
		boolean endOfDestination = false;

		StringBuilder output = new StringBuilder()

		outputFile.eachLine { line ->


			def matcher = TEST_CASE_PATTERN.matcher(line)
			if (matcher.matches()) {

				String message = matcher[0][2].trim()

				def nameMatcher = TEST_CLASS_PATTERN.matcher(matcher[0][1])
				if (!nameMatcher.matches()) {
					return
				}

				String testClassName = nameMatcher[0][1]
				String method = nameMatcher[0][2]

				if (message.startsWith("started")) {
					output = new StringBuilder()



					TestClass testClass = resultList.find { testClass -> testClass.name.equals(testClassName) }
					if (testClass == null) {
						testClass = new TestClass(name: testClassName);
						resultList << testClass
					}
					testClass.results << new TestResult(method: method)

				} else {
					//TestCase testCase = resultMap.get(testClass).find{ testCase -> testCase.method.equals(method) }
					TestClass testClass = resultList.find { testClass -> testClass.name.equals(testClassName) }
					if (testClass != null) {
						TestResult testResult = testClass.results.find { testResult -> testResult.method.equals(method) }

						if (testResult != null) {
							testResult.output = output.toString()

							testResult.success = !message.toLowerCase().startsWith("failed")
							if (!testResult.success) {
								logger.info("test + " + testResult + "failed!")
								overallTestSuccess = false;
							}

							def durationMatcher = DURATION_PATTERN.matcher(message)
							if (durationMatcher.matches()) {
								testResult.duration = Float.parseFloat(durationMatcher[0][1])
							}
						}
					} else {
						logger.info("No TestClass found for name: " + testClassName + " => " + line)
					}
				}
			}

			def testSuccessMatcher = TEST_SUCCEEDED_PATTERN.matcher(line)
			def testFailedMatcher = TEST_FAILED_PATTERN.matcher(line)

			if (testSuccessMatcher.matches() || testFailedMatcher.matches()) {
				testRun++;
				endOfDestination = true
			}

			if (testFailedMatcher.matches()) {
				overallTestSuccess = false;
			}


			if (endOfDestination) {
				Destination destination = destinations[(testRun - 1)]

				if (testResults.containsKey(destination)) {
					def destinationResultList = testResults.get(destination)
					destinationResultList.addAll(resultList);
				} else {
					testResults.put(destination, resultList)
				}

				resultList = []
				endOfDestination = false
			} else {
				if (output != null) {
					if (output.length() > 0) {
						output.append("\n")
					}
					output.append(line)
				}
			}
		}
		return testResults;
	}





	List<TestClass> processTestSummary(List<XMLPropertyListConfiguration> list) {

		List<TestClass> resultList = []

		for (entry in list) {
			List<XMLPropertyListConfiguration> testList = entry.getList("Tests")
			processTests(testList, resultList)
		}

		return resultList

	}

	List<TestClass> processTests(List<XMLPropertyListConfiguration> list, List<TestClass> resultList) {
		for (entry in list) {
			List<XMLPropertyListConfiguration> testList = entry.getList("Subtests")
			processSubtests(testList, resultList, entry.getString("TestName"))
		}


	}


	List<TestClass> processSubtests(List<XMLPropertyListConfiguration> list, List<TestClass> resultList, String name) {

		TestClass testClass = null

		for (entry in list) {
			if (entry.getString("TestObjectClass") == "IDESchemeActionTestSummary") {
				if (testClass == null) {
					testClass = new TestClass(name: name);
					resultList << testClass
				}

				String method = entry.getString("TestName")
				TestResult testResult = new TestResult(method: method)
				testResult.success = entry.getString("TestStatus") == "Success"
				testClass.results << testResult

			} else if (entry.getString("TestObjectClass") == "IDESchemeActionTestSummaryGroup") {
				List<XMLPropertyListConfiguration> testList = entry.getList("Subtests")
				processSubtests(testList, resultList, entry.getString("TestName"))
			}


		}

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

	/*
	def storeJson() {
		logger.info("Saving test results")

		def list = [];
		for (Destination destination in project.xcodebuild.availableDestinations) {

			def resultList = testResults[destination]

			list << [
							destination:
											[
															name    : destination.name,
															platform: destination.platform,
															arch    : destination.arch,
															id      : destination.id,
															os      : destination.os
											],
							results    :
											resultList.collect {
												TestClass t ->
													[
																	name  : t.name,
																	result: t.results.collect {
																		TestResult r ->
																			[
																							method  : r.method,
																							success : r.success,
																							duration: r.duration,
																							output  : r.output.split("\n").collect {
																								String s -> escapeString(s)
																							}
																			]
																	}
													]
											}
			]

		}

		def builder = new groovy.json.JsonBuilder()
		builder(list)


		File outputDirectory = new File(project.getBuildDir(), "test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		new File(outputDirectory, "results.json").withWriter { out ->
			out.write(builder.toPrettyString())
		}
	}
	*/


	def escapeString(String string) {
		if (string == null) {
			return null;
		}
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < string.length(); i++) {
			char ch = string.charAt(i);
			switch (ch) {
				case '"':
					buffer.append("\\\"");
					break;
				case '\\':
					buffer.append("\\\\");
					break;
				case '\b':
					buffer.append("\\b");
					break;
				case '\f':
					buffer.append("\\f");
					break;
				case '\n':
					buffer.append("\\n");
					break;
				case '\r':
					buffer.append("\\r");
					break;
				case '\t':
					buffer.append("\\t");
					break;
				case '/':
					buffer.append("\\/");
					break;
				default:
					buffer.append(ch);
					break;
			}
		}
		return buffer.toString();
	}

	void mergeResult(HashMap<Destination, ArrayList<TestClass>> fromPlist, HashMap<Destination, ArrayList<TestClass>> fromOutput) {

		fromPlist.each { destination, testClasses ->
			def secondDestinationClasses = fromOutput.get(destination)
			if (secondDestinationClasses != null) {
				mergeTestClasses(testClasses, secondDestinationClasses)
			}
		}
	}


	def mergeTestClasses(ArrayList<TestClass> fromPlist, ArrayList<TestClass> fromOutput) {
		fromPlist.each { testClass ->
			def testClassFromOutput = fromOutput.find { it.name == testClass.name }
			if (testClassFromOutput != null) {
				mergeTestResults(testClass.results, testClassFromOutput.results)
			}

		}

	}

	def mergeTestResults(List<TestResult> fromPlist, List<TestResult> fromOutput) {
		fromPlist.each { testResult ->
			def testResultFromOutput = fromOutput.find { it.method == testResult.method }
			if (testResultFromOutput != null) {
				testResult.duration = testResultFromOutput.duration
				testResult.output = testResultFromOutput.output
			}
		}
	}
}
