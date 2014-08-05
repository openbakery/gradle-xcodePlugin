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
package org.openbakery.sparkle

import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask

/**
 *
 * @author Stefan Gugarel
 *
 */
class SparkleArchiveTask extends DefaultTask {

	SparkleArchiveTask() {
		super()
		dependsOn("xcodebuild", "sparkle-notes")
		this.description = "Compresses app to ZIP when building Mac Apps with Sparkle"
	}
	
	@TaskAction
	def archiveApp() {

		if(!project.sparkle.appDirectory.exists()) {
		 	throw new IllegalArgumentException("Invalid app name specified for Sparkle: " + project.sparkle.appName)
	 	}

		if (!project.sparkle.outputDirectory.exists()) {
			project.sparkle.outputDirectory.mkdirs();
		}

		def ant = new groovy.util.AntBuilder()
		ant.zip(destfile: project.sparkle.outputDirectory.path + "/" + project.sparkle.appName +  ".zip") {
			zipfileset ( prefix:project.sparkle.fullAppName + "/Contents/", dir: project.sparkle.appDirectory.path, excludes : "MacOS/*", includes : "*/**");
			zipfileset ( prefix:project.sparkle.fullAppName + "/Contents/MacOS", dir: project.sparkle.appDirectory.path + "/MacOS", includes : "*", filemode : 755);
			zipfileset ( prefix:project.sparkle.fullAppName + "/Contents/Frameworks/Sparkle.framework/Resources/finish_installation.app/Contents/MacOS", dir: project.sparkle.appDirectory.path + "/Frameworks/Sparkle.framework/Resources/finish_installation.app/Contents/MacOS/", includes : "*", filemode : 755);
		}
		
		ant.move(file: "releasenotes.html",  todir: project.sparkle.outputDirectory, quiet: true)
	}

}