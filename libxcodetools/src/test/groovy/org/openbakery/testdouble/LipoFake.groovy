package org.openbakery.testdouble

import org.openbakery.CommandRunner
import org.openbakery.tools.Lipo

class LipoFake extends Lipo {


	HashMap<File, String> archs = new HashMap<>()
	HashMap<File, String> removeArch = new HashMap<>()

	LipoFake() {
		super(XcodeFake, new CommandRunner())
	}

	@Override
	List<String> getArchs(File binary) {
		if (archs.containsKey(binary)) {
			return archs[binary]
		}
		return new ArrayList<>()
	}

	@Override
	void removeArch(File binary, String arch) {
		this.removeArch.put(binary, arch)
	}


}
