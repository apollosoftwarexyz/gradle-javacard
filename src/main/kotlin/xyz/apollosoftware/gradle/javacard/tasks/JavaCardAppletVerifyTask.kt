package xyz.apollosoftware.gradle.javacard.tasks

import xyz.apollosoftware.gradle.javacard.JavaCardAPIVersion
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension
import xyz.apollosoftware.gradle.javacard.tasks.base.JavaCardAppletToolTask

/**
 * Task that can verify a Java Card applet CAP file conforms to the Java Card
 * Platform Specification.
 */
abstract class JavaCardAppletVerifyTask: JavaCardAppletToolTask() {

    companion object {
        /**
         * The file extension (to be used with case-insensitive comparison) for
         * export (EXP) files (.exp).
         */
        const val EXPORT_FILE_EXTENSION = "EXP"

        /**
         * The file extension (to be used with case-insensitive comparison) for
         * converted applet (CAP) files (.cap).
         */
        const val CAP_FILE_EXTENSION = "CAP"
    }

    /**
     * The Java Card API version to verify against.
     */
    @get:Input
    abstract val apiVersion: Property<JavaCardAPIVersion>

    /**
     * The location of the applet CAP and EXP files to verify.
     *
     * This location should contain the top-level folder of the package (e.g.,
     * if the package was `foo.bar.baz`, this path should be the path to the
     * directory that contains `foo`).
     */
    @get:InputDirectory
    abstract val appletDir: DirectoryProperty

    /**
     * The location to store the generated verification report (gathered from
     * the output of the CAP and EXP verification tools).
     */
    @get:OutputFile
    abstract val reportOutput: RegularFileProperty

    init {
        val javaCard = project.extensions.getByType(JavaCardGradleExtension::class.java)
        apiVersion.convention(javaCard.apiVersion)
    }

    @TaskAction
    fun executeTask() {
        // Get the list of export files.
        val exportFiles = appletDir.get().asFileTree
            .filter { it.extension.equals(EXPORT_FILE_EXTENSION, ignoreCase = true) }
            .map { it.absolutePath }
            .toTypedArray()

        exportFiles.forEach { exportFile ->
            logger.lifecycle("Verifying $EXPORT_FILE_EXTENSION file: $exportFile")

            tool(this::inspectValidatorResult) {
                it.mainClass.set("com.sun.javacard.offcardverifier.VerifyExp")
                it.args = listOf("-nobanner", exportFile)
            }
        }

        appletDir.get().asFileTree
            .filter { it.extension.equals(CAP_FILE_EXTENSION, ignoreCase = true) }
            .map { it.absolutePath }
            .forEach { capFile ->
                // Verify the CAP file.
                logger.lifecycle("Verifying $CAP_FILE_EXTENSION file: $capFile")

                tool(this::inspectValidatorResult) {
                    it.mainClass.set("com.sun.javacard.offcardverifier.Verifier")
                    it.args = listOf(
                        "-nobanner",
                        "-target",
                        apiVersion.get().version(),
                        *exportFiles,
                        capFile,
                    )
                }
            }
    }

    private fun inspectValidatorResult(output: String, result: ExecResult) {
        reportOutput.get().asFile.appendText(output + System.lineSeparator())

        // Reliant, obviously, on the format of the log messages not changing.
        // A better approach is probably to create a custom LogFilter and print
        // something in JSON that can be handled here (or just exit with an
        // error code).
        // See: https://docs.oracle.com/en/java/javacard/3.2/jctug/configure-logging-java-card-development-kit-tools.html#GUID-E7C2FB1A-4AD4-4469-8877-8DCEDCF4B06F
        if (!output.contains("Verification completed with 0 warnings and 0 errors.")) {
            throw GradleException("CAP file verification failed:\n$output")
        }
    }

}