package org.openbakery.stubs

import org.gradle.internal.progress.OperationIdentifier
import org.gradle.logging.ProgressLogger

/**
 * Created by rene on 30.06.15.
 */
class ProgressLoggerStub implements ProgressLogger {

	def progress = []

	@Override
	String getDescription() {
		return null
	}

	@Override
	void setDescription(String s) {
	}

	@Override
	String getShortDescription() {
		return null
	}

	@Override
	void setShortDescription(String s) {
	}

	@Override
	String getLoggingHeader() {
		return null
	}

	@Override
	void setLoggingHeader(String s) {
	}

	@Override
	ProgressLogger start(String s, String s1) {
		return null
	}

	@Override
	void started() {

	}

	@Override
	void started(String s) {

	}

	@Override
	void progress(String status) {
		progress << status
	}

	@Override
	void completed() {

	}

	@Override
	void completed(String s) {

	}

	@Override
	OperationIdentifier currentOperationId() {
		return null
	}
}
