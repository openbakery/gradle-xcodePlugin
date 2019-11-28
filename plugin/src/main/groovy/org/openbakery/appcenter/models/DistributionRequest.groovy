package org.openbakery.appcenter.models

class DistributionRequest {
	List<Destination> destinations
	String release_notes
	Boolean notify_testers
	Boolean mandatory_update

	DistributionRequest(List<String> destinationNames, String releaseNotes, Boolean notifyTesters, Boolean mandatoryUpdate) {
		List<Destination> destinationList = new ArrayList<>()

		destinationNames.each {
			destinationList.add(new Destination(it))
		}

		destinations = destinationList
		release_notes = releaseNotes
		notify_testers = notifyTesters
		mandatory_update = mandatoryUpdate
	}

	class Destination {
		String name

		Destination(String name) {
			this.name = name
		}
	}
}
