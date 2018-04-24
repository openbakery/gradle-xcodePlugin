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

import org.gradle.api.tasks.*
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.xcode.Xcodebuild

//@CompileStatic
class XcodeBuildArchiveTaskIosAndTvOS extends AbstractXcodeBuildTask {

    public static final String FOLDER_ARCHIVE = "archive"
    public static final String FOLDER_XCCONFIG = "xcconfig"

    XcodeBuildArchiveTaskIosAndTvOS() {
        super()

        dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
        dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)

        this.description = "Prepare the app bundle that it can be archive"
    }

    @Input
    String getScheme() {
        return project.xcodebuild.scheme
    }

    @InputFile
    @Optional
    File getProvisioningFile() {
        return project.xcodebuild.signing.mobileProvisionFile.first()
    }

    @OutputFile
    File getOutputTextFile() {
        File file = new File(project.getBuildDir(), "xcodebuild-archive-output.txt")
        return file
    }

    @OutputDirectory
    File getOutputDirectory() {
        def archiveDirectory = new File(project.getBuildDir(), FOLDER_ARCHIVE
                + "/" + project.xcodebuild.scheme + ".xcarchive")
        archiveDirectory.mkdirs()
        return archiveDirectory
    }

    @OutputFile
    File getXcconfigFile() {
        return new File(getOutputDirectory(), "project.xcconfig")
    }

    @TaskAction
    private void archive() {
        generateXConfigFile()
        runArchiving()
    }

    private void generateXConfigFile() {
        File file = getProvisioningFile()
        if (file.exists()) {
            ProvisioningProfileReader reader = new ProvisioningProfileReader(file, commandRunner)
            println reader.getTeamIdentifierPrefix()
            println reader.getUUID()
        }
    }

    private void runArchiving() {
        Xcodebuild xcodebuild = new Xcodebuild(project.projectDir,
                commandRunner,
                xcode,
                parameters,
                getDestinations())

        commandRunner.setOutputFile(getOutputTextFile())

        xcodebuild.archive(getScheme(),
                getOutputDirectory(),
                project.file("test.xcconfig"))
    }
}
