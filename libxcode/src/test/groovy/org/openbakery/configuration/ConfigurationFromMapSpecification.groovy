package org.openbakery.configuration

import spock.lang.Specification

class ConfigurationFromMapSpecification extends Specification {


	def "create instance"() {
		when:
		Map<String, Object> data = [:]
		def configuration = new ConfigurationFromMap(data)

		then:
		configuration instanceof ConfigurationFromMap
	}

	def "instance if of type Configuration"() {
		when:
		Map<String, Object> data = [:]
		def configuration = new ConfigurationFromMap(data)

		then:
		configuration instanceof Configuration
	}

	def "throws exception if map is null"() {
		when:
		new ConfigurationFromMap(null)
		then:
		thrown(IllegalArgumentException)
	}

	def "getString return null if not found"() {
		when:
		Map<String, Object> data = [:]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.getString("not_found") == null
	}

	def "getString returns value"() {
		when:
		Map<String, Object> data = ["com.apple.developer.default-data-protection": "NSFileProtectionComplete"]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.getString("com.apple.developer.default-data-protection") == "NSFileProtectionComplete"
	}


	def "getString returns a string if value is boolean"() {
		when:
		Map<String, Object> data = ["com.apple.developer.siri": true]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.getString("com.apple.developer.siri") == "true"
	}

	def "getString returns a string if value is number"() {
		when:
		Map<String, Object> data = ["com.apple.developer.siri": 5]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.getString("com.apple.developer.siri") == "5"
	}

	def "getString returns null an value is a array"() {
		when:
		Map<String, Object> data = ["array": [:]]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.getString("array") == null
	}

	def "getArray value"() {
		when:
		Map<String, Object> data = ["array": ["test", "test"]]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.getStringArray("array") instanceof List<String>
	}

	def "getStringArray returns array if value is not an array"() {
		when:
		Map<String, Object> data = ["test": "test"]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.getStringArray("test") == ["test"]
	}

	def "containsKey return true if key is present"() {
		when:
		Map<String, Object> data = ["array": ["test", "test"]]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.containsKey("array") == true
		configuration.containsKey("not_present") == false
	}

	def "get returns boolean"() {
		when:
		Map<String, Object> data = ["value": true]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.get("value") instanceof Boolean
		configuration.get("value") == true
	}

	def "get returns integer"() {
		when:
		Map<String, Object> data = ["value": 5]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.get("value") instanceof Integer
		configuration.get("value") == 5
	}

	def "getKeys returns keys"() {
		when:
		Map<String, Object> data = ["first": 5, "second": "second"]
		def configuration = new ConfigurationFromMap(data)
		then:
		configuration.getKeys().size() == 2
		configuration.getKeys().contains("first")
	}
}
