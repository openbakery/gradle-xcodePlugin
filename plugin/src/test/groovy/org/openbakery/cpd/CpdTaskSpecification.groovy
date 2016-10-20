package org.openbakery.cpd

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.testdouble.AntBuilderStub
import spock.lang.Specification

class CpdTaskSpecification extends Specification {
	Project project
	CpdTask cpdTask

	CommandRunner commandRunner = Mock(CommandRunner)
	AntBuilderStub antBuilderStub = new AntBuilderStub()

	def setup() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		cpdTask = project.getTasks().getByPath('cpd') as CpdTask

		antBuilderStub = new AntBuilderStub()
		project.ant = antBuilderStub

		cpdTask.commandRunner = commandRunner
	}

	def cleanup() {
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

	def "create destination directory"() {
		when:
		cpdTask.cpd()

		File destinationDirectory = new File(project.gradle.gradleUserHomeDir, "/caches/gxp")

		then:
		destinationDirectory.exists()
	}


	def "run without download"() {
		given:

		def destDir = project.gradle.gradleUserHomeDir.absolutePath + "/caches/gxp"
		new File("${destDir}").mkdirs()
		new File("${destDir}/pmd-bin-4.2.5.zip").text = ""
		new File("${destDir}/ObjCLanguage-0.0.7-SNAPSHOT.jar").text = ""
		new File("${destDir}/tools/pmd-4.2.5/lib").mkdirs()
		new File("${destDir}/tools/pmd-4.2.5/lib/a.jar").text = ""
		new File("${destDir}/tools/pmd-4.2.5/lib/b.jar").text = ""
		new File("${destDir}/tools/pmd-4.2.5/lib/c.jar").text = ""


		when:
		cpdTask.cpd()

		then:
		1 * commandRunner.setOutputFile(new File("${project.buildDir}/report/cpd/cpd.xml"))
		1 * commandRunner.run([
						'java', '-Xmx512m',
						'-cp', "\"${destDir}/tools/pmd-4.2.5/lib/a.jar:${destDir}/tools/pmd-4.2.5/lib/b.jar:${destDir}/tools/pmd-4.2.5/lib/c.jar:${destDir}/ObjCLanguage-0.0.7-SNAPSHOT.jar\"",
						'net.sourceforge.pmd.cpd.CPD',
						'--minimum-tokens', '10',
						'--files', "${project.projectDir}/null", "${project.projectDir}/nullTests",
						'--language', 'ObjectiveC',
						'--encoding', 'UTF-8',
						'--format', 'net.sourceforge.pmd.cpd.XMLRenderer'
		])

	}

}
