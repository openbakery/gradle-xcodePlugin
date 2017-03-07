package org.openbakery.configuration

import spock.lang.Specification

class ConfigurationFromPlistSpecification extends Specification {



	def "create instance"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")

		then:
		configuration instanceof ConfigurationFromPlist
	}

	def "instance if of type Configuartion"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")

		then:
		configuration instanceof Configuration
	}

	def "throws exception if plist does not exist"() {
		when:
		new ConfigurationFromPlist("file/does/not/exist")
		then:
		thrown(FileNotFoundException)
	}

	def "getString resturns null if not found"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.getString("not_found") == null

	}

	def "getString returns value"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.getString("com.apple.developer.default-data-protection") == "NSFileProtectionComplete"
	}

	def "getString returns a string if value is boolean"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.getString("com.apple.developer.siri") == "true"
	}

	def "getString returns a string if value is number"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.getString("com.apple.number") == "5"
	}


	def "getString returns null an value is a array"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.getString("com.apple.developer.associated-domains") == null
	}

	def "getArray value"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.getStringArray("com.apple.developer.associated-domains") instanceof List<String>
	}

	def "getStringArray returns null if value is not an array"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.getStringArray("com.apple.developer.siri") == []
	}

	def "getKeys returns keys"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.getKeys().size() == 5
		configuration.getKeys().contains("com.apple.developer.default-data-protection")
	}

	def "containsKey returns true if given key is present"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.containsKey("com.apple.developer.default-data-protection") == true
		configuration.containsKey("not_present") == false
	}
	
	def "get returns boolean"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.get("com.apple.developer.siri") instanceof Boolean
		configuration.get("com.apple.developer.siri") == true
	}
	
	def "get returns integer"() {
		when:
		def configuration = new ConfigurationFromPlist("src/test/Resource/entitlements.plist")
		then:
		configuration.get("com.apple.number") instanceof Integer
		configuration.get("com.apple.number") == 5
	}
}
