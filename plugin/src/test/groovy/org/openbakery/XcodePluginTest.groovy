package org.openbakery

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class XcodePluginTest {
    @Test
    public void greeterPluginAddsXcodeTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'xcode'

        assertTrue(project.tasks.xcodebuild instanceof XcodeTask)
    }
}
