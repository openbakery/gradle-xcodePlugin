package org.openbakery.xcode

enum Extension {
    // https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/AppExtensionKeys.html#//apple_ref/doc/uid/TP40014212-SW15
    watch("com.apple.watchkit"),
    today("com.apple.widget-extension"),
    share("com.apple.share-services"),
    keyboard("com.apple.keyboard-service"),
    sticker("com.apple.message-payload-provider")

    String identifier

    Extension(String identifier) {
        this.identifier = identifier
    }

    static Extension extensionFromIdentifier(String identifier) {
        return values().find { it.identifier == identifier }
    }
}
