package org.openbakery.output

import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.text.StyledTextOutput

import java.util.regex.Matcher

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 02.10.13
 * Time: 11:08
 */
class XcodeBuildOutputAppender implements OutputAppender {

	enum OutputState {
	  OK, ERROR, WARNING
	}

	String command = null
	String currentSourceFile
	boolean warning = false
	boolean error = false
	boolean fullProgress = false
	OutputState outputState = OutputState.OK
	ProgressLogger progressLogger

	StyledTextOutput output

	XcodeBuildOutputAppender(ProgressLogger progressLogger, StyledTextOutput output) {
		this.output = output
		this.progressLogger = progressLogger
		reset()
	}

	XcodeBuildOutputAppender(StyledTextOutput output) {
		this(null, output);
		fullProgress = true
	}

	void reset() {
		currentSourceFile = null
		error = false
		warning = false
		command = null
	}


	boolean checkCompileC(String line) {
		if (line.startsWith("CompileC")) {
			int sourceFileStartIndex = line.indexOf(".o ") + 3
			int sourceFileEndIndex = Integer.MAX_VALUE
			if (line.indexOf(".mm ") >= 0) {
				sourceFileEndIndex = line.indexOf(".mm ") + 3
			} else if (line.indexOf(".cc ") >= 0) {
				sourceFileEndIndex = line.indexOf(".cc ") + 3
			} else if (line.indexOf(".c ") >= 0) {
				sourceFileEndIndex = line.indexOf(".c ") + 2
			} else if (line.indexOf(".m ") >= 0) {
				sourceFileEndIndex = line.indexOf(".m ") + 2
			}

			if (sourceFileStartIndex < sourceFileEndIndex && sourceFileEndIndex < line.length()) {
				currentSourceFile = line.substring(sourceFileStartIndex, sourceFileEndIndex)
				setCommand("Compile")
			}
			return true
		}
		return false
	}

	boolean checkCompileSwift(String line) {
		if (line.startsWith("CompileSwift")) {
			int sourceFileStartIndex = line.lastIndexOf(" ") + 1
			int sourceFileEndIndex = line.indexOf(".swift") + 6

			if (sourceFileStartIndex < sourceFileEndIndex && sourceFileEndIndex <= line.length()) {
				currentSourceFile = line.substring(sourceFileStartIndex, sourceFileEndIndex)
				setCommand("Compile")
			}
			return true
		}

		// /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/swift

		if (line.contains(".xctoolchain/usr/bin/swift ")) {
			currentSourceFile = "<multiple files>"
			setCommand("Compile")
			return true
		}


		return false
	}

	boolean checkCompileXib(String line) {
		if (line.startsWith("CompileStoryboard") || line.startsWith("CompileXIB")) {
			int sourceFileStartIndex = line.indexOf(" ") + 1
			if (sourceFileStartIndex < line.length()) {
				currentSourceFile = line.substring(sourceFileStartIndex, line.length())
				setCommand("Compile")
			}
			return true
		}
		return false
	}

	boolean checkCompileLd(String line) {
		if (line.startsWith("Ld")) {
			setCommand("Linking")
			String[] tokens = line.split(" ");
			if (tokens.length > 1) {
				currentSourceFile = tokens[1]
			}
			return true
		}
		return false
	}

	boolean checkCreateBinary(String line) {
		if (line.startsWith("CreateUniversalBinary")) {
				progressLogger.progress("Create Binary")
			}
		return false
	}

	boolean checkCodesign(String line) {
		if (line.startsWith("CodeSign")) {
			int sourceFileStartIndex = line.indexOf(" ") + 1
			int sourceFileEndIndex = line.indexOf(" (in target:")

			if (sourceFileStartIndex < sourceFileEndIndex && sourceFileEndIndex <= line.length()) {
				currentSourceFile = line.substring(sourceFileStartIndex, sourceFileEndIndex)
				setCommand("CodeSign")
			}
			return true
		}
		return false
	}


