package org.openbakery.signing

import groovy.transform.CompileStatic

@CompileStatic
enum SigningMethod {
	AppStore("app-store"),
	AdHoc("ad-hoc"),
	Enterprise("enterprise"),
	Dev("development")

	private final String value

	SigningMethod(String value) {
		this.value = value
	}

	String getValue() {
		return value
	}

	static Optional<SigningMethod> fromString(value) {
		return Optional.ofNullable(values().find { it.getValue() == value })
	}
}
