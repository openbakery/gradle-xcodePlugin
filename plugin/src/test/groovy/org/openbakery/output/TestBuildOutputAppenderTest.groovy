package org.openbakery.output

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.Destination
import org.openbakery.XcodePlugin
import org.openbakery.stubs.ProgressLoggerStub
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 09.10.13
 * Time: 18:19
 * To change this template use File | Settings | File Templates.
 */
class TestBuildOutputAppenderTest {



	def errorTestOutput = "Test Case '-[DTActionPanelTest_iPhone testActionPanelSizeDidChangeDelegate]' started.\n" +
					"2013-10-09 18:12:12:101 FOO[22741:c07] layoutSubviews\n" +
					"2013-10-09 18:12:12:101 FOO[22741:c07] oldFrame {{0, 380}, {320, 80}}\n" +
					"2013-10-09 18:12:12:102 FOO[22741:c07] newFrame {{0, 320}, {320, 140}}\n" +
					"/Users/dummy/poject/UnitTests/iPhone/DTPopoverController/DTActionPanelTest_iPhone.m:85: error: -[DTActionPanelTest_iPhone testActionPanelSizeDidChangeDelegate] : Expected 2 matching invocations, but received 0\n" +
					"Test Case '-[DTActionPanelTest_iPhone testActionPanelSizeDidChangeDelegate]' failed (0.026 seconds).\n" +
					"Test Case '-[DTActionPanelTest_iPhone testActionPanelSizeDidChangeDelegateOnHide]' started."


	def successTestOutput = "Test Case '-[DTActionPanelTest_iPhone testCollapsed]' started.\n" +
					"2013-10-09 18:12:12:108 FOO[22741:c07] newFrame {{0, 320}, {320, 140}}\n" +
					"2013-10-09 18:12:12:112 FOO[22741:c07] empty\n" +
					"2013-10-09 18:12:12:113 FOO[22741:c07] empty\n" +
					"Test Case '-[DTActionPanelTest_iPhone testCollapsed]' passed (0.005 seconds)."


	Project project

	@BeforeClass
	def setup() {
		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		Destination destinationPad = new Destination()
		destinationPad.platform = XcodePlugin.SDK_IPHONESIMULATOR
		destinationPad.name = "iPad"
		destinationPad.arch = "i386"
		destinationPad.id = "iPad Air"
		destinationPad.os = "iOS"

		Destination destinationPhone = new Destination()
		destinationPhone.platform = XcodePlugin.SDK_IPHONESIMULATOR
		destinationPhone.name = "iPhone"
		destinationPhone.arch = "i386"
		destinationPhone.id = "iPhone 4s"
		destinationPhone.os = "iOS"


		project.xcodebuild.availableSimulators << destinationPad
		project.xcodebuild.availableSimulators << destinationPhone

		project.xcodebuild.destination {
			name = "iPad"
		}
		project.xcodebuild.destination {
			name = "iPhone"
		}

	}

	@Test
	void testNoOutput() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, project)

		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		assert output.toString().equals("") : "Expected empty output but was " + output

	}

	@Test
	void testSuccess() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, project)
		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		for (String line in successTestOutput.split("\n")) {
				appender.append(line)
		}
		String expected = "\nPerform unit tests for: iPad/" + XcodePlugin.SDK_IPHONESIMULATOR + "/iOS\n\n"
		assert output.toString().equals(expected) : "Expected '" + expected + "' but was: " + output.toString()
	}



	@Test
	void testSuccess_fullProgress() {

		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, project)
		appender.fullProgress = true;
		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		for (String line in successTestOutput.split("\n")) {
				appender.append(line)
		}
		String expected = "\nPerform unit tests for: iPad/" + XcodePlugin.SDK_IPHONESIMULATOR + "/iOS\n\n      OK -[DTActionPanelTest_iPhone testCollapsed] - (0.005 seconds)\n"
		assert output.toString().equals(expected) : "Expected '" + expected + "' but was: " + output.toString()
	}

	@Test
	void testSuccess_Progress() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		ProgressLoggerStub progress = new ProgressLoggerStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(progress, output, project)
		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		for (String line in successTestOutput.split("\n")) {
				appender.append(line)
		}

		assertThat(progress.progress, hasItem("0 tests completed, running 'DTActionPanelTest_iPhone'"))

	}

	@Test
	void testSuccess_progress_complex() {
		String xcodebuildOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-complex-test.txt"))
		StyledTextOutputStub output = new StyledTextOutputStub()
		ProgressLoggerStub progress = new ProgressLoggerStub()
		TestBuildOutputAppender appender = new TestBuildOutputAppender(progress, output, project)
		for (String line : xcodebuildOutput.split("\n")) {
			appender.append(line);
		}

		assertThat(progress.progress, hasItem("0 tests completed, running 'TestGoogleWebStreetViewProvider'"))
		assertThat(progress.progress, hasItem("1 tests completed, running 'TestGoogleWebStreetViewProvider'"))
		assertThat(progress.progress, hasItem("4 tests completed, running 'TestGoogleWebStreetViewProvider'"))
		assertThat(progress.progress, hasItem("5 tests completed, running 'TestMapFeatureProviderUtil'"))
		assertThat(progress.progress, hasItem("Tests finished: iPad/iphonesimulator/iOS"))

		assertThat(output.toString(), containsString("30 tests completed"))
		int matches = StringUtils.countMatches(output.toString(), "30 tests completed");
		assertThat(matches, is(2))


	}

	@Test
	void testSuccess_progress_with_failed() {
		String xcodebuildOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))
		StyledTextOutputStub output = new StyledTextOutputStub()
		ProgressLoggerStub progress = new ProgressLoggerStub()
		TestBuildOutputAppender appender = new TestBuildOutputAppender(progress, output, project)
		for (String line : xcodebuildOutput.split("\n")) {
			appender.append(line);
		}

		assertThat(progress.progress, hasItem("0 tests completed, running 'ExampleTests'"))
		assertThat(output.toString(), containsString("1 tests completed, 1 failed"))
	}



	@Test
	void testFailed() {

		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, project)
		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		for (String line in errorTestOutput.split("\n")) {
				appender.append(line)
		}
		String expected = "\nPerform unit tests for: iPad/" + XcodePlugin.SDK_IPHONESIMULATOR + "/iOS\n\n  FAILED -[DTActionPanelTest_iPhone testActionPanelSizeDidChangeDelegate] - (0.026 seconds)\n"
		assert output.toString().equals(expected) : "Expected '" + expected + "' but was: " + output.toString()
	}



/*
	@Test
	void testFinished() {
		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output.txt"))

		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, project)

		for (String line : simctlOutput.split("\n")) {
			appender.append(line);
		}

		assert output.toString().contains("Tests finished:")

	}
*/

	@Test
	void testFinishedFailed() {
		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))

		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, project)

		for (String line : simctlOutput.split("\n")) {
			appender.append(line);
		}

		assert output.toString().contains("TESTS FAILED")

	}

	@Test
	void testComplexOutput() {
		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-complex-test.txt"))
		StyledTextOutputStub output = new StyledTextOutputStub()
		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, project)
		for (String line : simctlOutput.split("\n")) {
			appender.append(line);
		}
		assert output.toString().contains("Perform unit tests for: iPad/" + XcodePlugin.SDK_IPHONESIMULATOR + "/iOS")
		assert output.toString().contains("Perform unit tests for: iPhone/" + XcodePlugin.SDK_IPHONESIMULATOR + "/iOS")
	}

}
