package org.openbakery.signing

import org.apache.commons.io.FilenameUtils

class ProvisioningFile implements Serializable {

	private File file
	private String applicationIdentifier
	private String uuid
	private String teamIdentifier
	private String teamName
	private String name

	public static final String PROVISIONING_NAME_BASE = "gradle-"

	ProvisioningFile(File file,
					 String applicationIdentifier,
					 String uuid,
					 String teamIdentifier,
					 String teamName,
					 String name) {
		this.applicationIdentifier = applicationIdentifier
		this.file = file
		this.uuid = uuid
		this.teamIdentifier = teamIdentifier
		this.teamName = teamName
		this.name = name
	}

	String getApplicationIdentifier() {
		return applicationIdentifier
	}

	File getFile() {
		return file
	}

	String getUuid() {
		return uuid
	}

	String getTeamIdentifier() {
		return teamIdentifier
	}

	String getTeamName() {
		return teamName
	}

	String getName() {
		return name
	}

	String getFormattedName() {
		return formattedName(uuid, file)
	}

	public static String formattedName(String uuid, File file) {
		return PROVISIONING_NAME_BASE + uuid + "." + FilenameUtils.getExtension(file.getName())
	}
}
