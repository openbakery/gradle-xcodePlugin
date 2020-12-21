package org.openbakery.carthage;

import org.gradle.api.Project;

public class CarthagePluginExtension {

	boolean cache = true;
	boolean xcframework = false;
	String command = null;

	public CarthagePluginExtension(Project project) {
	}


	CarthageParameters getParameters() {
		CarthageParameters result = new CarthageParameters();
		result.setCache(this.cache);
		result.setXcframework(this.xcframework);
		result.setCommand(this.command);
		return result;
	}


}
