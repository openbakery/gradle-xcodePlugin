package org.openbakery.signing;

import org.openbakery.CommandRunner;
import org.openbakery.codesign.ProvisioningProfileReader;
import org.openbakery.util.PlistHelper;

import java.io.File;

public class ProvisioningProfileReaderIgnoreExpired extends ProvisioningProfileReader {

	public ProvisioningProfileReaderIgnoreExpired(File provisioningProfile, CommandRunner commandRunner, PlistHelper plistHelper) {
		super(provisioningProfile, commandRunner, plistHelper);
	}

	public ProvisioningProfileReaderIgnoreExpired(File provisioningProfile, CommandRunner commandRunner) {
		super(provisioningProfile, commandRunner);
	}

	@Override
	public boolean checkExpired() {
		return false;
	}
}
