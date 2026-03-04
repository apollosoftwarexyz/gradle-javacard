package xyz.apollosoftware.gradle.javacard.dsl

import xyz.apollosoftware.gradle.javacard.JavaCardAPIVersion
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.jvm.toolchain.JavaLauncher

/**
 * The Gradle DSL extension (javaCard) for configuring the Java Card Gradle
 * plugin defaults (and/or project-specific options).
 */
abstract class JavaCardGradleExtension {

    companion object {
        /**
         * The name of the extension in configuration.
         */
        const val NAME = "javaCard"

        /**
         * The Gradle system property that should be checked as a fallback for
         * the location of the Java Card SDK tools.
         */
        const val TOOLS_PATH_PROPERTY = "xyz.apollosoftware.gradle.javacard.tools-path"

        /**
         * The environment variable that should be checked as the final fallback
         * for the location of the Java Card SDK tools.
         */
        const val TOOLS_PATH_ENV_VAR = "JC_HOME_TOOLS"

        /**
         * Application Identifier (AID) pattern.
         */
        val AID_PATTERN = Regex("^0x[0-9]{2}(:0x[0-9]{2}){4,15}$")
    }

    init {
        compiler.finalizeValueOnRead()
        toolLauncher.finalizeValueOnRead()
        sourceSet.finalizeValueOnRead()
        toolsPath.finalizeValueOnRead()
        apiVersion.finalizeValueOnRead()
        aid.finalizeValueOnRead()
        packageName.finalizeValueOnRead()

        support32BitIntegers.apply {
            finalizeValueOnRead()
            convention(false)
        }

        debug.apply {
            finalizeValueOnRead()
            convention(false)
        }
    }

    /**
     * The Java compiler to use to compile the applet classes.
     *
     * Defaults to a JDK 17 toolchain compiler, which is recommended by the Java
     * Card SDK.
     */
    abstract val compiler: Property<JavaCompiler>

    /**
     * The Java version used to run tools.
     *
     * Defaults to a JDK 17 toolchain launcher, which is recommended by the Java
     * Card SDK.
     */
    abstract val toolLauncher: Property<JavaLauncher>

    /**
     * The source set containing the applet classes to be compiled.
     */
    abstract val sourceSet: Property<SourceSet>

    /**
     * The tools path can be used to explicitly define where the Java Card SDK
     * tools can be found.
     *
     * If not specified, the Gradle system property
     * `xyz.apollosoftware.gradle.javacard.tools-path` is used instead. If that
     * is also not specified, the environment variable `JC_HOME_TOOLS` is
     * checked.
     */
    abstract val toolsPath: DirectoryProperty

    /**
     * The Java Card API version to use.
     *
     * See [getValidApiVersion].
     */
    abstract val apiVersion: Property<JavaCardAPIVersion>

    /**
     * Validate and get the Java Card API version.
     *
     * The version must be set.
     */
    @Internal
    fun getValidApiVersion(): JavaCardAPIVersion {
        if (!apiVersion.isPresent) {
            throw GradleException("""
                Java Card API Version is not set. Please set the apiVersion in the javaCard { } block in your configuration, like so:
                
                    import xyz.apollosoftware.gradle.javacard.JavaCardAPIVersion
                
                    /* ... */
                
                    javaCard {
                        apiVersion = JavaCardAPIVersion.of("3.0.4")
                
                        /* ... */
                    }
            """.trimIndent())
        }

        return apiVersion.get()
    }

    /**
     * The package Application Identifier (AID).
     *
     * See [getValidAid].
     */
    abstract val aid: Property<String>

    /**
     * Validate and get the package Application Identifier (AID).
     *
     * This is 5 to 16 bytes in length. For readability, this must be specified
     * in the colon-delimited hexadecimal convention.
     */
    @Internal
    fun getValidAid(): String {
        if (!aid.isPresent) {
            throw GradleException(
                """
                Java Card package Application ID (AID) is not set. Please set the aid in the javaCard { } block in your configuration, like so:
                
                /* ... */
                
                javaCard {
                    /* ... */
                    aid = "0x00:0x00:0x00:0x00:0x00"
                }
            """.trimIndent()
            )
        }

        val resolvedAid = aid.get()
        if (!resolvedAid.matches(AID_PATTERN)) {
            throw IllegalArgumentException("Invalid Java Card Application ID (AID): `$resolvedAid` (the AID must be 5 to 16 bytes where each byte is represented as hex: 0x00-0xFF)")
        }

        return resolvedAid
    }

    /**
     * The fully qualified name of the package for the CAP file.
     */
    abstract val packageName: Property<String>

    /**
     * Whether to enable 32-bit integer support (the `-i` flag).
     *
     * This is disabled by default.
     */
    abstract val support32BitIntegers: Property<Boolean>

    /**
     * Whether to generate the optional debug component of the CAP file.
     *
     * This is disabled by default.
     */
    abstract val debug: Property<Boolean>

    /**
     * The list of applets to include in the built CAP file.
     */
    abstract val applets: NamedDomainObjectContainer<JavaCardGradleApplet>

    fun applets(action: Action<in NamedDomainObjectContainer<JavaCardGradleApplet>>)
            = action.execute(applets)

}
