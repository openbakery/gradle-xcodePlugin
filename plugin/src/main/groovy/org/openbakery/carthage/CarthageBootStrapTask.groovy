package org.openbakery.carthage

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import org.openbakery.CommandRunner
import org.openbakery.xcode.Type

import javax.inject.Inject
import java.util.regex.Pattern

@CacheableTask
@CompileStatic
class CarthageBootStrapTask extends DefaultTask {

	@InputFile
	@PathSensitive(PathSensitivity.NAME_ONLY)
	RegularFileProperty cartFile = project.layout.fileProperty()

	Property<Type> platform = project.objects.property(Type)

	@Input
	Provider<String> carthagePlatformName = platform.map {
		this.typeToCarthagePlatform(it)
	} as Provider<String>

	final Property<CommandRunner> commandRunnerProperty = project.objects.property(CommandRunner)

	final WorkerExecutor workerExecutor

	static final String CARTHAGE_FILE = "Cartfile"
	static final String CARTHAGE_PLATFORM_IOS = "iOS"
	static final String CARTHAGE_PLATFORM_MACOS = "Mac"
	static final String CARTHAGE_PLATFORM_TVOS = "tvOS"
	static final String CARTHAGE_PLATFORM_WATCHOS = "watchOS"

	static final Pattern LINE_PATTERN = ~/^(binary|github|git)\s"([^"^\/]+)\/(([^\/]+)||([^"]+)\/([^"]+).json)"."([^"]+)"$/

	@Inject
	CarthageBootStrapTask(WorkerExecutor workerExecutor) {
		super()
		this.workerExecutor = workerExecutor

		setDescription "Check out and build the Carthage project dependencies"

		cartFile.set(new File(project.rootProject.rootDir, "Cartfile.resolved"))

		onlyIf {
			return cartFile.asFile.get().exists()
		}
	}

	@OutputDirectories
	Map<String, File> getOutputFiles() {
		HashMap<String, File> result = new HashMap<>()
		cartFile.asFile
				.get()
				.getText()
				.readLines()
				.collect { return LINE_PATTERN.matcher(it) }
				.findAll { it.matches() }
				.collect { it.group(3) }
				.each {
			result.put(it, new File(project.rootProject.projectDir,
					"Carthage/Build/${carthagePlatformName.get()}/${it}.framework"))
		}

		return result
	}

	@TaskAction
	void update() {
		logger.warn('Bootstrap Carthage for platform ' + carthagePlatformName)

		List<String> names = cartFile.asFile
				.get()
				.getText()
				.readLines()
				.collect { return LINE_PATTERN.matcher(it) }
				.findAll { it.matches() }
				.collect { it.group(3) }
				.each { createWorker(it) }

		workerExecutor.await()
	}

	private void createWorker(String source) {
		workerExecutor.submit(CarthageBootstrapRunnable.class) { WorkerConfiguration config ->
			// Use the minimum level of isolation
			config.isolationMode = IsolationMode.PROCESS

			// Constructor parameters for the unit of work implementation
			config.params project.rootProject.projectDir,
					source,
					carthagePlatformName.get(),
					commandRunnerProperty.get()

			config.displayName = "Bootstrap " + source
		}
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
}
