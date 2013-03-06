package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author René Pirringer
 *
 */
class UIAutomationTestTask extends AbstractXcodeTask {



	public UIAutomationTestTask() {
		super()
		dependsOn("xcodebuild")
		this.description = "Runs the UIAutomation scripts"
	}

	@TaskAction
	def runTests() {

		File scriptsDirectory = new File(project.uiautomation.scriptsDirectory);


		String[] instrumentsCommand =  ["instruments", "-t", "/Applications/Xcode.app/Contents/Applications/Instruments.app/Contents/PlugIns/AutomationInstrument.bundle/Contents/Resources/Automation.tracetemplate" ]
		String applicationsFolder = System.getProperty("user.home") + "/Library/Application Support/iPhone Simulator/6.1/Applications/" + getAppBundleName();

		println "instrumentsCommand: " + instrumentsCommand;
		println "applicationsFolder: " + applicationsFolder;


		scriptsDirectory.list().each {
			println it


		}

	}
}
