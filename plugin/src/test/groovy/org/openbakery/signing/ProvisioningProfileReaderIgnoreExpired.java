package org.openbakery.signing;

import org.openbakery.CommandRunner;
import org.openbakery.codesign.ProvisioningProfileReader;
import org.openbakery.util.PlistHelper;

import java.io.File;

/**
 * Created by rene on 04.02.16.
 */
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
