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
		File destinationDirectory = new File(gradle.gradleUserHomeDir, "/caches/gxp")

		if (!destinationDirectory.exists()) {
			destinationDirectory.mkdirs()
		}

		downloadPmd(destinationDirectory)
		downloadObjcGrammar(destinationDirectory)

		def cp = computeClasspath(destinationDirectory)

		def projectDir = project.projectDir
		def xcodebuild = project.xcodebuild
		def buildDir = project.buildDir

		File outputFile = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("report/cpd/cpd.xml")
		commandRunner.setOutputFile(outputFile)

		def commands = [
						"java", "-Xmx512m",
						"-cp", "\"${cp.join(':')}\"",
						'net.sourceforge.pmd.cpd.CPD',
						"--minimum-tokens", "10",
						"--files", "${projectDir.absolutePath}/${xcodebuild.target}", "${projectDir.absolutePath}/${xcodebuild.target}Tests",
						"--language", "ObjectiveC",
						"--encoding", "UTF-8",
						"--format", "net.sourceforge.pmd.cpd.XMLRenderer"
		].collect { it.toString() }
		commandRunner.run(commands)

	}

	List<String> computeClasspath(File destinationDirectory) {
		def pmdLibDir = new File(destinationDirectory, "tools/pmd-${PMD_VERSION}/lib")
		def obcJarFile = new File(destinationDirectory, "ObjCLanguage-${OBJC_LANG_VERSION}.jar")

		if (pmdLibDir.listFiles()) {
			return pmdLibDir.listFiles()*.absolutePath + [obcJarFile.absolutePath]
		}
		return []
	}

	private void downloadObjcGrammar(File destinationDirectory) {
		def objCJarFile = new File(destinationDirectory, "ObjCLanguage-${OBJC_LANG_VERSION}.jar")
		def objcUrl = OBJC_LANG_SOURCE + "/ObjCLanguage-${OBJC_LANG_VERSION}.jar"
		if (!objCJarFile.exists()) {
			downloadFile(objCJarFile, objcUrl)
		}
	}

	private void downloadFile(File theFile, String theUrl) {
		this.ant.get([src: theUrl, dest: theFile])
	}

	private void downloadPmd(File destinationDirectory) {
		def pmdZipFile = new File(destinationDirectory, "pmd-bin-${PMD_VERSION}.zip")
		def pmdUrl = PMD_SOURCE + "/${PMD_VERSION}/pmd-bin-${PMD_VERSION}.zip"
		if (!pmdZipFile.exists()) {
			downloadFile(pmdZipFile, pmdUrl)
			ant.unzip(src: "${destinationDirectory}/pmd-bin-${PMD_VERSION}.zip", dest: "${destinationDirectory}/tools", overwrite: 'true')
		}
	}
}
