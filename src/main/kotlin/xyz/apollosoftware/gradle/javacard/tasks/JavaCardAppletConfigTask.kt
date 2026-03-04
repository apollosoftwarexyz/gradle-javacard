package xyz.apollosoftware.gradle.javacard.tasks

import xyz.apollosoftware.gradle.javacard.JavaCardAPIVersion
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleApplet
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension

/**
 * Task that builds the configuration for the Java Card SDK converter tool.
 */
abstract class JavaCardAppletConfigTask: DefaultTask() {

    /**
     * See [xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension.apiVersion]
     */
    @get:Input
    abstract val apiVersion: Property<JavaCardAPIVersion>

    /**
     * See [xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension.aid]
     */
    @get:Input
    abstract val aid: Property<String>

    /**
     * See [xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension.packageName]
     */
    @get:Input
    abstract val packageName: Property<String>

    /**
     * The applet version.
     *
     * By default, this is typically set to the project version.
     */
    @get:Input
    abstract val version: Property<String>

    /**
     * See [xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension.applets]
     */
    @get:Nested
    abstract val applets: SetProperty<JavaCardGradleApplet>

    /**
     * See [xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension.support32BitIntegers]
     */
    @get:Input
    abstract val support32BitIntegers: Property<Boolean>

    /**
     * See [xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension.debug]
     */
    @get:Input
    abstract val debug: Property<Boolean>

    /**
     * The path to the directory that contains the classes to convert to an
     * applet.
     */
    @get:Input
    abstract val classesDir: Property<String>

    /**
     * The path to the directory where the built applet artifacts (CAP files)
     * should be placed.
     */
    @get:Input
    abstract val appletDestinationDir: Property<String>

    /**
     * The config file produced by the task.
     */
    @get:OutputFile
    abstract val appletConfigLocation: RegularFileProperty

    init {
        val javaCard = project.extensions.getByType(JavaCardGradleExtension::class.java)

        apiVersion.apply {
            convention(javaCard.apiVersion)
            finalizeValueOnRead()
        }

        aid.apply {
            convention(javaCard.aid)
            finalizeValueOnRead()
        }

        packageName.apply {
            convention(javaCard.packageName)
            finalizeValueOnRead()
        }

        applets.apply {
            convention(javaCard.applets)
            finalizeValueOnRead()
        }

        version.apply {
            convention(project.version.toString())
            finalizeValueOnRead()
        }

        support32BitIntegers.apply {
            convention(javaCard.support32BitIntegers)
            finalizeValueOnRead()
        }

        debug.apply {
            convention(javaCard.debug)
            finalizeValueOnRead()
        }
    }

    @TaskAction
    fun executeTask() {
        val applets = applets.get().toSortedSet(compareBy { applet -> applet.aid })
        logger.lifecycle("Writing config for ${applets.size} applet${if (applets.size == 1) "" else "s"} from ${packageName.get()} with API version ${apiVersion.get().version()}")

        // Map the applet entries into config lines.
        val appletConfigs = applets.map {
            val appletClass = listOf(packageName.get(), it.appletClass.get()).joinToString(".")
            logger.lifecycle("\t${aid.get()}:${it.aid} -> $appletClass")
            "-applet ${aid.get()}:${it.aid} $appletClass"
        }.toTypedArray()

        val file = appletConfigLocation.get().asFile
        file.parentFile.mkdirs()
        file.writeText(listOfNotNull(
            "-nobanner",
            if (support32BitIntegers.get()) "-i" else null,
            if (debug.get()) "-debug" else null,
            *appletConfigs,
            "-out CAP JCA EXP",
            "-classdir ${classesDir.get()}",
            "-d ${appletDestinationDir.get()}",
            "-target ${apiVersion.get().version()}",
            packageName.get(),
            "${aid.get()} ${version.get()}",
            "",
        ).joinToString("\n"))
        logger.lifecycle("Done writing configuration: $file")
    }

}