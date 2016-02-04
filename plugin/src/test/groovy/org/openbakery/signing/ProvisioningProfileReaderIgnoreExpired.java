package org.openbakery.signing;

import org.openbakery.CommandRunner;

/**
 * Created by rene on 04.02.16.
 */
public class ProvisioningProfileReaderIgnoreExpired extends ProvisioningProfileReader {

	public ProvisioningProfileReaderIgnoreExpired(Object provisioningProfile, Object project, CommandRunner commandRunner) {
		super(provisioningProfile, project, commandRunner);
	}

	@Override
	public boolean checkExpired() {
		return false;
	}
}
