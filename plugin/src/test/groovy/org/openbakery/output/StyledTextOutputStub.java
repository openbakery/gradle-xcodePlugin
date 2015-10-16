package org.openbakery.output;

import org.gradle.logging.StyledTextOutput;

import java.text.MessageFormat;

public class StyledTextOutputStub implements StyledTextOutput {
	StringBuilder builder = new StringBuilder();

	@Override
	public StyledTextOutput append(char c) {
		builder.append(String.valueOf(c));
		return this;
	}

	@Override
	public StyledTextOutput append(CharSequence charSequence) {
		builder.append(charSequence);
		return this;
	}

	@Override
	public StyledTextOutput append(CharSequence charSequence, int i, int i1) {
		return this;
	}

	@Override
	public StyledTextOutput style(StyledTextOutput.Style style) {
		return this;
	}

	@Override
	public StyledTextOutput withStyle(StyledTextOutput.Style style) {
		return this;
	}

	@Override
	public StyledTextOutput text(Object o) {
		builder.append(o);
		return this;
	}

	@Override
	public StyledTextOutput println(Object o) {
		builder.append(o);
		builder.append("\n");
		return this;
	}

	@Override
	public StyledTextOutput format(String s, Object... objects) {
		builder.append(MessageFormat.format(s, objects));
		return this;
	}

	@Override
	public StyledTextOutput formatln(String s, Object... objects) {
		builder.append(MessageFormat.format(s, objects));
		builder.append("\n");
		return this;
	}

	@Override
	public StyledTextOutput println() {
		builder.append("\n");
		return this;
	}

	@Override
	public StyledTextOutput exception(Throwable throwable) {
		return this;
	}

	@Override
	public String toString() {
		return builder.toString();
	}


}
