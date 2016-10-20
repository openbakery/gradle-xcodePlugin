package org.openbakery

public enum Type {
	iOS("iOS"),
	OSX("OSX"),
	tvOS("tvOS"),
	watchOS("watchOS")


	String value;

	public Type(String value) {
		this.value = value;
	}

	public static Type typeFromString(String string) {
		if (string == null) {
			return iOS;
		}
		for (Type type in Type.values()) {
			if (string.toLowerCase().startsWith(type.value.toLowerCase())) {
				return type;
			}
		}
		return iOS;
	}
}