	boolean checkCodesignError(String line) {
		if (line.startsWith("Code Sign error:")) {
			setCommand("CodeSign")
			error = true
			outputState = OutputState.ERROR
			printOutput()
			return true
		}
		return false
	}

	boolean checkError(String line) {
		if (hasToken(line, "error:")) {
			outputState = OutputState.ERROR
			error = true
			printOutput()
			return true
		}
		// matches '/Users/me/Project/Test/Test.swift:16:10: error: use'
		if (line ==~ /.+:\d+:\d+:\serror:\s.*/) {
			outputState = OutputState.ERROR
			return true
		}

		if (line.endsWith("errors generated.") || line.endsWith("error generated.") || line.startsWith("clang: error:")) {
			error = true
			return true
		}
		return false
	}

	boolean checkWarning(String line) {
		if (hasToken(line, "warning:")) {
			outputState = OutputState.WARNING
			warning = true
			printOutput()
			return true
		}
		// matches '/Users/me/Project/Test/Test.swift:16:10: warning: use'
		if (line ==~ /.+:\d+:\d+:\swarning:\s.*/) {
			outputState = OutputState.WARNING
			return true
		}

		if (line.endsWith("warnings generated.") || line.endsWith("warning generated.")) {
			warning = true
			return true
		}
		return false
	}

	boolean checkLine(String line) {
		if (checkCompileC(line)) {
			return true
		}
		if (checkCompileSwift(line)) {
			return true
		}
		if (checkCompileXib(line)) {
			return true
		}
		if (checkCompileLd(line)) {
			return true
		}
		if (checkCreateBinary(line)) {
			return true
		}
		if (checkCodesign(line)) {
			return true
		}
		if (checkCodesignError(line)) {
			return true
		}
		if (checkError(line)) {
			return true
		}
		if (checkWarning(line)) {
			return true
		}
		return false
	}

	@Override
	void append(String line) {
		checkLine(line)

	 if ((currentSourceFile != null || command != null) && line.equals("")) {
			printOutput()
			reset()
		}

 		if (isFinished(line)) {
			outputState = OutputState.OK
		}

		switch (outputState) {
			case OutputState.ERROR:
				output.withStyle(StyledTextOutput.Style.Failure).text(line)
				output.withStyle(StyledTextOutput.Style.Failure).text("\n")
				break
			case OutputState.WARNING:
				output.withStyle(StyledTextOutput.Style.Identifier).text(line)
				output.withStyle(StyledTextOutput.Style.Identifier).text("\n")
				break
			default:
				break
		}

		if (progressLogger != null && command != null && currentSourceFile != null) {
			progressLogger.progress(command + " " +  currentSourceFile)
		}
	}



	private void printOutput() {
	// end of command
		if (error) {
			output.withStyle(StyledTextOutput.Style.Failure).text("   ERROR")
		} else if (warning) {
			output.withStyle(StyledTextOutput.Style.Identifier).text("WARNINGS")
		} else {
			if (!fullProgress) {
				return;
			}
			output.withStyle(StyledTextOutput.Style.Identifier).text("      OK")
		}
		output.text(" - ");
		output.text(command)
		if (currentSourceFile != null) {
			output.text(": ")
			output.text(currentSourceFile)
		}
		output.println()
	}


	boolean isFinished(String line) {
		if (line == "") {
			return true
		}
		if (line.startsWith("**")) {

			if (line.endsWith("BUILD SUCCEEDED **")) {
				return true
			}

			if (line.endsWith("BUILD FAILED **")) {
				return true
			}

		}
		return false
	}


	boolean hasToken(String line, String token) {
		if (line != null && currentSourceFile) {
			String[] tokens = line.split(" ")
			if (tokens.length > 1) {
				if (tokens[0].contains(currentSourceFile) && tokens[1].contains(token)) {
					return true
				}
			}
		}
		return false
	}

	void setCommand(String command) {
		this.command = command
		outputState = OutputState.OK
	}
}
