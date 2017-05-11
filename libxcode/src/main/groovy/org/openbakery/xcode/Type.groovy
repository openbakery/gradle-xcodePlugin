package org.openbakery.xcode

enum Type {
	iOS("iOS"),
	macOS("macOS"),
	tvOS("tvOS"),
	watchOS("watchOS")


	String value;

	Type(String value) {
		this.value = value
	}

	static Type typeFromString(String string) {
		if (string == null) {
			return iOS
		}
		for (Type type in Type.values()) {
			// for backward compatibility
			if (string.toLowerCase().equalsIgnoreCase("osx")) {
				return Type.macOS
			}
			if (string.toLowerCase().startsWith(type.value.toLowerCase())) {
				return type
			}
		}
		return iOS
	}
}
