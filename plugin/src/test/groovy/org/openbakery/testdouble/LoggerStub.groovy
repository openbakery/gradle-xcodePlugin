package org.openbakery.testdouble

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.slf4j.Marker

/**
 * Created by rene on 30.06.15.
 */
class LoggerStub implements Logger {

	def logEntries = [:]

	@Override
	boolean isLifecycleEnabled() {
		return false
	}

	@Override
	String getName() {
		return null
	}

	@Override
	boolean isTraceEnabled() {
		return false
	}

	@Override
	void trace(String s) {

	}

	@Override
	void trace(String s, Object o) {

	}

	@Override
	void trace(String s, Object o, Object o1) {

	}

	@Override
	void trace(String s, Object... objects) {

	}

	@Override
	void trace(String s, Throwable throwable) {

	}

	@Override
	boolean isTraceEnabled(Marker marker) {
		return false
	}

	@Override
	void trace(Marker marker, String s) {

	}

	@Override
	void trace(Marker marker, String s, Object o) {

	}

	@Override
	void trace(Marker marker, String s, Object o, Object o1) {

	}

	@Override
	void trace(Marker marker, String s, Object... objects) {

	}

	@Override
	void trace(Marker marker, String s, Throwable throwable) {

	}

	@Override
	boolean isDebugEnabled() {
		return false
	}

	@Override
	void debug(String s) {

	}

	@Override
	void debug(String s, Object o) {

	}

	@Override
	void debug(String s, Object o, Object o1) {

	}

	@Override
	void debug(String s, Object... objects) {

	}

	@Override
	void debug(String s, Throwable throwable) {

	}

	@Override
	boolean isDebugEnabled(Marker marker) {
		return false
	}

	@Override
	void debug(Marker marker, String s) {

	}

	@Override
	void debug(Marker marker, String s, Object o) {

	}

	@Override
	void debug(Marker marker, String s, Object o, Object o1) {

	}

	@Override
	void debug(Marker marker, String s, Object... objects) {

	}

	@Override
	void debug(Marker marker, String s, Throwable throwable) {

	}

	@Override
	boolean isInfoEnabled() {
		return false
	}

	@Override
	void info(String s) {

	}

	@Override
	void info(String s, Object o) {

	}

	@Override
	void info(String s, Object o, Object o1) {

	}

	@Override
	void lifecycle(String s) {
		def entries = logEntries["LIFECYCLE"]
		if (entries == null) {
			entries = []
		}
		entries << s
		logEntries["LIFECYCLE"] = entries
	}

	@Override
	void lifecycle(String s, Object... objects) {

	}

	@Override
	void lifecycle(String s, Throwable throwable) {

	}

	@Override
	boolean isQuietEnabled() {
		return false
	}

	@Override
	void quiet(String s) {

	}

	@Override
	void quiet(String s, Object... objects) {

	}

	@Override
	void info(String s, Object... objects) {

	}

	@Override
	void info(String s, Throwable throwable) {

	}

	@Override
	boolean isInfoEnabled(Marker marker) {
		return false
	}

	@Override
	void info(Marker marker, String s) {

	}

	@Override
	void info(Marker marker, String s, Object o) {

	}

	@Override
	void info(Marker marker, String s, Object o, Object o1) {

	}

	@Override
	void info(Marker marker, String s, Object... objects) {

	}

	@Override
	void info(Marker marker, String s, Throwable throwable) {

	}

	@Override
	boolean isWarnEnabled() {
		return false
	}

	@Override
	void warn(String s) {

	}

	@Override
	void warn(String s, Object o) {

	}

	@Override
	void warn(String s, Object... objects) {

	}

	@Override
	void warn(String s, Object o, Object o1) {

	}

	@Override
	void warn(String s, Throwable throwable) {

	}

	@Override
	boolean isWarnEnabled(Marker marker) {
		return false
	}

	@Override
	void warn(Marker marker, String s) {

	}

	@Override
	void warn(Marker marker, String s, Object o) {

	}

	@Override
	void warn(Marker marker, String s, Object o, Object o1) {

	}

	@Override
	void warn(Marker marker, String s, Object... objects) {

	}

	@Override
	void warn(Marker marker, String s, Throwable throwable) {

	}

	@Override
	boolean isErrorEnabled() {
		return false
	}

	@Override
	void error(String s) {

	}

	@Override
	void error(String s, Object o) {

	}

	@Override
	void error(String s, Object o, Object o1) {

	}

	@Override
	void error(String s, Object... objects) {

	}

	@Override
	void error(String s, Throwable throwable) {

	}

	@Override
	boolean isErrorEnabled(Marker marker) {
		return false
	}

	@Override
	void error(Marker marker, String s) {

	}

	@Override
	void error(Marker marker, String s, Object o) {

	}

	@Override
	void error(Marker marker, String s, Object o, Object o1) {

	}

	@Override
	void error(Marker marker, String s, Object... objects) {

	}

	@Override
	void error(Marker marker, String s, Throwable throwable) {

	}

	@Override
	void quiet(String s, Throwable throwable) {

	}

	@Override
	boolean isEnabled(LogLevel logLevel) {
		return false
	}

	@Override
	void log(LogLevel logLevel, String s) {

	}

	@Override
	void log(LogLevel logLevel, String s, Object... objects) {

	}

	@Override
	void log(LogLevel logLevel, String s, Throwable throwable) {

	}
}
