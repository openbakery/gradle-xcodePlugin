package org.openbakery

import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.apache.commons.io.input.ReversedLinesFileReader
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory
import org.gradle.logging.StyledTextOutput
import org.gradle.logging.StyledTextOutputFactory
import org.openbakery.simulators.SimulatorApp
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

	HashMap<Destination, List<TestClass>> allResults


	def TEST_CASE_PATTERN = ~/^Test Case '(.*)'(.*)/

	def TEST_CLASS_PATTERN = ~/-\[([\w\.]*)\s(\w*)\]/

	def TEST_FAILED_PATTERN = ~/.*\*\* TEST FAILED \*\*/
	def TEST_SUCCEEDED_PATTERN = ~/.*\*\* TEST SUCCEEDED \*\*/

	def TEST_SUITE_PATTERN = ~/.*Test Suite '(.*)'(.*)/


	def DURATION_PATTERN = ~/^\w+\s\((\d+\.\d+).*/

	File outputDirectory = null

	XcodeTestTask() {
		super()
		dependsOn(
						XcodePlugin.XCODE_CONFIG_TASK_NAME,
						XcodePlugin.KEYCHAIN_CREATE_TASK_NAME,
						XcodePlugin.PROVISIONING_INSTALL_TASK_NAME,
		)

		this.description = "Runs the unit tests for the Xcode project"
	}

	@TaskAction
	def test() {
		if (project.xcodebuild.scheme == null && project.xcodebuild.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}

		outputDirectory = new File(project.getBuildDir(), "test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}


		if (project.xcodebuild.sdk.equals(XcodePlugin.SDK_IPHONESIMULATOR)) {
       new SimulatorApp(commandRunner).killAll()
		}

		def commandList = createCommandList()

		// Run the command in a pseudo-terminal to force line-buffered output
		// (Otherwise stderr can corrupt the stdout output)
		commandList = ["script", "-q", "/dev/null"] + commandList

		addIOSSimulatorTargets(commandList)


		commandList.add('test');

		File outputFile = new File(outputDirectory, "xcodebuild-output.txt")
		commandRunner.setOutputFile(outputFile);

		ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class);
		ProgressLogger progressLogger = progressLoggerFactory.newOperation(XcodeTestTask.class).start("XcodeTestTask", "XcodeTestTask");


		try {
			StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(XcodeBuildTask.class, LogLevel.LIFECYCLE)
			TestBuildOutputAppender outputAppender = new TestBuildOutputAppender(progressLogger, output, project)
			commandRunner.run(project.projectDir.absolutePath, commandList, null, outputAppender)
		} catch (CommandRunnerException ex) {
			throw new Exception("Error attempting to run the unit tests!", ex);
		} finally {
			if (!parseResult(outputFile)) {
				//logger.lifecycle("Tests Failed!")
				//logger.lifecycle(getFailureFromLog(outputFile));
				throw new Exception("Not all unit tests are successful!")
			}
			//logger.lifecycle("Done")
		}
	}


	void addIOSSimulatorTargets(ArrayList commandList) {
		if (project.xcodebuild.isSDK(XcodePlugin.SDK_MACOSX)) {
			return
		}

		for (Destination destination in project.xcodebuild.availableDestinations) {

			def destinationParameters = []

			if (destination.platform != null) {
				destinationParameters << "platform=" + destination.platform
			}
			if (destination.id != null) {
				destinationParameters << "id=" + destination.id
			} else {
				if (destination.name != null) {
					destinationParameters << "name=" + destination.name
				}
				if (destination.arch != null && destination.platform.equals("OS X")) {
					destinationParameters << "arch=" + destination.arch
				}

				if (destination.os != null && destination.platform.equals("iOS Simulator")) {
					destinationParameters << "OS=" + destination.os
				}
			}

			commandList.add("-destination")
			commandList.add(destinationParameters.join(","))


		}
	}


	boolean parseResult(File outputFile) {
		logger.debug("parse result from: {}", outputFile)
		if (!outputFile.exists()) {
			logger.lifecycle("No xcodebuild output file found!");
			return false;
		}
		boolean overallTestSuccess = true;
		this.allResults = new HashMap<Destination, ArrayList<TestClass>>()

		def resultList = []

		List<String> testSuites = null;

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
								logger.lifecycle("test + " + testResult + "failed!")
								overallTestSuccess = false;
							}

							def durationMatcher = DURATION_PATTERN.matcher(message)
							if (durationMatcher.matches()) {
								testResult.duration = Float.parseFloat(durationMatcher[0][1])
							}
						}
					} else {
						logger.lifecycle("No TestClass found for name: " + testClassName + " => " + line)
					}
				}
			}

			def testSuiteMatcher = TEST_SUITE_PATTERN.matcher(line)
			if (testSuiteMatcher.matches()) {

				String testSuiteName = testSuiteMatcher[0][1].trim();
				def testSuiteAction = testSuiteMatcher[0][2].trim();


				if (testSuiteAction.startsWith('started')) {
					if (testSuites == null) {
						testSuites = new ArrayList<String>();
					}
					testSuites.add(testSuiteName);
				} else if (testSuiteAction.startsWith('finished') || testSuiteAction.startsWith('passed') || testSuiteAction.startsWith('failed')) {
					testSuites.remove(testSuiteName);
				}


			}

			def testSuccessMatcher = TEST_SUCCEEDED_PATTERN.matcher(line)
			def testFailedMatcher = TEST_FAILED_PATTERN.matcher(line)

			if (testSuccessMatcher.matches() || testFailedMatcher.matches()) {
				testRun ++;
				endOfDestination = true
			}

			if (testFailedMatcher.matches()) {
				overallTestSuccess = false;
			}


			if( endOfDestination ) {
				Destination destination = project.xcodebuild.availableDestinations[testRun]

				if (this.allResults.containsKey(destination)) {
					def destinationResultList = this.allResults.get(destination)
					destinationResultList.addAll(resultList);
				} else {
					this.allResults.put(destination, resultList)
				}

				resultList = []
				testSuites = null
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
		store()
		logger.lifecycle("");
		if (overallTestSuccess) {
			logger.lifecycle("All " + numberSuccess() + " tests were successful");
		} else {
			logger.lifecycle(numberSuccess() + " tests were successful, and " + numberErrors() + " failed");
		}

		return overallTestSuccess;
	}


	def store() {
		logger.debug("store to test-result.xml")
		FileWriter writer = new FileWriter(new File(outputDirectory, "test-results.xml"))

		def xmlBuilder = new MarkupBuilder(writer)

		xmlBuilder.testsuites() {
			for (Destination destination in project.xcodebuild.availableDestinations) {
				String name = destination.toPrettyString()

				def resultList = this.allResults[destination]

				int success = 0;
				int errors = 0;
				if (resultList != null) {
					success = numberSuccess(resultList);
					errors = numberErrors(resultList);
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


	int numberSuccess() {
		int success = 0;
		for (java.util.List list in this.allResults.values()) {
			success += numberSuccess(list);
		}
		return success;
	}

	int numberErrors() {
		int errors = 0;
		for (java.util.List list in this.allResults.values()) {
			errors += numberErrors(list);
		}
		return errors;
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

	def storeJson() {
		logger.lifecycle("Saving test results")

		def list = [];
		for (Destination destination in project.xcodebuild.availableDestinations) {

			def resultList = this.allResults[destination]

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
