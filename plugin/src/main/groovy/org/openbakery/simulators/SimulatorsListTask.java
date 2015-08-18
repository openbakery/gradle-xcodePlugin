package org.openbakery.simulators;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.openbakery.Destination;
import org.openbakery.XcodeBuildPluginExtension;
import org.openbakery.configuration.XcodeConfig;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by rene on 13.08.14.
 */
public class SimulatorsListTask extends DefaultTask {

	private XcodeConfig config;
	private XcodeBuildPluginExtension xcodebuild;

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

		setDescription("List all available iOS Simulators");

		xcodebuild = getProject().getExtensions().findByType(XcodeBuildPluginExtension.class);

		config = new XcodeConfig(getProject(), xcodebuild.getBuildSpec());

	}

	void executeTask() {
		config.configuration();


		List<Destination> availableSimulators = xcodebuild.getAvailableSimulators();

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
				getLogger().lifecycle("-- iOS {} -- ", destination.getOs());
				currentOS = destination.getOs();
			}

			String id = "";
			if (destination.getId() != null) {
				id = "(" + destination.getId() + ")";
			}

			getLogger().lifecycle("\t {} {}", destination.getName(), id);
		}


	}
}
