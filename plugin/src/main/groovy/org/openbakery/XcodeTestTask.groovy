package org.openbakery

import groovy.json.JsonOutput
import groovy.json.StringEscapeUtils
import org.gradle.api.tasks.TaskAction


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
						//passed (0.006 seconds)

	def DURATION_PATTERN = ~/^\w+\s\((\d+\.\d+).*/


	XcodeTestTask() {
		super()
		dependsOn('keychain-create', 'provisioning-install')
		this.description = "Run the unit test fo the Xcode project"
	}

	@TaskAction
	def test() {
		if (project.xcodebuild.scheme == null && project.xcodebuild.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}

		def commandList = createCommandList()

		commandList.add('test');

		try {
			commandRunner.runCommandWithResult(commandList)
		} finally {
			parseResult(commandRunner.getResult());
		}
		println "Done"
		println "--------------------------------------------------------------------------------"
		println "--------------------------------------------------------------------------------"

	}


	def parseResult(String result) {

		def allResults = [:]

		def resultList = []

		int testRun = 0;

		StringBuilder output = new StringBuilder()
		for (String line in result.split("\n")) {

			def matcher = TEST_CASE_PATTERN.matcher(line)

			def ALL_TESTS_FINISHED = "Test Suite 'All tests' finished at";

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
					TestResult testResult = testClass.results.find { testResult -> testResult.method.equals(method)}

					if (testResult != null) {
						testResult.output = output.toString()
						testResult.success = message.startsWith("passed")

						def durationMatcher = DURATION_PATTERN.matcher(message)
						if (durationMatcher.matches()) {
							testResult.duration = Float.parseFloat(durationMatcher[0][1])
						}
					}
				}
			} else if (line.startsWith(ALL_TESTS_FINISHED)) {
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

	}



	def store(def allResults) {

		def builder = new groovy.json.JsonBuilder()

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
												String s -> StringEscapeUtils.escapeJavaScript(s)
											}
										]
									}
								]
							}
					]


		}

		builder(list)


		File outputDirectory = new File(project.getBuildDir(), "test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		new File(outputDirectory, "results.json").withWriter { out ->
			out.write(builder.toPrettyString())
		}
	}
}
