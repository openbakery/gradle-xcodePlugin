package org.openbakery.output;


import org.gradle.internal.logging.text.StyledTextOutput;

public class ConsoleOutputAppender implements OutputAppender {

	private StyledTextOutput output;

	public ConsoleOutputAppender(StyledTextOutput output) {

		this.output = output;
	}

	@Override
	public void append(String line) {
		output.withStyle(StyledTextOutput.Style.Info).println(line);

	}

}
