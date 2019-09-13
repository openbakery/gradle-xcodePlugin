package org.openbakery.appcenter.models

class DistributionRequest {
	String destination_name
	String release_notes
	Boolean notify_testers
	Boolean mandatory_update

	DistributionRequest(String destinationName, String releaseNotes, Boolean notifyTesters, Boolean mandatoryUpdate) {
		destination_name = destinationName
		release_notes = releaseNotes
		notify_testers = notifyTesters
		mandatory_update = mandatoryUpdate
	}
}
