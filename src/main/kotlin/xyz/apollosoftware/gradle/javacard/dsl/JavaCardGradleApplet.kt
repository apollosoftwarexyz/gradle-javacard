package xyz.apollosoftware.gradle.javacard.dsl

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import javax.inject.Inject

/**
 * A repeatable Gradle DSL extension for defining and configuring Java Card
 * applets, named by their Application Identifier (AID).
 *
 * This is intended to be used as a nested part of [JavaCardGradleExtension].
 */
abstract class JavaCardGradleApplet @Inject constructor(@Input val aid: String): Named {

    /**
     * Returns the applet's Application Identifier (AID).
     */
    @Internal
    override fun getName(): String {
        return aid
    }

    /**
     * The applet class name, relative to the package.
     */
    @get:Input
    abstract val appletClass: Property<String>

    init {
        appletClass.finalizeValueOnRead()
    }

}
