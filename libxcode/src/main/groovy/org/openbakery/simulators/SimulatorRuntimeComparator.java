package org.openbakery.simulators;

import java.util.Comparator;

public class SimulatorRuntimeComparator implements Comparator<SimulatorRuntime> {
	@Override
	public int compare(SimulatorRuntime first, SimulatorRuntime second) {

		int result = first.getType().getValue().compareTo(second.getType().getValue());
		if (result != 0) {
			return result;
		}

		return second.getVersion().compareTo(first.getVersion());
	}
}
