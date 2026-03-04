package xyz.apollosoftware.gradle.javacard.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleEmulatorExtension
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleModuleSpec
import java.io.File
import javax.inject.Inject

/**
 * Task that fetches [JCardEngine](https://github.com/martinpaljak/JCardEngine)
 * as a dependency and copies the JAR to a cache directory.
 *
 * The file is provided as an output so this can be attached to other tasks.
 */
abstract class JavaCardAppletFetchJCardEngineTask: DefaultTask() {

    companion object {
        /**
         * The default cache directory (within the build directory) to cache
         * the JCardEngine JAR in.
         */
        const val DEFAULT_CACHE_DIR_NAME = "jcardengine-cache"
    }

    /**
     * The JCardEngine module to use.
     *
     * See [xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleEmulatorExtension.jCardEngine]
     */
    @get:Nested
    abstract val jCardEngine: Property<JavaCardGradleModuleSpec>

    /**
     * The JCardTool module to use.
     *
     * See [xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleEmulatorExtension.jCardTool]
     */
    @get:Nested
    abstract val jCardTool: Property<JavaCardGradleModuleSpec>

    /**
     * A list of additional dependency notations to resolve.
     */
    @get:Input
    abstract val additionalDependencies: ListProperty<String>

    /**
     * The output directory where the fetched JARs will be written.
     */
    @get:OutputDirectory
    abstract val jarDestinationFolder: DirectoryProperty

    /**
     * An internal property used by the task to cache data resolved at
     * configuration time.
     */
    @get:Internal
    private val resolutionConfig: Provider<Set<File>>

    @get:Inject
    abstract val objectFactory: ObjectFactory

    @get:Inject
    abstract val dependencyFactory: DependencyFactory

    fun jCardEngine(action: Action<in JavaCardGradleModuleSpec>) {
        toolResolutionAction(action, jCardEngine) { extension -> extension.jCardEngine }
    }

    fun jCardTool(action: Action<in JavaCardGradleModuleSpec>) {
        toolResolutionAction(action, jCardTool) { extension -> extension.jCardTool }
    }

    /**
     * A convenience wrapper that clones the existing configuration for a
     * [JavaCardGradleModuleSpec] and allows each of the properties to be
     * overridden (this is useful for allowing independent configuration of the
     * module specs whilst still falling back to the defaults).
     */
    private fun toolResolutionAction(action: Action<in JavaCardGradleModuleSpec>, property: Property<JavaCardGradleModuleSpec>, defaultGetter: (JavaCardGradleEmulatorExtension) -> JavaCardGradleModuleSpec) {
        val javaCardEmulator = project.extensions.getByType(JavaCardGradleEmulatorExtension::class.java)
        val defaults = defaultGetter(javaCardEmulator)

        property.set(objectFactory.newInstance(JavaCardGradleModuleSpec::class.java).apply {
            repositoryUrl.convention(defaults.repositoryUrl)
            group.convention(defaults.group)
            module.convention(defaults.module)
            version.convention(defaults.version)
            action.execute(this)
        })
    }

    init {
        val javaCardEmulator = project.extensions.getByType(JavaCardGradleEmulatorExtension::class.java)
        jCardEngine.convention(javaCardEmulator.jCardEngine)
        jCardEngine.finalizeValueOnRead()
        jCardTool.convention(javaCardEmulator.jCardTool)
        jCardTool.finalizeValueOnRead()
        additionalDependencies.convention(javaCardEmulator.additionalDependencies)
        additionalDependencies.finalizeValueOnRead()

        jarDestinationFolder.convention(project.layout.buildDirectory.dir(DEFAULT_CACHE_DIR_NAME))

        resolutionConfig = project.provider {
            val jCardEngineSpec = jCardEngine.get()
            val jCardToolSpec = jCardTool.get()

            // For now, we assume that mavenCentral is available as a
            // repository.

            // If JCardEngine does not come from Maven Central (it currently
            // does not), we need to manually add the other repository. To
            // minimize configuration disruption, we ensure this applies only to
            // the specific artifact.

            if (!jCardEngineSpec.fromMavenCentral) {
                project.repositories.maven {
                    it.url = project.uri(jCardEngineSpec.repositoryUrl.get())
                    it.content { repository ->
                        if (jCardEngineSpec.fromMavenCentral) {
                            // Ensure the repository is only used for the artifact.
                            repository.includeVersion(
                                jCardEngineSpec.group.get(),
                                jCardEngineSpec.module.get(),
                                jCardEngineSpec.version.get(),
                            )
                        }
                    }
                }
            }

            if (!jCardToolSpec.fromMavenCentral) {
                project.repositories.maven {
                    it.url = project.uri(jCardToolSpec.repositoryUrl.get())

                    it.content { repository ->
                        if (jCardToolSpec.fromMavenCentral) {
                            // Ensure the repository is only used for the artifact.
                            repository.includeVersion(
                                jCardToolSpec.group.get(),
                                jCardToolSpec.module.get(),
                                jCardToolSpec.version.get(),
                            )
                        }
                    }
                }
            }

            val jCardEngine = project.dependencies.create(jCardEngineSpec.asDependency).apply {
                because("JCardEngine is used by another task.")
            }

            val jCardTool = project.dependencies.create(jCardToolSpec.asDependency).apply {
                because("JCardTool is used by another task.")
            }

            project.configurations.detachedConfiguration().apply {
                isTransitive = true

                // Force the specified versions to be used.
                resolutionStrategy.force(jCardEngineSpec.asDependency)
                resolutionStrategy.force(jCardToolSpec.asDependency)

                dependencies.add(jCardTool)
                dependencies.add(jCardEngine)

                additionalDependencies.get().forEach { dependencies.add(dependencyFactory.create(it)) }
            }.resolve()
        }
    }

    @TaskAction
    fun executeTask() {
        val destinationLocation = jarDestinationFolder.get()
        val destinationFolder = destinationLocation.asFile

        // Ensure we always re-create the folder if the task has run (this
        // ensures we've removed old JARs that are no longer needed).
        if (destinationFolder.exists()) {
            if (!destinationFolder.deleteRecursively()) {
                throw IllegalStateException("Cannot delete folder: ${destinationFolder.absolutePath}")
            }
        }
        destinationFolder.mkdirs()

        resolutionConfig.get().forEach { file ->
            logger.lifecycle("Fetched ${file.name}...")
            file.copyTo(destinationLocation.file(file.name).asFile, overwrite = true)
        }
    }

}
