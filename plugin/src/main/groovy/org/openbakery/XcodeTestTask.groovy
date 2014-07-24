package org.openbakery

import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.StyledTextOutput
import org.gradle.logging.StyledTextOutputFactory
import org.openbakery.output.TestBuildOutputAppender
import org.openbakery.output.XcodeBuildOutputAppender


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




/**
 * User: rene
 * Date: 12.07.13
 * Time: 09:19
 */
class XcodeTestTask extends AbstractXcodeBuildTask {


	def TEST_CASE_PATTERN = ~/^Test Case '(.*)'(.*)/

	def TEST_CLASS_PATTERN = ~/^-\[(\w*)\s(\w*)\]/

	def DURATION_PATTERN = ~/^\w+\s\((\d+\.\d+).*/


	XcodeTestTask() {
		super()
		dependsOn('keychain-create', 'provisioning-install')
		this.description = "Runs the unit tests for the Xcode project"
	}

	@TaskAction
	def test() {
		if (project.xcodebuild.scheme == null && project.xcodebuild.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}

		def commandList = createCommandList()


		for (Destination destination in project.xcodebuild.destinations) {

			StringBuilder destinationBuilder = new StringBuilder();
			if (destination.platform != null) {
				destinationBuilder.append("platform=");
				destinationBuilder.append(destination.platform)
			}
			if (destination.name != null) {
				if (destinationBuilder.length() > 0) {
					destinationBuilder.append(",")
				}
				destinationBuilder.append("name=");
				destinationBuilder.append(destination.name)
			}
			if (destination.arch != null && destination.platform.equals("OS X")) {
				if (destinationBuilder.length() > 0) {
					destinationBuilder.append(",")
				}
				destinationBuilder.append("arch=");
				destinationBuilder.append(destination.arch)
			}

			if (destination.os != null && destination.platform.equals("iOS Simulator")) {
				if (destinationBuilder.length() > 0) {
					destinationBuilder.append(",")
				}
				destinationBuilder.append("OS=");
				destinationBuilder.append(destination.os)
			}

			commandList.add("-destination")
			commandList.add(destinationBuilder.toString());
		}



		commandList.add('test');

		try {
			StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(XcodeBuildTask.class)
			commandRunner.run("${project.projectDir.absolutePath}", commandList, null, new TestBuildOutputAppender(output, project))
		} finally {
			String commandOutput = commandRunner.getResult();
			File outputDirectory = new File(project.getBuildDir(), "test");
			if (!outputDirectory.exists()) {
				outputDirectory.mkdirs()
			}
			new File(outputDirectory, "xcodebuild-output.txt").withWriter { out ->
				out.write(commandOutput)
			}
			if (!parseResult(commandOutput)) {
				logger.quiet("Tests Failed!");
				throw new Exception("Not all unit tests are successful!");
			};
			logger.quiet("Done")
		}
	}


	boolean parseResult(String result) {

		boolean overallTestSuccess = true;
		def allResults = [:]

		def resultList = []

		int testRun = 0;

		StringBuilder output = new StringBuilder()
		for (String line in result.split("\n")) {

			def matcher = TEST_CASE_PATTERN.matcher(line)

			def ALL_TESTS_SUCCEEDED = "** TEST SUCCEEDED **";
			def ALL_TESTS_FAILED = "** TEST FAILED **";

			if (matcher.matches()) {

				String message = matcher[0][2].trim()

				def nameMatcher = TEST_CLASS_PATTERN.matcher(matcher[0][1])
				if (!nameMatcher.matches()) {
					continue;
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
								logger.quiet("test + " + testResult + "failed!")
								overallTestSuccess = false;
							}

							def durationMatcher = DURATION_PATTERN.matcher(message)
							if (durationMatcher.matches()) {
								testResult.duration = Float.parseFloat(durationMatcher[0][1])
							}
						}
					} else {
						logger.quiet("No TestClass found for name: " + testClassName + " => " + line)
					}
				}
			} else if (line.startsWith(ALL_TESTS_SUCCEEDED) || line.startsWith(ALL_TESTS_FAILED)) {
				Destination destination = project.xcodebuild.destinations[testRun]
				allResults.put(destination, resultList)
				testRun ++;

				resultList = []
			} else {
				if (output != null) {
					if (output.length() > 0) {
						output.append("\n")
					}
					output.append(line)
				}
			}
		}

		store(allResults)
		if (overallTestSuccess) {
			logger.quiet("All " + numberSuccess(allResults) + " tests where successful");
		} else {
			logger.quiet(numberSuccess(allResults) + " tests where successful, and " + numberErrors(allResults) + " failues");
		}
		//logger.quiet("overallTestResult " + overallTestSuccess)

		return overallTestSuccess;
	}


	def store(def allResults) {

		File outputDirectory = new File(project.getBuildDir(), "test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		FileWriter writer = new FileWriter(new File(outputDirectory, "test-results.xml"))

		def xmlBuilder = new MarkupBuilder(writer)

		xmlBuilder.testsuites() {
			for (Destination destination in project.xcodebuild.destinations) {
				String name = destination.platform + " / " + destination.name + " / " + destination.os

				def resultList = allResults[destination]

				int success = 0;
				int errors = 0;
				if (resultList != null) {
					success = numberSuccess(resultList);
					errors = numberErrors(resultList);
				}

				testsuite(name: name, tests: success, errors: errors, failures: "0", skip: "0") {

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


	int numberSuccess(java.util.Map results) {
		int success = 0;
		for (java.util.List list in results.values()) {
			success += numberSuccess(list);
		}
		return success;
	}

	int numberErrors(java.util.Map results) {
		int success = 0;
		for (java.util.List list in results.values()) {
			success += numberErrors(list);
		}
		return success;
	}

	int numberSuccess(java.util.List results) {
		int success = 0;
		for (TestClass testClass in results) {
			success += testClass.numberSuccess()
		}
		return success
	}

	int numberErrors(java.util.List results) {
		int errors = 0;
		for (TestClass testClass in results) {
			errors += testClass.numberErrors()
		}
		return errors
	}

	def storeJson(def allResults) {
		logger.quiet("Saving test results")

		def list = [];
		for (Destination destination in project.xcodebuild.destinations) {

			def resultList = allResults[destination]

			list << [
						destination:
							[
								name : destination.name,
								platform : destination.platform,
								arch: destination.arch,
								id: destination.id,
								os: destination.os
							],
						results:
							resultList.collect {
								TestClass t -> [
									name: t.name,
									result: t.results.collect {
										TestResult r ->	[
											method: r.method,
											success: r.success,
											duration: r.duration,
											output: r.output.split("\n").collect {
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


}
