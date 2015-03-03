/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery

import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.TaskAction


class InfoPlistModifyTask extends AbstractDistributeTask {

    public InfoPlistModifyTask() {
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
	}

    @TaskAction
	def prepare() {

        def infoPlist = new File(project.projectDir, project.xcodebuild.infoPlist)
        logger.lifecycle("Updating {}", infoPlist)

		if (project.infoplist.bundleIdentifier != null) {
			setValueForPlist(infoPlist, "CFBundleIdentifier", project.infoplist.bundleIdentifier)
		}

		// add suffix to bundleIdentifier
		if (project.infoplist.bundleIdentifierSuffix != null) {
			def bundleIdentifier = getValueFromPlist(infoPlist, "CFBundleIdentifier")

			setValueForPlist(infoPlist, "CFBundleIdentifier", bundleIdentifier + project.infoplist.bundleIdentifierSuffix)

		}

		// Modify bundle bundleDisplayName
		if (project.infoplist.bundleDisplayName != null) {
			setValueForPlist(infoPlist, "CFBundleDisplayName", project.infoplist.bundleDisplayName)
		}

		// add suffix to bundleDisplayName
		if (project.infoplist.bundleDisplayNameSuffix != null) {
			def bundleDisplayName = getValueFromPlist(infoPlist, "CFBundleDisplayName")
			setValueForPlist(infoPlist, "CFBundleDisplayName", bundleDisplayName + project.infoplist.bundleDisplayNameSuffix)

		}

		logger.debug("project.infoplist.version: {}", project.infoplist.version)

		modifyVersion(infoPlist)
		modifyShortVersion(infoPlist)


		for(String command in project.infoplist.commands) {
			setValueForPlist(infoPlist, command)
		}

        if (project.xcodebuild.infoPlistConfig) {
            project.xcodebuild.infoPlistConfig.each { key,value ->
                setValueForPlist(infoPlist,key,value)
            }
        }

        if (project.xcodebuild.hasAppExtensions()) {
            project.xcodebuild.appExtensions.each {appExtension->
                if (appExtension.infoPlistPath) {
                    def appExtensionInfoPlistFile = new File(appExtension.infoPlistPath)
                    logger.lifecycle("Updating {}", appExtensionInfoPlistFile)
                    appExtension.infoPlistConfig.each { key,value ->
                        setValueForPlist(appExtensionInfoPlistFile,key,value)
                    }
                }
            }
        }


    }

	private void modifyVersion(File infoPlist) {
		if (project.infoplist.version == null && project.infoplist.versionSuffix == null && project.infoplist.versionPrefix == null) {
			return
		}

        def version;
		if (project.infoplist.version != null) {
			version = project.infoplist.version
		} else {
			version = getValueFromPlist(infoPlist, "CFBundleVersion")
		}

		if (project.infoplist.versionSuffix) {
			version = version + project.infoplist.versionSuffix
		}

		if (project.infoplist.versionPrefix) {
			version = project.infoplist.versionPrefix + version
		}

		logger.debug("Modify CFBundleVersion to {}", version)
		setValueForPlist(infoPlist, "CFBundleVersion", version)
	}


	private void modifyShortVersion(File infoPlist) {
		if (project.infoplist.shortVersionString == null && project.infoplist.shortVersionStringSuffix == null && project.infoplist.shortVersionStringPrefix == null) {
			return
		}

		def shortVersionString
		try {
			shortVersionString = getValueFromPlist(infoPlist, "CFBundleShortVersionString")
		} catch (IllegalStateException ex) {
			// no CFBundleShortVersionString exists so noting can be modified!
			return
		}

		if (project.infoplist.shortVersionString != null) {
			shortVersionString = project.infoplist.shortVersionString
		}
		if (project.infoplist.shortVersionStringSuffix) {
			shortVersionString = shortVersionString + project.infoplist.shortVersionStringSuffix
		}

		if (project.infoplist.shortVersionStringPrefix) {
			shortVersionString = project.infoplist.shortVersionStringPrefix + shortVersionString
		}

		logger.debug("Modify CFBundleShortVersionString to {}", shortVersionString)
		setValueForPlist(infoPlist, "CFBundleShortVersionString", shortVersionString)

	}

}