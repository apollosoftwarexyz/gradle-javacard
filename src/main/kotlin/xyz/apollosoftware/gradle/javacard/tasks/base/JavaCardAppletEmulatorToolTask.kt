package xyz.apollosoftware.gradle.javacard.tasks.base

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension
import xyz.apollosoftware.gradle.javacard.tasks.base.JavaCardAppletEmulatorToolTask.Companion.JCE_MAIN_CLASS
import javax.inject.Inject

/**
 * Base class for tasks using [JCardEngine](https://github.com/martinpaljak/JCardEngine).
 *
 * If the path to JCardEngine is not explicitly specified, it is automatically
 * fetched using the [xyz.apollosoftware.gradle.javacard.tasks.JavaCardAppletFetchJCardEngineTask].
 */
abstract class JavaCardAppletEmulatorToolTask: DefaultTask() {

    companion object {
        /**
         * The default main class to invoke in the JCardEngine JAR file.
         */
        const val JCE_MAIN_CLASS = "pro.javacard.engine.tool.JCardTool"
    }

    @get:InputDirectory
    abstract val emulatorClasspath: DirectoryProperty

    /**
     * The main class to invoke in the [emulatorClasspath].
     *
     * By default, this is [JCE_MAIN_CLASS].
     */
    @get:Input
    abstract val mainClass: Property<String>

    @get:Nested
    abstract val toolLauncher: Property<JavaLauncher>

    @get:Inject
    abstract val objectFactory: ObjectFactory

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        mainClass.convention(JCE_MAIN_CLASS)

        // This tool also targets Java 17 (presumably because of the Java Card
        // SDK) so that is used as the default here too.
        val javaCard = project.extensions.getByType(JavaCardGradleExtension::class.java)
        val defaultToolLauncher = javaCard.toolLauncher
        toolLauncher.convention(defaultToolLauncher)
    }

    /**
     * Invoke a tool.
     */
    protected fun tool(action: Action<JavaExecSpec>) {
        // Invoke the JCardEngine JAR
        execOperations.javaexec {
            it.executable = toolLauncher.get().executablePath.asFile.absolutePath
            it.classpath = objectFactory.fileCollection().from(emulatorClasspath.get().asFileTree)
            it.mainClass.set(mainClass)
            action.execute(it)
        }
    }

}
