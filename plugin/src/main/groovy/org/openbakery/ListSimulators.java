package org.openbakery;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by rene on 13.08.14.
 */
public class ListSimulators extends DefaultTask {




	public ListSimulators() {
		setDescription("List all available iOS Simulators");
	}

	@TaskAction
	void list() {

		XcodeBuildPluginExtension xcodebuild = getProject().getExtensions().findByType(XcodeBuildPluginExtension.class);

		List<Destination> availableSimulators = xcodebuild.getAvailableSimulators();

		Collections.sort(availableSimulators, new Comparator<Destination>() {
			@Override
			public int compare(Destination first, Destination second) {
				int result = first.getOs().compareTo(second.getOs());
				if (result != 0) {
					return result;
				}
				return first.getName().compareTo(second.getName());
			}
		});

		String currentOS = "";
		for (Destination destination : availableSimulators) {

			if (!currentOS.equals(destination.getOs())) {
				getLogger().quiet("-- iOS {} -- ", destination.getOs());
				currentOS = destination.getOs();
			}

			String id = "";
			if (destination.getId() != null) {
				id = "(" + destination.getId() + ")";
			}

			getLogger().quiet("\t {} {}", destination.getName(), id);
		}


	}
}
