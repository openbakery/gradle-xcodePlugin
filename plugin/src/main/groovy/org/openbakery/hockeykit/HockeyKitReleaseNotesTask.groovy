/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery.hockeykit

import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin
import org.pegdown.PegDownProcessor

/**
 *
 * @author René Pirringer
 *
 */
class HockeyKitReleaseNotesTask extends AbstractHockeyKitTask {

	HockeyKitReleaseNotesTask() {
		super()
		dependsOn(XcodePlugin.HOCKEYKIT_ARCHIVE_TASK_NAME)
		this.description = "Creates the releasenotes.html and includes the notes that can be deployed to the HockeyKit Server"
	}

	void executeTask() {

		if (project.hockeykit.notes != null) {

			def matcher = project.hockeykit.notes =~ /^\s*"(.*)"$/

			String notes = "";
			if (project.hockeykit.notes ==~ /^<\w+>.*<\/\w+>$/) {
				notes = project.hockeykit.notes;
			} else {
				// convert markdown
				PegDownProcessor pegDownProcessor = new PegDownProcessor();
				notes = pegDownProcessor.markdownToHtml(project.hockeykit.notes)
			}
			String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><title>Release Notes</title></head><body>" + notes + "</body></html>";
			new File(getOutputDirectory(), "releasenotes.html").write(html, "UTF-8");

		}
	}

}