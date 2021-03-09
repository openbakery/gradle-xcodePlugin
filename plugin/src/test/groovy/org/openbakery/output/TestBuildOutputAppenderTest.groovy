package org.openbakery.output

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.SimulatorControlFake
import org.openbakery.xcode.Destination
import org.openbakery.testdouble.ProgressLoggerStub
import org.junit.Test
import org.junit.Before
import org.openbakery.xcode.DestinationResolver

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
	List<Destination> destinations

	@Before
	void setup() {
		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		SimulatorControlFake simulatorControl = new SimulatorControlFake("simctl-list-xcode7.txt")

		project.xcodebuild.destination {
			name = "iPad 2"
		}
		project.xcodebuild.destination {
			name = "iPhone 4s"
		}

		DestinationResolver destinationResolver = new DestinationResolver(simulatorControl)
		destinations = destinationResolver.getDestinations(project.xcodebuild.getXcodebuildParameters())
	}

	@Test
	void testNoOutput() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, destinations)

		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		assert output.toString().equals("") : "Expected empty output but was " + output

	}

	@Test
	void testSuccess() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, destinations)
		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		for (String line in successTestOutput.split("\n")) {
				appender.append(line)
		}
		String expected = "\nRun tests for: iPad 2/iOS Simulator/9.0\n"
		assertThat(output.toString(), is(equalTo(expected)))
	}



	@Test
	void testSuccess_fullProgress() {

		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, destinations)
		appender.fullProgress = true;
		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		for (String line in successTestOutput.split("\n")) {
				appender.append(line)
		}
		String expected = "\nRun tests for: iPad 2/iOS Simulator\n\n\n"
		String outputString = output.toString()
		assertThat(outputString, containsString("Run tests for: iPad 2/iOS Simulator"))
		assertThat(outputString, containsString("      OK -[DTActionPanelTest_iPhone testCollapsed] - (0.005 seconds)"))
	}

	@Test
	void testSuccess_Progress() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		ProgressLoggerStub progress = new ProgressLoggerStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(progress, output, destinations)
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
		TestBuildOutputAppender appender = new TestBuildOutputAppender(progress, output, destinations)
		for (String line : xcodebuildOutput.split("\n")) {
			appender.append(line);
		}

		assertThat(progress.progress, hasItem("0 tests completed, running 'TestGoogleWebStreetViewProvider'"))
		assertThat(progress.progress, hasItem("1 tests completed, running 'TestGoogleWebStreetViewProvider'"))
		assertThat(progress.progress, hasItem("4 tests completed, running 'TestGoogleWebStreetViewProvider'"))
		assertThat(progress.progress, hasItem("5 tests completed, running 'TestMapFeatureProviderUtil'"))
		assertThat(progress.progress, hasItem("Tests finished: iPhone 4s/iOS Simulator/9.0"))

		assertThat(output.toString(), containsString("30 tests completed"))
		int matches = StringUtils.countMatches(output.toString(), "30 tests completed");
		assertThat(matches, is(2))


	}

	@Test
	void testSuccess_progress_with_failed() {
		String xcodebuildOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))
		StyledTextOutputStub output = new StyledTextOutputStub()
		ProgressLoggerStub progress = new ProgressLoggerStub()
		TestBuildOutputAppender appender = new TestBuildOutputAppender(progress, output, destinations)
		for (String line : xcodebuildOutput.split("\n")) {
			appender.append(line);
		}

		assertThat(progress.progress, hasItem("0 tests completed, running 'ExampleTests'"))
		assertThat(output.toString(), containsString("1 tests completed, 1 failed"))
	}



	@Test
	void testFailed() {

		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, destinations)
		appender.append("PhaseScriptExecution Copy\\ Pods\\ Resources build/obj/MyApp.build/Debug-iphonesimulator/myApp.build/Script-FCB0D86122C34DC69AE16EE3.sh")

		for (String line in errorTestOutput.split("\n")) {
				appender.append(line)
		}
		//String expected = "\nRun tests for: iPad/" + XcodePlugin.SDK_IPHONESIMULATOR + "/iOS\n\n  FAILED -[DTActionPanelTest_iPhone testActionPanelSizeDidChangeDelegate] - (0.026 seconds)\n"
		//assert output.toString().equals(expected) : "Expected '" + expected + "' but was: " + output.toString()
		String outputString = output.toString()

		assertThat(outputString, containsString("Run tests for: iPad 2/iOS Simulator"))
		assertThat(outputString, containsString("FAILED -[DTActionPanelTest_iPhone testActionPanelSizeDidChangeDelegate] - (0.026 seconds)"))
		assertThat(outputString, containsString("/Users/dummy/poject/UnitTests/iPhone/DTPopoverController/DTActionPanelTest_iPhone.m:85: error: -[DTActionPanelTest_iPhone testActionPanelSizeDidChangeDelegate] : Expected 2 matching invocations, but received 0\n"))
	}



	@Test
	void testFinishedFailed() {
		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))

		StyledTextOutputStub output = new StyledTextOutputStub()

		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, destinations)

		for (String line : simctlOutput.split("\n")) {
			appender.append(line);
		}

		assertThat(output.toString(), endsWith("1 tests completed, 1 failed\n"))

	}


	@Test
	void testComplexOutput() {
		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-complex-test.txt"))
		StyledTextOutputStub output = new StyledTextOutputStub()
		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, destinations)
		for (String line : simctlOutput.split("\n")) {
			appender.append(line);
		}
		assertThat(output.toString(), containsString("Run tests for: iPad 2/iOS Simulator"))
		assertThat(output.toString(), containsString("Run tests for: iPhone 4s/iOS Simulator"))
	}


	@Test
	void testFailureOutput() {
		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-test-compile-error.txt"))
		StyledTextOutputStub output = new StyledTextOutputStub()
		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, destinations)
		for (String line : simctlOutput.split("\n")) {
			appender.append(line);
		}

		assertThat(output.toString(), startsWith("Testing failed:"))
		assertThat(output.toString(), containsString("No visible @interface for 'DTPanelView' declares the selector 'moveToNewPositionWithDuration:bounce:completion:'"))
		assertThat(output.toString(), endsWith("\n0 tests completed\n"))

	}


	@Test
	void testNoFailureOutput() {
		String lines = "CompileC /Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/Objects-normal/i386/ExampleTests.o ExampleTests/ExampleTests.m normal i386 objective-c com.apple.compilers.llvm.clang.1_0.compiler\n" +
						"    cd /Users/example/workspace/openbakery/xcodePlugin/gradle-xcodePlugin/example/Example\n" +
						"    export LANG=en_US.US-ASCII\n" +
						"    export PATH=\"/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin:/Applications/Xcode.app/Contents/Developer/usr/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/opt/X11/bin:/usr/local/git/bin:/usr/local/MacGPG2/bin:/usr/local/git/bin:/Users/example/Java/gradle/bin:/Applications/Xcode.app/Contents/Developer/usr/bin/:/Users/example/.rvm/bin\"\n" +
						"    /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang -x objective-c -arch i386 -fmessage-length=0 -fdiagnostics-show-note-include-stack -fmacro-backtrace-limit=0 -std=gnu99 -fobjc-arc -fmodules -fmodules-cache-path=/Users/example/Library/Developer/Xcode/DerivedData/ModuleCache -fmodules-prune-interval=86400 -fmodules-prune-after=345600 -Wnon-modular-include-in-framework-module -Werror=non-modular-include-in-framework-module -Wno-trigraphs -fpascal-strings -O0 -Wno-missing-field-initializers -Wno-missing-prototypes -Werror=return-type -Wno-implicit-atomic-properties -Werror=deprecated-objc-isa-usage -Werror=objc-root-class -Wno-receiver-is-weak -Wno-arc-repeated-use-of-weak -Wduplicate-method-match -Wno-missing-braces -Wparentheses -Wswitch -Wunused-function -Wno-unused-label -Wno-unused-parameter -Wunused-variable -Wunused-value -Wempty-body -Wuninitialized -Wno-unknown-pragmas -Wno-shadow -Wno-four-char-constants -Wno-conversion -Wconstant-conversion -Wint-conversion -Wbool-conversion -Wenum-conversion -Wshorten-64-to-32 -Wpointer-sign -Wno-newline-eof -Wno-selector -Wno-strict-selector-match -Wundeclared-selector -Wno-deprecated-implementations -DDEBUG=1 -DDEBUG=1 -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator8.0.sdk -fexceptions -fasm-blocks -fstrict-aliasing -Wprotocol -Wdeprecated-declarations -g -Wno-sign-conversion -fobjc-abi-version=2 -fobjc-legacy-dispatch -mios-simulator-version-min=7.0 -iquote /Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/ExampleTests-generated-files.hmap -I/Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/ExampleTests-own-target-headers.hmap -I/Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/ExampleTests-all-target-headers.hmap -iquote /Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/ExampleTests-project-headers.hmap -I/Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Products/Debug-iphonesimulator/include -I/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/include -I/Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/DerivedSources/i386 -I/Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/DerivedSources -F/Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Products/Debug-iphonesimulator -F/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator8.0.sdk/Developer/Library/Frameworks -F/Applications/Xcode.app/Contents/Developer/Library/Frameworks -F/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/Library/Frameworks -F/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator8.0.sdk/Developer/Library/Frameworks -include /Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/PrecompiledHeaders/Example-Prefix-acdqhvibqngspqcpkwqtdjprohex/Example-Prefix.pch -MMD -MT dependencies -MF /Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/Objects-normal/i386/ExampleTests.d --serialize-diagnostics /Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/Objects-normal/i386/ExampleTests.dia -c /Users/example/workspace/openbakery/xcodePlugin/gradle-xcodePlugin/example/Example/ExampleTests/ExampleTests.m -o /Users/example/Library/Developer/Xcode/DerivedData/Example-gzxkuommzsgsxicqlcnrrqgobshp/Build/Intermediates/Example.build/Debug-iphonesimulator/ExampleTests.build/Objects-normal/i386/ExampleTests.o\n" +
						"\n" +
						"\tProperty 'moveToNewPositionWithDuration' not found on object of type 'DTPanelNavigationController *'\n" +
						"\tNo visible @interface for 'DTPanelView' declares the selector 'moveToNewPositionWithDuration:bounce:completion:'\n" +
						"** TEST FAILED **"

		StyledTextOutputStub output = new StyledTextOutputStub()
		TestBuildOutputAppender appender = new TestBuildOutputAppender(output, destinations)
		for (String line : lines.split("\n")) {
			appender.append(line);
		}

		assertThat(output.toString(), is("0 tests completed\n"))

	}



	@Test
	void testCompile_startingTests() {
		String xcodebuildOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-createbinary.txt"))
		StyledTextOutputStub output = new StyledTextOutputStub()
		ProgressLoggerStub progress = new ProgressLoggerStub()
		TestBuildOutputAppender appender =  new TestBuildOutputAppender(progress, output, destinations)
		for (String line : xcodebuildOutput.split("\n")) {
			appender.append(line);
		}
		assertThat(progress.progress, hasItem("Starting Tests"))
	}



	@Test
	void testCompile_progress() {
		String xcodebuildOutput = FileUtils.readFileToString(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))
		StyledTextOutputStub output = new StyledTextOutputStub()
		ProgressLoggerStub progress = new ProgressLoggerStub()
		TestBuildOutputAppender appender =  new TestBuildOutputAppender(progress, output, destinations)
		for (String line : xcodebuildOutput.split("\n")) {
			appender.append(line);
		}
		assertThat(progress.progress, hasItem("Compile Example/AppDelegate.m"))
	}




}
