import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.exec
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs

version = "2019.1"

project {
	buildType(BuildAndTest)

	params {
		param("RELEASE_VERSION", "0.19.2")
	}

}

object BuildAndTest : BuildType({
	name = "Build - Test"

	vcs {
		root(DslContext.settingsRoot)
	}

	steps {
		this.script {
			scriptContent = "./gradlew clean test -PversionNumber=%RELEASE_VERSION%"
		}
	}

	features {
		swabra { }
	}

	triggers {
		vcs {
		}
	}
})
