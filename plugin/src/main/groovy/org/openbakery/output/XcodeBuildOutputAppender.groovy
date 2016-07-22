package org.openbakery.output

import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.text.StyledTextOutput

import java.util.regex.Matcher

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 02.10.13
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class XcodeBuildOutputAppender implements OutputAppender {


	String command = null
	String currentSourceFile
	StringBuilder outputText = new StringBuilder()
	boolean hasOutput = false
	boolean warning = false
	boolean error = false
	boolean fullProgress = false;
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
		outputText = new StringBuilder()
		hasOutput = false
		error = false
		warning = false
		command = null
	}

	@Override
	void append(String line) {

		if (hasOutput) {
			outputText.append("\n")
			outputText.append(line)
		}

		if (line.startsWith("CompileC")) {

			int sourceFileStartIndex = line.indexOf(".o ")+3
			int sourceFileEndIndex = Integer.MAX_VALUE
			if (line.indexOf(".mm ") >= 0 ) {
				sourceFileEndIndex = line.indexOf(".mm ")+3
			} else if (line.indexOf(".cc ") >= 0) {
				sourceFileEndIndex = line.indexOf(".cc ")+3
			} else if (line.indexOf(".c ") >= 0) {
				sourceFileEndIndex = line.indexOf(".c ")+2
			} else if (line.indexOf(".m ") >= 0) {
				sourceFileEndIndex = line.indexOf(".m ")+2
			}

			if (sourceFileStartIndex < sourceFileEndIndex && sourceFileEndIndex < line.length()) {
				currentSourceFile = line.substring(sourceFileStartIndex, sourceFileEndIndex)
				command = "Compile"


			}
		} else if (line.startsWith("CompileStoryboard") || line.startsWith("CompileXIB")) {
			int sourceFileStartIndex = line.indexOf(" ")+1
			if (sourceFileStartIndex < line.length()) {
				currentSourceFile = line.substring(sourceFileStartIndex, line.length())
				command = "Compile"
			}
		} else if (line.startsWith("Ld")) {
			command = "Linking"
			String[] tokens = line.split(" ");
			if (tokens.length > 1) {
				currentSourceFile = tokens[1];
			}
		} else if (line.startsWith("CreateUniversalBinary")) {
			progressLogger.progress("Create Binary")
		} else if (line.startsWith("Code Sign error:")) {

			command = "CodeSign"
			error = true
			hasOutput = true
			outputText.append("\n")
			outputText.append(line)
		} else if ((currentSourceFile != null || command != null) && line.equals("")) {
			printOutput()
			reset()
		} else if (line.endsWith("errors generated.") || line.endsWith("error generated.") || line.startsWith("clang: error:")) {
			error = true
		} else if (line.endsWith("warnings generated.") || line.endsWith("warning generated.")) {
			warning = true
		} else if (!hasOutput && currentSourceFile != null && line.contains(currentSourceFile) && !line.startsWith(" ")) {
			hasOutput = true
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
			output.text(currentSourceFile);
		}
		output.println();
		if (hasOutput) {
			output.println(outputText.toString())
		}
	}
}
