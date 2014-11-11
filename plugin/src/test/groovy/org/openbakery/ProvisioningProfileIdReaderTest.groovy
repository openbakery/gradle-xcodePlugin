package org.openbakery

import org.openbakery.signing.ProvisioningProfileIdReader
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * User: rene
 * Date: 07/11/14
 */
class ProvisioningProfileIdReaderTest {

	ProvisioningProfileIdReader reader;

	@BeforeClass
	void setup() {
		reader = new ProvisioningProfileIdReader();
	}


	@Test
	void readUUIDFromFile() {
		String UUID = reader.readProvisioningProfileUUID("src/test/Resource/test.mobileprovision")
		assert UUID.equals("FFFFFFFF-AAAA-BBBB-CCCC-DDDDEEEEFFFF")
	}
}