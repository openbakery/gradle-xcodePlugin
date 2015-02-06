package org.openbakery

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.signing.PackageTask
import org.openbakery.signing.ProvisioningProfileIdReader
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * User: rene
 * Date: 07/11/14
 */
class ProvisioningProfileIdReaderTest {




    /**
     * Created by gugmaster on 05/02/15.
     */
    static class XcodePackageTaskOSXTest {

        Project project

        PackageTask packageTask;

        File projectDir
        File buildOutputDirectory
        File appDirectory

        GMockController mockControl
        CommandRunner commandRunnerMock

        @BeforeMethod
        void setup() {

            projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
            project = ProjectBuilder.builder().withProjectDir(projectDir).build()
            project.buildDir = new File(projectDir, 'build').absoluteFile
            project.apply plugin: org.openbakery.XcodePlugin
            project.xcodebuild.infoPlist = 'Info.plist'
            project.xcodebuild.productName = 'Example'
            project.xcodebuild.productType = 'app'
            project.xcodebuild.sdk = XcodePlugin.SDK_MACOSX
            project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

            packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)


            buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration)
            buildOutputDirectory.mkdirs()

            appDirectory = new File(buildOutputDirectory, "Example.app")
            appDirectory.mkdirs()

            File infoPlist = new File("../example/OSX/ExampleOSX/ExampleOSX/Info.plist")
            FileUtils.copyFile(infoPlist, new File(appDirectory, "" + "Contents/Info.plist"))
        }

        @Test
        void getNoIconMacOSX() {

            // Info.plist from Example.app
            File infoPlistInAppFile = new File(projectDir, "/build/sym/Debug/Example.app/Contents/Info.plist")

            def macOSXIcons = xcodeBuildArchiveTask.getMacOSXIcons(infoPlistInAppFile)

            assert macOSXIcons.size() == 0
        }
    }
}