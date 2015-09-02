package org.openbakery

import org.gradle.api.tasks.TaskAction

/**
 * Created by chaitanyar on 3/2/15.
 */
class EntitlementsModifyTask extends AbstractDistributeTask  {
	public EntitlementsModifyTask() {
	}

	void executeTask() {
		if (project.xcodebuild.entitlementsPath) {
			def entitlementsFile = new File(project.projectDir, project.xcodebuild.entitlementsPath)

			logger.lifecycle("Updating {}", entitlementsFile)
			if (project.xcodebuild.entitlementsConfig) {
				project.xcodebuild.entitlementsConfig.each { String key, String value ->
					plistHelper.setValueForPlist(entitlementsFile, key, value)
				}
			}

			if (project.xcodebuild.hasAppExtensions()) {
				project.xcodebuild.appExtensions.each { appExtension ->
					if (appExtension.entitlementsPath) {
						def appExtensionEntitlementsFile = new File(appExtension.entitlementsPath)
						logger.lifecycle("Updating {}", appExtensionEntitlementsFile)
						appExtension.entitlementsConfig.each { String key, String value ->
							plistHelper.setValueForPlist(appExtensionEntitlementsFile, key, value)
						}
					}
				}
			}
		}
	}
}
