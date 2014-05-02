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
package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
	
import org.pegdown.PegDownProcessor

/**
 *
 * @author Stefan Gugarel
 *
 */
class SparkleReleaseNotesTask extends DefaultTask {

	SparkleReleaseNotesTask() {
		super()
		this.description = "Creates release notes when building Mac Apps with Sparkle"
	}
	
	@TaskAction
	def createReleaseNotes() {
		
		def notes = "$System.env.CHANGELOG"
		if (notes) {
		
			def matcher = notes =~ /^\s*"(.*)"$/

			if (notes ==~ /^<\w+>.*<\/\w+>$/) {
				notes = notes;
			} else {
				// convert markdown
				PegDownProcessor pegDownProcessor = new PegDownProcessor();
				notes = pegDownProcessor.markdownToHtml(notes)
			}
			String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><title>Release Notes</title></head><body>" + notes + "</body></html>";
			new File("releasenotes.html").write(html, "UTF-8");

		} else {
			println "No notes found"
		}
	}
	
}