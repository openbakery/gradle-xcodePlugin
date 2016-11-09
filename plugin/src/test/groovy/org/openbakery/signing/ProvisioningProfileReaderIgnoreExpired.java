package org.openbakery.signing;

import org.openbakery.CommandRunner;
import org.openbakery.codesign.ProvisioningProfileReader;

import java.io.File;

/**
 * Created by rene on 04.02.16.
 */
public class ProvisioningProfileReaderIgnoreExpired extends ProvisioningProfileReader {

	public ProvisioningProfileReaderIgnoreExpired(File provisioningProfile, CommandRunner commandRunner) {
		super(provisioningProfile, commandRunner);
	}

	@Override
	public boolean checkExpired() {
		return false;
	}
}
