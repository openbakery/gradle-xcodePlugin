package org.openbakery.cpd

import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask

/**
 * Generates a CPD report on Objective C code
 * @author rahul
 * @since 13.07.15
 */
class CpdTask extends AbstractXcodeTask {

    private static final String PMD_VERSION = '4.2.5'
    private static final String OBJC_LANG_VERSION = '0.0.7-SNAPSHOT'

    CpdTask() {
        super()
        this.description = "Runs the CPD report on Objective C code"
    }

    @TaskAction
    def cpd() {
        def gradle = project.gradle
        def destDir = "${gradle.gradleUserHomeDir.absolutePath}/ios"

        def pmdZipFile = new File("${destDir}/pmd-bin-${PMD_VERSION}.zip")
        def pmdUrl = "http://tcpdiag.dl.sourceforge.net/project/pmd/pmd/${PMD_VERSION}/pmd-bin-${PMD_VERSION}.zip"
        if (!pmdZipFile.exists()) {
            pmdZipFile.withOutputStream { os ->
                pmdUrl.toURL().withInputStream { inputStream ->
                    os << inputStream
                }
            }

            ant.unzip(src: "${destDir}/pmd-bin-${PMD_VERSION}.zip", dest: "${destDir}/tools", overwrite: 'true')
        }

        def objCJarFile = new File("${destDir}/ObjCLanguage-${OBJC_LANG_VERSION}.jar")
        def objcUrl = "https://raw.githubusercontent.com/jkennedy1980/Objective-C-CPD-Language/master/releases/ObjCLanguage-${OBJC_LANG_VERSION}.jar"
        if (!objCJarFile.exists()) {
            objCJarFile.withOutputStream { os ->
                objcUrl.toURL().withInputStream { inputStream ->
                    os << inputStream
                }
            }
        }

        def cp = new File("${destDir}/tools/pmd-${PMD_VERSION}/lib").listFiles()*.absolutePath +
                new File("${destDir}/ObjCLanguage-${OBJC_LANG_VERSION}.jar").absolutePath

        def projectDir = project.projectDir
        def xcodebuild = project.xcodebuild
        def buildDir = project.buildDir

        commandRunner.setOutputFile(new File("${buildDir}/cpd.xml"))
        commandRunner.run(
                "java", "-Xmx512m",
                "-cp", "\"${cp.join(':')}\"",
                'net.sourceforge.pmd.cpd.CPD',
                "--minimum-tokens", "60",
                "--files", "${projectDir.absolutePath}/${xcodebuild.target}", "${projectDir.absolutePath}/${xcodebuild.target}Tests",
                "--language", "ObjectiveC",
                "--encoding", "UTF-8",
                "--format", "net.sourceforge.pmd.cpd.XMLRenderer"
        )

    }
}
