package xyz.apollosoftware.gradle.javacard.dsl

import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Nested

/**
 * The Gradle DSL extension (javaCardEmulator) for configuring the Java Card
 * Gradle plugin's JCardEngine defaults (and/or project-specific options).
 */
abstract class JavaCardGradleEmulatorExtension {

    companion object {
        /**
         * The name of the extension in configuration.
         */
        const val NAME = "javaCardEmulator"

        /**
         * The default repository to use for the Java Card emulator.
         *
         * See: [https://gist.github.com/martinpaljak/c77d11d671260e24eef6c39123345cae](https://gist.github.com/martinpaljak/c77d11d671260e24eef6c39123345cae)
         */
        const val JCE_REPOSITORY = "https://mvn.javacard.pro/maven/"

        /**
         * The default group to use for the Java Card emulator.
         */
        const val JCE_GROUP = "com.github.martinpaljak"

        /**
         * The default module to use for the Java Card emulator.
         */
        const val JCE_MODULE = "jcardengine"

        /**
         * The default module to use for the Java Card emulator CLI.
         */
        const val JCT_MODULE = "jcard-tool"

        /**
         * The default version to use for the Java Card emulator.
         */
        const val JCE_VERSION = "26.02.19"
    }

    init {
        jCardEngine.repositoryUrl.convention(JCE_REPOSITORY)
        jCardEngine.group.convention(JCE_GROUP)
        jCardEngine.module.convention(JCE_MODULE)
        jCardEngine.version.convention(JCE_VERSION)

        jCardTool.repositoryUrl.convention(jCardEngine.repositoryUrl)
        jCardTool.group.convention(jCardEngine.group)
        jCardTool.module.convention(JCT_MODULE)
        jCardTool.version.convention(jCardEngine.version)

        // These are the optional dependencies from JCardTool:
        // https://github.com/martinpaljak/JCardEngine/blob/next/tool/pom.xml#L27-L44
        additionalDependencies.convention(listOf(
            "net.sf.jopt-simple:jopt-simple:5.0.4",
            "org.slf4j:slf4j-simple:2.0.17",
            "org.ow2.asm:asm:9.9.1",
        ))
    }

    /**
     * The JCardEngine Gradle module to use.
     *
     * By default, the [JCE_REPOSITORY], [JCE_GROUP], [JCE_MODULE] and
     * [JCE_VERSION] are used.
     *
     * This can be safely substituted for any version that is CLI-compatible
     * with the default version.
     */
    @get:Nested
    abstract val jCardEngine: JavaCardGradleModuleSpec

    fun jCardEngine(action: Action<in JavaCardGradleModuleSpec>)
        = action.execute(jCardEngine)

    /**
     * The JCardTool (component of JCardEngine) Gradle module to use.
     *
     * By default, [JCT_MODULE] and the jCardEngine repository URL, group and
     * version are used.
     *
     * This can be safely substituted for any version that is CLI-compatible
     * with the default version.
     */
    @get:Nested
    abstract val jCardTool: JavaCardGradleModuleSpec

    fun jCardTool(action: Action<in JavaCardGradleModuleSpec>)
        = action.execute(jCardTool)

    /**
     * A list of additional dependency notations to resolve.
     */
    abstract val additionalDependencies: ListProperty<String>

}