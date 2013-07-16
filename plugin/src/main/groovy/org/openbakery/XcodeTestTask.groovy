package org.openbakery

import org.gradle.api.tasks.TaskAction


class TestResult {
	String method;
	boolean success;
	String output;


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

	//def TEST_START_PATTERN = ~/^Test Case '(.*)' started.*/
	//def TEST_PASSED_TOKEN = ~/^Test Case '(.*)' passed.*/
	//def TEST_FAILED_TOKEN = ~/^Test Case '(.*)' failed.*/


	XcodeTestTask() {
		super()
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


		def resultMap = [:]

		def resultList = []


		StringBuilder output = null
		for (String line in result.split("\n")) {

			def matcher = TEST_CASE_PATTERN.matcher(line)
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

/*
					def testsForClass = resultMap.get(testClass)
					if (testsForClass == null) {
						testsForClass = []
					}
					testsForClass.add(testCase);
					resultMap.put(testCase.testClass, testsForClass);
					println testsForClass
*/
				} else {
					//TestCase testCase = resultMap.get(testClass).find{ testCase -> testCase.method.equals(method) }
					TestClass testClass = resultList.find { testClass -> testClass.name.equals(testClassName) }
					TestResult testResult = testClass.results.find { testResult -> testResult.method.equals(method)}

					if (testResult != null) {
						testResult.output = output.toString()
						testResult.success = message.startsWith("passed");
					}
				}
			} else {
				if (output != null) {
					if (output.length() > 0) {
						output.append("\n")
					}
					output.append(line)
				}
			}
		}

		def builder = new groovy.json.JsonBuilder()


		def list = resultList.collect{
			TestClass t -> [
							name: t.name,
							result: t.results.collect {
								TestResult r -> [
								        method: r.method,
												success: r.success,
												output: r.output
								]
							}]
		}
		builder(list)






		println "RESULTS: " + resultList
/*
		builder.resultList() {
			resultList.each { TestClass testClass -> name(
							name     : testClass.name,
							tests: testClass.each {
								TestResult testResult -> result(
												method : testResult.method,
												success : testResult.success,
												output : testResult.output
								)
							})}
		}
		*/
		/*


		,
							tests : testClass.results.each {
								TestResult testResult -> result(
												method : testResult.method,
												success : testResult.success,
												output : testResult.output
								)

							}

		def root = builder.results {
			test {
				name "${p.name}"

				addresses(
								p.addresses.collect{
									Address a -> [addr: a.address, state:a.state]
								}
				)
			}
		}
*/
/*
		def list = resultList.collect{
			TestCase testCase -> [
							testClass     : testCase.testClass,
							method	: testCase.method,
							success : testCase.success,
							output   : testCase.output]}
		builder(results: list)
*/

		//
/*
		builder.results() {
			resultMap.values().each { TestCase testCase -> result(
							name     : testCase.name,
							success : testCase.success,
							output   : testCase.output)}
		}
*/

		File outputDirectory = new File(project.getBuildDir(), "test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		new File(outputDirectory, "unittest-result.json").withWriter { out ->
			out.write(builder.toPrettyString())
		}
	}


}
