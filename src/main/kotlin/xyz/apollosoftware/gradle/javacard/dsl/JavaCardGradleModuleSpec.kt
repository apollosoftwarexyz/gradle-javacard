package xyz.apollosoftware.gradle.javacard.dsl

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

/**
 * A specification for a module that is used by the Java Card Gradle plugin.
 */
abstract class JavaCardGradleModuleSpec {

    init {
        repositoryUrl.finalizeValueOnRead()
        group.finalizeValueOnRead()
        module.finalizeValueOnRead()
        version.finalizeValueOnRead()
    }

    /**
     * The maven repository URL to fetch the artifact from.
     *
     * To use Maven Central (or other repositories already available to the
     * project), set this to an empty string.
     *
     * Example: `https://mvn.javacard.pro/maven`
     */
    @get:Input
    abstract val repositoryUrl: Property<String>

    /**
     * Whether the artifact should be resolved from Maven Central.
     */
    internal val fromMavenCentral: Boolean
        @Internal
        get() = repositoryUrl.get().isEmpty()

    /**
     * The module group.
     *
     * Example: `com.github.martinpaljak`
     */
    @get:Input
    abstract val group: Property<String>

    /**
     * The module name (in Maven terminology, this is the 'artifact').
     *
     * Example: `jcardengine`
     */
    @get:Input
    abstract val module: Property<String>

    /**
     * The module/artifact version.
     *
     * Example: `25.12.14`
     */
    @get:Input
    abstract val version: Property<String>

    /**
     * Return the module in Gradle dependency notation.
     */
    internal val asDependency: String
        @Internal
        get() = listOf(group.get(), module.get(), version.get()).joinToString(":")

}