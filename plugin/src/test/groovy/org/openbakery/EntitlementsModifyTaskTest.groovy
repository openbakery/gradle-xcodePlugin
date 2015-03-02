/**
 * Created by chaitanyar on 3/2/15.
 */

package org.openbakery

import ch.qos.logback.core.util.FileUtil
import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 25.07.14.
 */
class EntitlementsModifyTaskTest {


    Project project
    File projectDir
    EntitlementsModifyTask task
    File entitlementsFile

    GMockController mockControl
    CommandRunner commandRunnerMock

    @BeforeMethod
    def setup() {
        mockControl = new GMockController()
        commandRunnerMock = mockControl.mock(CommandRunner)

        projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.apply plugin: org.openbakery.XcodePlugin

        project.xcodebuild.entitlementsPath = "Entitlements.entitlements"

        task = project.tasks.findByName('entitlementsModify')
        task.setProperty("commandRunner", commandRunnerMock)

        entitlementsFile = new File(task.project.projectDir, "Entitlements.entitlements")
        FileUtils.write(entitlementsFile, "dummy")
    }

    @AfterMethod
    void cleanUp() {
        FileUtils.deleteDirectory(project.projectDir)
    }


    @Test
    void testModifyAppGroup() {
        project.xcodebuild.entitlementsConfig = ["com.apple.security.application-groups:0":"group.ZillowRE.test"]
        def command = "com.apple.security.application-groups:0"
        def value = "group.ZillowRE.test"

        List<String> commandList
        commandList?.clear()
        commandList = ["/usr/libexec/PlistBuddy", entitlementsFile.absolutePath, "-c", "Set :" + command + " " + value]
        commandRunnerMock.run(commandList).times(1)


        mockControl.play {
            task.prepare()
        }

    }
}
