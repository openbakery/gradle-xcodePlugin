package org.openbakery.tools

import org.openbakery.Destination
import org.openbakery.Type
import org.openbakery.XcodeBuildPluginExtension

/**
 * Created by rene on 04.08.16.
 */
class XcodebuildParameters {

	String scheme
	String target
	boolean simulator
	Type type
	String workspace
	String configuration
	File dstRoot
	File objRoot
	File symRoot
	File sharedPrecompsDir
	File derivedDataPath
	List<String> arch
	def additionalParameters
	List<Destination> destinations


	public XcodebuildParameters() {
	}

	public XcodebuildParameters(XcodeBuildPluginExtension extension) {
		scheme = extension.scheme
		target = extension.target
		simulator = extension.simulator
		type = extension.type
		workspace = extension.workspace
		configuration = extension.configuration
		dstRoot = extension.dstRoot
		objRoot = extension.objRoot
		symRoot = extension.symRoot
		sharedPrecompsDir = extension.sharedPrecompsDir
		derivedDataPath = extension.derivedDataPath
		destinations = extension.availableDestinations.clone()
		if (extension.arch != null) {
			arch = extension.arch.clone()
		}
		additionalParameters = extension.additionalParameters
	}

	@Override
	public String toString() {
		return "XcodebuildParameters {" +
						", scheme='" + scheme + '\'' +
						", target='" + target + '\'' +
						", simulator=" + simulator +
						", type=" + type +
						", workspace='" + workspace + '\'' +
						", configuration='" + configuration + '\'' +
						", dstRoot=" + dstRoot +
						", objRoot=" + objRoot +
						", symRoot=" + symRoot +
						", sharedPrecompsDir=" + sharedPrecompsDir +
						", derivedDataPath=" + derivedDataPath +
						", arch=" + arch +
						", additionalParameters=" + additionalParameters +
						", destinations=" + destinations +
						'}';
	}
}
