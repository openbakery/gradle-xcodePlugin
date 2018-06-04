package org.openbakery.carthage

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecSpec
import org.gradle.workers.WorkerExecutor
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode

import javax.inject.Inject

@CacheableTask
@CompileStatic
class CarthageBootStrapTask extends DefaultTask {

	@Input
	@Optional
	Property<String> requiredXcodeVersion = project.objects.property(String)

	@Input
	Property<String> carthagePlatformName = project.objects.property(String)

	@InputFile
	@PathSensitive(PathSensitivity.NAME_ONLY)
	RegularFileProperty cartFile = project.layout.fileProperty()

	@OutputDirectory
	Property<File> outputDirectory = project.objects.property(File)

	public static final String NAME = "carthageBootstrap"

	final Property<Type> platform = project.objects.property(Type)
	final Property<Xcode> xcodeProperty = project.objects.property(Xcode)
	final Property<CommandRunner> commandRunnerProperty = project.objects.property(CommandRunner)

	final WorkerExecutor workerExecutor

	static final String CARTHAGE_FILE = "Cartfile.resolved"
	static final String CARTHAGE_PLATFORM_IOS = "iOS"
	static final String CARTHAGE_PLATFORM_MACOS = "Mac"
	static final String CARTHAGE_PLATFORM_TVOS = "tvOS"
	static final String CARTHAGE_PLATFORM_WATCHOS = "watchOS"
	static final String CARTHAGE_USR_BIN_PATH = "/usr/local/bin/carthage"
	static final String ACTION_BOOTSTRAP = "bootstrap"
	static final String ARG_PLATFORM = "--platform"
	static final String ARG_CACHE_BUILDS = "--cache-builds"

	@Inject
	CarthageBootStrapTask(WorkerExecutor workerExecutor) {
		super()
		this.workerExecutor = workerExecutor

		setDescription "Check out and build the Carthage project dependencies"

		cartFile.set(new File(project.rootProject.rootDir, CARTHAGE_FILE))

		carthagePlatformName.set(platform.map(new Transformer<String, Type>() {
			@Override
			String transform(Type type) {
				return typeToCarthagePlatform(type)
			}
		}))

		outputDirectory.set(carthagePlatformName.map(new Transformer<File, String>() {
			@Override
			File transform(String platformName) {
				return new File(project.rootProject.rootDir, "Carthage/Build/" + platformName)
			}
		}))

		xcodeProperty.set(commandRunnerProperty.map(new Transformer<Xcode, CommandRunner>() {
			@Override
			Xcode transform(CommandRunner commandRunner) {
				return new Xcode(commandRunner)
			}
		}))

		onlyIf {
			return cartFile.asFile.get().exists()
		}
	}

	void setXcode(Xcode xcode) {
		this.xcode = xcode
	}

	@TaskAction
	void update() {
		logger.warn('Bootstrap Carthage for platform ' + carthagePlatformName)
		project.exec(new Action<ExecSpec>() {
			@Override
			void execute(ExecSpec execSpec) {
				execSpec.args = [ACTION_BOOTSTRAP,
								 ARG_CACHE_BUILDS,
								 "--new-resolver",
								 "--color", "always",
								 ARG_PLATFORM,
								 carthagePlatformName.getOrNull().toString()] as List<String>

				execSpec.environment(getEnvValues())
				execSpec.executable = getCarthageCommand()
				execSpec.workingDir(project.rootProject.projectDir)
			}
		})
	}

	private final Map<String, String> getEnvValues() {
		final Map<String, String> envValues
		if (requiredXcodeVersion.present) {
			envValues = xcodeProperty.get()
					.getXcodeSelectEnvValue(requiredXcodeVersion.getOrNull())
		} else {
			envValues = [:]
		}

		return envValues
	}

	private String typeToCarthagePlatform(Type type) {
		switch (type) {
			case Type.iOS: return CARTHAGE_PLATFORM_IOS
			case Type.tvOS: return CARTHAGE_PLATFORM_TVOS
			case Type.macOS: return CARTHAGE_PLATFORM_MACOS
			case Type.watchOS: return CARTHAGE_PLATFORM_WATCHOS
			default: return 'all'
		}
	}

	private String getCarthageCommand() {
		try {
			return commandRunnerProperty.get()
					.runWithResult("which", "carthage")
		} catch (CommandRunnerException exception) {
			// ignore, because try again with full path below
		}

		try {
			commandRunnerProperty.get()
					.runWithResult("ls", CARTHAGE_USR_BIN_PATH)
			return CARTHAGE_USR_BIN_PATH
		} catch (CommandRunnerException exception) {
			// ignore, because blow an exception is thrown
		}

		throw new IllegalStateException("The carthage command was not found. Make sure that Carthage is installed")
	}
}
