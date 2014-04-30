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

import org.gradle.api.Project


class SparklePluginExtension {
	def String appName = null
	def String fullAppName = null
	def Object outputDirectory
	def Object appDirectory

    private final Project project

    public SparklePluginExtension(Project project) {

        super()

        this.outputDirectory = {
            return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("sparkle")
        }


        this.project = project
    }

    File getOutputDirectory() {
        return project.file(outputDirectory)
    }

    void setOutputDirectory(Object outputDirectory) {
        this.outputDirectory = outputDirectory
    }

    String getAppName() {

        if (appName)
        {
            return appName
        }

        // default setting in XCode is target name
        return project.xcodebuild.target
    }

    String getFullAppName() {

        return getAppName() + '.app';
    }

    void setAppDirectory(Object appDirectory) {
        this.appDirectory = appDirectory
    }

    File getAppDirectory() {

        // build path for Contents in app bundle
        return project.file(project.xcodebuild.symRoot.absolutePath + '/' + project.xcodebuild.configuration + '/' + getFullAppName() +  '/Contents')
    }
}
