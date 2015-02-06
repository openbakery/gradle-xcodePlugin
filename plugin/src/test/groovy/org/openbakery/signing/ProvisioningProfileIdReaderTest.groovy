package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import org.openbakery.signing.ProvisioningProfileIdReader

/**
 * Created by gugmaster on 05/02/15.
 */
class ProvisioningProfileIdReaderTest {

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
    void readUUIDFromFile() {
        ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader("src/test/Resource/test.mobileprovision");
        assert reader.getUUID().equals("FFFFFFFF-AAAA-BBBB-CCCC-DDDDEEEEFFFF")
    }

    @Test
    void readApplicationIdentifierPrefix() {
        ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader("src/test/Resource/test.mobileprovision");
        assert reader.getApplicationIdentifierPrefix().equals("AAAAAAAAAAA")
    }


    @Test
    void readApplicationIdentifier() {
        ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader("src/test/Resource/test.mobileprovision");
        assert reader.getApplicationIdentifier().equals("org.openbakery.Example")
    }


    @Test(expectedExceptions = [IllegalArgumentException.class])
    void readProfileHasExpired() {
        new ProvisioningProfileIdReader("src/test/Resource/expired.mobileprovision");
    }


    // OSX Tests

    @Test
    void readMacProvisioningProfile() {

        File wildcardMacProfile = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")

        assert wildcardMacProfile.exists()

        ProvisioningProfileIdReader provisioningProfileReader = new ProvisioningProfileIdReader(wildcardMacProfile)

        def applcationIdentifier = provisioningProfileReader.getApplicationIdentifier()

        assert applcationIdentifier.equals("*")

    }

}
