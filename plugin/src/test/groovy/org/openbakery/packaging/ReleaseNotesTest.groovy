package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import org.openbakery.packaging.ReleaseNotesTask
import org.junit.Before
import org.junit.After
import org.junit.Test

class ReleaseNotesTest {

	File projectDir
	Project project
	ReleaseNotesTask releaseNotesTask

	@Before
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		FileUtils.copyFileToDirectory(new File("src/test/Resource/CHANGELOG.md"), projectDir)

		releaseNotesTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_RELEASE_NOTES_TASK_NAME)
	}

	@Test
	void checkCreationOfReleaseNotesHTMLFile() {

		releaseNotesTask.createReleaseNotes()

		File releaseNotesFile = new File(projectDir.absolutePath + "/build/package/releasenotes.html")

		assert(releaseNotesFile.exists())
	}

	@After
	void cleanUp() {
		FileUtils.deleteDirectory(projectDir)
	}
}
