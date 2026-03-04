package xyz.apollosoftware.gradle.javacard.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import xyz.apollosoftware.gradle.javacard.tasks.base.JavaCardAppletToolTask

/**
 * Task that builds the Java Card applet, given a configuration file.
 */
abstract class JavaCardAppletTask: JavaCardAppletToolTask() {

    /**
     * The files that should trigger the task to rebuild.
     */
    @get:InputFiles
    abstract val inputFiles: Property<FileCollection>

    /**
     * The location for the Java Card SDK converter tool's configuration.
     *
     * This would normally be built by the [JavaCardAppletConfigTask].
     */
    @get:InputFile
    abstract val appletConfigLocation: RegularFileProperty

    /**
     * The destination directory that the built applet is written to.
     *
     * (This must match the destination location supplied in the
     * [appletConfigLocation], but must be supplied here for Gradle's caching
     * mechanism to know whether to run the task).
     */
    @get:OutputDirectory
    abstract val appletDestinationLocation: DirectoryProperty

    @TaskAction
    fun executeTask() {
        logger.lifecycle("Building applet")

        tool {
            it.mainClass.set("com.sun.javacard.converter.Main")
            it.args = listOf("-config", appletConfigLocation.get().asFile.absolutePath)
        }
    }

}
