package org.openbakery.cpd

import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask

/**
 * Generates a CPD report on Objective C code
 * @author rahul
 * @since 13.07.15
 */
@SuppressWarnings("GrMethodMayBeStatic")
class CpdTask extends AbstractXcodeTask {

    private static final String PMD_VERSION = '4.2.5'
    private static final String OBJC_LANG_VERSION = '0.0.7-SNAPSHOT'
    private static final String OBJC_LANG_SOURCE = "https://raw.githubusercontent.com/jkennedy1980/Objective-C-CPD-Language/master/releases"
    private static final String PMD_SOURCE = "http://tcpdiag.dl.sourceforge.net/project/pmd/pmd"

    CpdTask() {
        super()
        this.description = "Runs the CPD report on Objective C code"
    }

    @TaskAction
    def cpd() {
        def gradle = project.gradle
        String destDir = "${gradle.gradleUserHomeDir.absolutePath}/ios"

        downloadPmd(destDir)
        downloadObjcGrammar(destDir)

        def cp = computeClasspath(destDir)

        def projectDir = project.projectDir
        def xcodebuild = project.xcodebuild
        def buildDir = project.buildDir

        commandRunner.setOutputFile(new File("${buildDir}/cpd.xml"))

        def commands = [
                "java", "-Xmx512m",
                "-cp", "\"${cp.join(':')}\"",
                'net.sourceforge.pmd.cpd.CPD',
                "--minimum-tokens", "10",
                "--files", "${projectDir.absolutePath}/${xcodebuild.target}", "${projectDir.absolutePath}/${xcodebuild.target}Tests",
                "--language", "ObjectiveC",
                "--encoding", "UTF-8",
                "--format", "net.sourceforge.pmd.cpd.XMLRenderer"
        ].collect {it.toString()}
        commandRunner.run(commands)

    }

    List<String> computeClasspath(String destDir) {
        def pmdLibDir = new File("${destDir}/tools/pmd-${PMD_VERSION}/lib")
        def obcJarFile = new File("${destDir}/ObjCLanguage-${OBJC_LANG_VERSION}.jar")

        pmdLibDir.listFiles()*.absolutePath + [obcJarFile.absolutePath]
    }

    private void downloadObjcGrammar(String destDir) {
        def objCJarFile = new File("${destDir}/ObjCLanguage-${OBJC_LANG_VERSION}.jar")
        def objcUrl = OBJC_LANG_SOURCE + "/ObjCLanguage-${OBJC_LANG_VERSION}.jar"
        if (!objCJarFile.exists()) {
            downloadFile(objCJarFile, objcUrl)
        }
    }

    private void downloadFile(File theFile, String theUrl) {
        this.ant.get([src: theUrl, dest: theFile])
    }

    private void downloadPmd(String destDir) {
        def pmdZipFile = new File("${destDir}/pmd-bin-${PMD_VERSION}.zip")
        def pmdUrl = PMD_SOURCE + "/${PMD_VERSION}/pmd-bin-${PMD_VERSION}.zip"
        if (!pmdZipFile.exists()) {
            downloadFile(pmdZipFile, pmdUrl)
            ant.unzip(src: "${destDir}/pmd-bin-${PMD_VERSION}.zip", dest: "${destDir}/tools", overwrite: 'true')
        }
    }
}
