package org.openbakery.carthage;

import org.gradle.api.Project;
import org.openbakery.xcode.XcodebuildParameters;

public class CarthagePluginExtension {

	boolean cache = true;
	boolean xcframework = false;

	public CarthagePluginExtension(Project project) {
	}


	CarthageParameters getParameters() {
		CarthageParameters result = new CarthageParameters();
		result.setCache(this.cache);
		result.setXcframework(this.xcframework);
		return result;
	}


}
