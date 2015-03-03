package org.openbakery

import org.gradle.api.tasks.TaskAction

/**
 * Created by chaitanyar on 3/2/15.
 */
class EntitlementsModifyTask extends AbstractDistributeTask  {
    public EntitlementsModifyTask() {
        dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
    }
    @TaskAction
    void prepare() {
        def entitlementsFile = new File(project.projectDir, project.xcodebuild.entitlementsPath)

        logger.lifecycle("Updating {}", entitlementsFile)
        if (project.xcodebuild.entitlementsConfig) {
            project.xcodebuild.entitlementsConfig.each { key,value ->
                setValueForPlist(entitlementsFile,key,value)
            }
        }

        if (project.xcodebuild.hasAppExtensions()) {
            project.xcodebuild.appExtensions.each {appExtension->
                if (appExtension.entitlementsPath) {
                    def appExtensionEntitlementsFile = new File(appExtension.entitlementsPath)
                    logger.lifecycle("Updating {}", appExtensionEntitlementsFile )
                    appExtension.entitlementsConfig.each { key,value ->
                        setValueForPlist(appExtensionEntitlementsFile ,key,value)
                    }
                }
            }
        }
    }
}