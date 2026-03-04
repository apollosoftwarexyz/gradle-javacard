package xyz.apollosoftware.gradle.javacard.tasks.base

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlin.text.Charsets.UTF_8

/**
 * Base class for using the Java Card SDK tools.
 */
abstract class JavaCardAppletToolTask: DefaultTask() {

    @get:Nested
    abstract val toolLauncher: Property<JavaLauncher>

    @get:InputDirectory
    abstract val toolsPath: DirectoryProperty

    @get:Input
    abstract val quiet: Property<Boolean>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        quiet.convention(false)

        val javaCard = project.extensions.getByType(JavaCardGradleExtension::class.java)

        toolsPath.convention(javaCard.toolsPath)
        toolLauncher.convention(javaCard.toolLauncher)
    }

    /**
     * Invoke a tool.
     *
     * This function automatically builds the classpath with the necessary
     * libraries - the main class to invoke and arguments must still be supplied
     * in the action callback.
     */
    protected fun tool(inspectResult: ((output: String, result: ExecResult) -> Unit)? = null, action: Action<JavaExecSpec>) {
        if (!quiet.get()) {
            logger.lifecycle("Using tools: ${toolsPath.get()}")
        }

        val libDir = toolsPath.get().dir("lib")

        val commonsCli = libDir.asFileTree.filter { it.name.startsWith("commons-cli") }.firstOrNull()
        if (commonsCli == null) {
            throw FileNotFoundException("Missing commons-cli-x.x.x.jar in $libDir")
        }

        val classpath = libDir.files(
            commonsCli,
            "jctasks_tools.jar",
            "json.jar",
            "tools.jar",
        )

        val outputStream = ByteArrayOutputStream()
        val bufferedOutputStream = BufferedOutputStream(outputStream)

        val result = execOperations.javaexec {
            it.executable = toolLauncher.get().executablePath.asFile.absolutePath
            it.classpath = classpath
            it.standardOutput = bufferedOutputStream
            it.errorOutput = bufferedOutputStream
            action.execute(it)
        }

        val output = String(outputStream.toByteArray(), UTF_8).trim()
        inspectResult?.invoke(output, result)

        if (result.exitValue != 0) {
            throw GradleException(output)
        } else if (!quiet.get()) {
            logger.lifecycle(output)
        }
    }

}