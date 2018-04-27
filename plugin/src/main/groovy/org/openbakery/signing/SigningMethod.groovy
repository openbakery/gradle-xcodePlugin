package org.openbakery.signing

enum SigningMethod {
	AppStore("app-store"),
	AdHoc("ad-hoc"),
	Package("package"),
	Entreprise("enterprise"),
	Developement("development"),
	DeveloperId("developer-id"),
	MacApplication("mac-application")

	private final String value

	SigningMethod(String value) {
		this.value = value
	}

	String getValue() {
		return value
	}
}
