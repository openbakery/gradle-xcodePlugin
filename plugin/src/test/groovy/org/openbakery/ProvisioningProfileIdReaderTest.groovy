package org.openbakery

import org.openbakery.signing.ProvisioningProfileIdReader
import org.testng.annotations.Test

/**
 * User: rene
 * Date: 07/11/14
 */
class ProvisioningProfileIdReaderTest {


	@Test
	void readUUIDFromFile() {
		ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader("src/test/Resource/test.mobileprovision");
		assert reader.getUUID().equals("FFFFFFFF-AAAA-BBBB-CCCC-DDDDEEEEFFFF")
	}

	@Test
	void readApplicationIdentifierPrefix() {
		ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader("src/test/Resource/test.mobileprovision");
		assert reader.getApplicationIdentifierPrefix().equals("AAAAAAAAAAA")
	}


	@Test
	void readApplicationIdentifier() {
		ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader("src/test/Resource/test.mobileprovision");
		assert reader.getApplicationIdentifier().equals("org.openbakery.Example")
	}


	@Test(expectedExceptions = [IllegalArgumentException.class])
	void readProfileHasExpired() {
		new ProvisioningProfileIdReader("src/test/Resource/expired.mobileprovision");
	}


}