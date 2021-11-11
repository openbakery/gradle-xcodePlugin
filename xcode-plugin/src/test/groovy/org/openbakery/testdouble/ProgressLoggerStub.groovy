package org.openbakery.testdouble

import org.gradle.internal.logging.progress.ProgressLogger

import javax.annotation.Nullable


class ProgressLoggerStub implements ProgressLogger {

	def progress = []


	@Override
	String getDescription() {
		return null
	}

	@Override
	ProgressLogger setDescription(String s) {
		return null
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
	void progress(@Nullable String status) {
		progress << status
	}

	@Override
	void progress(@Nullable String status, boolean b) {
		progress << status
	}

	@Override
	void completed() {
	}

	@Override
	void completed(String s, boolean b) {
	}
}
