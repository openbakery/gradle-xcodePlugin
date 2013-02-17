package org.openbakery


class HockeyAppPluginExtension {
	def String outputDirectory = "build/hockeyapp"
	def String apiToken = null
	def String notes = "This build was uploaded using the gradle xcodePlugin"
    def String status = 2
    def String notify= 1
    def String notesType=1
}
