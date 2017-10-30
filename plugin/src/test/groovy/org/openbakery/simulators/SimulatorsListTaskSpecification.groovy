package org.openbakery.simulators

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import org.openbakery.testdouble.SimulatorControlStub
import spock.lang.Specification

class SimulatorsListTaskSpecification extends Specification {

    SimulatorsListTask task
    Project project
    File projectDir

    def setup() {
        projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.apply plugin: org.openbakery.XcodePlugin

        task = project.tasks.findByName(XcodePlugin.SIMULATORS_LIST_TASK_NAME)
        task.log = Mock(Logger)
    }

    def "instance if SimulatorListTask is created and has simulator control"() {
        expect:
        task instanceof SimulatorsListTask
        task.simulatorControl instanceof SimulatorControl
    }

    def "list xcode 9.1 simulators"() {
        when:
        task.simulatorControl = new SimulatorControlStub("simctl-list-xcode9_1.txt")

        task.list()

        then:
        1 * task.log.lifecycle('-- tvOS Simulator 11.1 --')
        then:
        1 * task.log.lifecycle('\tApple TV (850757D6-EFF0-49D7-9C13-54CB1AFB5076)')
        then:
        1 * task.log.lifecycle('\tApple TV 4K (05EF6541-D28F-4349-BB75-20397B1D63FD)')
        then:
        1 * task.log.lifecycle('\tApple TV 4K (at 1080p) (57F8D699-0EEB-4A16-8CC0-93A824C7C4F5)')
        then:
        1 * task.log.lifecycle('-- iOS Simulator 11.1 --')
        then:
        1 * task.log.lifecycle('\tiPhone 5s (B4CE41EC-3A8B-4C3F-A2ED-1E3B12ECDFA9)')


    }

}
