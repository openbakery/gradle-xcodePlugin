package org.openbakery.signing

enum SigningMethod {
	AppStore("app-store"),
	AdHoc("ad-hoc"),
	Entreprise("enterprise"),
	Dev("development")

	private final String value

	SigningMethod(String value) {
		this.value = value
	}

	String getValue() {
		return value
	}

	public static Optional<SigningMethod> fromString(value) {
		return Optional.ofNullable(values().find { it.getValue() == value })
	}
}
