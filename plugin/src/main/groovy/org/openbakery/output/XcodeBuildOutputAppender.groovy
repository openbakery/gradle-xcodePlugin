package org.openbakery.output

import org.gradle.logging.StyledTextOutput
import org.gradle.logging.StyledTextOutputFactory

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 02.10.13
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class XcodeBuildOutputAppender implements OutputAppender {

	boolean command = false
	String currentSourceFile
	StringBuilder errorText = new StringBuilder()
	boolean error = false

	StyledTextOutput output

	XcodeBuildOutputAppender(StyledTextOutput output) {
		this.output = output;
		reset()
	}

	void reset() {
		currentSourceFile = null
		errorText = new StringBuilder()
		error = false;
	}

	@Override
	void append(String line) {

		if (error) {
			errorText.append("\n")
			errorText.append(line)
		}

		if (line.startsWith("CompileC")) {
			int sourceFileStartIndex = line.indexOf(".o")+3;
			int sourceFileEndIndex = line.indexOf(".m")+2;
			if (sourceFileEndIndex < line.length()) {
				currentSourceFile = line.substring(sourceFileStartIndex, sourceFileEndIndex)
				output.text("Compile: " + currentSourceFile);
				command = true
			}
		} else if (line.startsWith("CompileStoryboard") || line.startsWith("CompileXIB")) {
			int sourceFileStartIndex = line.indexOf(" ")+1;
			if (sourceFileStartIndex < line.length()) {
				currentSourceFile = line.substring(sourceFileStartIndex, line.length())
				output.text("Compile: " + currentSourceFile);
				command = true
			}
		}	else if (currentSourceFile != null && !line.startsWith(" ") && line.contains(currentSourceFile)) {
			error = true
		} else if (currentSourceFile != null && line.equals("")) {
			// end of command
			output.text(" - ")
			if (error) {
				output.withStyle(StyledTextOutput.Style.Failure).text("ERROR")
			} else {
				output.withStyle(StyledTextOutput.Style.Identifier).text("OK")
			}
			output.println();
			if (error) {
				output.println(errorText.toString())
			}
			reset()
		}
	}
}
