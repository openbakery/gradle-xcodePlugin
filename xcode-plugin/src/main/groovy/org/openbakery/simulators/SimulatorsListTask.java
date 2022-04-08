package org.openbakery.simulators;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.openbakery.xcode.Destination;
import org.openbakery.xcode.Type;
import org.openbakery.XcodePlugin;

public class SimulatorsListTask extends AbstractSimulatorTask {

	protected Logger log = this.getLogger();

	int compareTo(String first, String second) {
		if (first == null && second == null) {
			return 0;
		}

		if (first == null && second != null) {
			return 1;
		}

		if (first != null && second == null) {
			return -1;
		}
		return first.compareTo(second);

	}

	public SimulatorsListTask() {
		setDescription("List all available Simulators");
	}

	@TaskAction
	void list() {
		listAllOfType(Type.tvOS);
		listAllOfType(Type.iOS);
	}

    void listAllOfType(Type type) {

		List<Destination> availableSimulators = getSimulatorControl().getAllDestinations(type);

		Collections.sort(availableSimulators, new Comparator<Destination>() {
			@Override
			public int compare(Destination first, Destination second) {

				int result = compareTo(first.getOs(), second.getOs());
				if (result != 0) {
					return result;
				}
				return compareTo(first.getName(), second.getName());
			}
		});

		String currentOS = "";
		for (Destination destination : availableSimulators) {

			if (!currentOS.equals(destination.getOs())) {
				log.lifecycle("-- " + destination.getPlatform() + " " + destination.getOs() + " --");
				currentOS = destination.getOs();
			}

			String id = "";
			if (destination.getId() != null) {
				id = "(" + destination.getId() + ")";
			}

			log.lifecycle("\t" + destination.getName() + " " + id);
		}


	}

}
