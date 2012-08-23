package org.openbakery

import org.gradle.api.tasks.TaskAction

class KeychainCleanupTask extends AbstractXcodeTask {

    KeychainCleanupTask() {
        super();
        this.description = "Cleanup the keychain"
    }

    @TaskAction
    def cleanup() {
        println "Delete Keychain '" + project.keychain.keychainName + "'";
        def keychainPath = System.getProperty("user.home") + "/Library/Keychains/" + project.keychain.keychainName;
        if (new File(keychainPath).exists()) {
            runCommand(["security", "delete-keychain", keychainPath]);
        }
    }

}