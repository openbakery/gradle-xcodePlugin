package org.openbakery.xcode

enum Type {
    iOS("iOS"),
    macOS("macOS"),
    tvOS("tvOS"),
    watchOS("watchOS")

    private final String value

    private static final String BACKWARD_COMPATIBILITY_MAC_OS = "osx"

    Type(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }

    static Type typeFromString(final String string) {
        return values()
                .findAll { string?.toLowerCase()?.startsWith(it.value.toLowerCase()) }
                .find() ?: (string?.toLowerCase()?.startsWith(BACKWARD_COMPATIBILITY_MAC_OS) ? macOS : null)
    }
}
