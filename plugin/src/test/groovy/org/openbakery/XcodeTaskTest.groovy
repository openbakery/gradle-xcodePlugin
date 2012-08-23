package org.openbakery

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class XcodeTaskTest {
    @Test
    public void canAddTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('xcode', type: XcodeTask)
        assertTrue(task instanceof XcodeTask)
    }
}
