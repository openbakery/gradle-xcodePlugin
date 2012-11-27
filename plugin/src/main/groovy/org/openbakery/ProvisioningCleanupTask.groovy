package org.openbakery

import org.gradle.api.tasks.TaskAction

class ProvisioningCleanupTask extends AbstractXcodeTask {
    ProvisioningProfileIdReader provisioningProfileIdReader

    ProvisioningCleanupTask() {
        provisioningProfileIdReader = new ProvisioningProfileIdReader()
    }

    @TaskAction
    def clean() {
        new File(project.provisioning.destinationRoot).deleteDir()

        def uuid = provisioningProfileIdReader.readProvisioningProfileIdFromDestinationRoot(project.provisioning.destinationRoot)
        if (uuid != null) {
            File mobileprovisionPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/" + uuid + ".mobileprovision")
            if (mobileprovisionPath.exists()) {
                println "Deleting " + mobileprovisionPath
                mobileprovisionPath.delete()
            }
        }
    }
}