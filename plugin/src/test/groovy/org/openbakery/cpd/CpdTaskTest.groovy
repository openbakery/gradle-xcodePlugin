package org.openbakery.cpd

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gmock.GMock
import org.gmock.GMockController
import org.gradle.api.AntBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rahul on 7/22/15.
 */
class CpdTaskTest {
    Project project
    CpdTask cpdTask

    GMockController mockControl
    CommandRunner commandRunnerMock
    AntBuilder antMock

    @BeforeMethod
    void setup() {
        mockControl = new GMockController()
        commandRunnerMock = mockControl.mock(CommandRunner)
        antMock = mockControl.mock(AntBuilder)

        File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.buildDir = new File('build').absoluteFile
        project.apply plugin:org.openbakery.XcodePlugin

        cpdTask = project.getTasks().getByPath('cpd') as CpdTask

        cpdTask.setProperty("commandRunner", commandRunnerMock)
    }

    @AfterMethod
    void cleanUp() {
        FileUtils.deleteDirectory(project.projectDir)
    }

/*    @Test
    void testDownloadAndRun() {

        def destDir = project.gradle.gradleUserHomeDir.absolutePath + "/ios"
        cpdTask.metaClass.getAnt = { antMock }

        antMock.get([
                src: 'http://tcpdiag.dl.sourceforge.net/project/pmd/pmd/4.2.5/pmd-bin-4.2.5.zip',
                dest: new File("${destDir}/pmd-bin-4.2.5.zip")
        ]).times(1).returns()
        antMock.get([
                src: 'https://raw.githubusercontent.com/jkennedy1980/Objective-C-CPD-Language/master/releases/ObjCLanguage-0.0.7-SNAPSHOT.jar',
                dest: new File("${destDir}/ObjCLanguage-0.0.7-SNAPSHOT.jar")
        ]).times(1)
        antMock.unzip(
                src: "${destDir}/pmd-bin-4.2.5.zip", dest: "${destDir}/tools", overwrite: 'true'
        ).times(1)

        commandRunnerMock.setOutputFile(new File("${project.buildDir}/cpd.xml")).times(1)
        commandRunnerMock.run([
                'java', '-Xmx512m',
                '-cp', "\"${destDir}/tools/pmd-4.2.5/lib/a.jar:${destDir}/tools/pmd-4.2.5/lib/b.jar:${destDir}/tools/pmd-4.2.5/lib/c.jar:${destDir}/ObjCLanguage-0.0.7-SNAPSHOT.jar\"",
                'net.sourceforge.pmd.cpd.CPD',
                '--minimum-tokens', '10',
                '--files', "${project.projectDir}/null", "${project.projectDir}/nullTests",
                '--language', 'ObjectiveC',
                '--encoding', 'UTF-8',
                '--format', 'net.sourceforge.pmd.cpd.XMLRenderer'
        ]).times(1)

        mockControl.play {
            cpdTask.cpd()
        }

    }*/

    @Test
    void testRunWithoutDownload() {

        def destDir = project.gradle.gradleUserHomeDir.absolutePath + "/ios"

        new File("${destDir}").mkdirs()
        new File("${destDir}/pmd-bin-4.2.5.zip").text = ""
        new File("${destDir}/ObjCLanguage-0.0.7-SNAPSHOT.jar").text = ""
        new File("${destDir}/tools/pmd-4.2.5/lib").mkdirs()
        new File("${destDir}/tools/pmd-4.2.5/lib/a.jar").text = ""
        new File("${destDir}/tools/pmd-4.2.5/lib/b.jar").text = ""
        new File("${destDir}/tools/pmd-4.2.5/lib/c.jar").text = ""

        commandRunnerMock.setOutputFile(new File("${project.buildDir}/cpd.xml")).times(1)
        commandRunnerMock.run([
                'java', '-Xmx512m',
                '-cp', "\"${destDir}/tools/pmd-4.2.5/lib/a.jar:${destDir}/tools/pmd-4.2.5/lib/b.jar:${destDir}/tools/pmd-4.2.5/lib/c.jar:${destDir}/ObjCLanguage-0.0.7-SNAPSHOT.jar\"",
                'net.sourceforge.pmd.cpd.CPD',
                '--minimum-tokens', '10',
                '--files', "${project.projectDir}/null", "${project.projectDir}/nullTests",
                '--language', 'ObjectiveC',
                '--encoding', 'UTF-8',
                '--format', 'net.sourceforge.pmd.cpd.XMLRenderer'
        ]).times(1)

        mockControl.play {
            cpdTask.cpd()
        }

    }


}
