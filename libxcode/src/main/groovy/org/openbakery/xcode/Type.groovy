package org.openbakery.xcode

enum Type {
    iOS("iOS"),
    macOS("macOS"),
    tvOS("tvOS"),
    watchOS("watchOS")

    private final String value

    Type(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }

    static Type typeFromString(final String string) {
        Type result = values()
                .findAll { string?.toLowerCase()?.startsWith(it.value.toLowerCase()) }
                .find()

        result = result ?:
                (string?.toLowerCase()?.startsWith("osx") ? macOS : null)

        return result
    }
}
