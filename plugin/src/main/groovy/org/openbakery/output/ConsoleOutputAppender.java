package org.openbakery.output;

import org.gradle.api.plugins.jetty.AbstractJettyRunTask;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.StyledTextOutput;

/**
 * Created by rene on 06.08.14.
 */
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
