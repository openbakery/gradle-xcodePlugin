package org.openbakery.testdouble

import org.openbakery.CommandRunner
import org.openbakery.tools.Lipo

class LipoFake extends Lipo {


	HashMap<String, String> archs = new HashMap<>()
	HashMap<String, String> removeArch = new HashMap<>()

	LipoFake() {
		super(XcodeFake, new CommandRunner())
	}

	@Override
	List<String> getArchs(String binaryName) {
		if (archs.containsKey(binaryName)) {
			return archs[binaryName]
		}
		return new ArrayList<>()
	}

	@Override
	void removeArch(String binaryName, String arch) {
		this.removeArch.put(binaryName, arch)
	}


}
