package org.openbakery.log


open class LoggerFactory {

	companion object Base {

		private var instance: LoggerFactory? = null

		@JvmStatic
		fun getInstance(): LoggerFactory {
			if (instance == null) {
				instance = LoggerFactory()
			}
			return instance!!
		}

		@JvmStatic
		fun setFactory(factory: LoggerFactory) {
			this.instance = factory
		}



	}

	open fun getLogger(name : String): Logger {
		return EmptyLogger()
	}

}
