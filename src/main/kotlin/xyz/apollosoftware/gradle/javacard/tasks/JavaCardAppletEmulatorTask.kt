package xyz.apollosoftware.gradle.javacard.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import xyz.apollosoftware.gradle.javacard.tasks.base.JavaCardAppletEmulatorToolTask
import java.io.FileNotFoundException

/**
 * Task that runs [JCardEngine](https://github.com/martinpaljak/JCardEngine).
 *
 * See [xyz.apollosoftware.gradle.javacard.tasks.base.JavaCardAppletEmulatorToolTask]
 */
@DisableCachingByDefault(because = "The emulator should always be executed.")
abstract class JavaCardAppletEmulatorTask: JavaCardAppletEmulatorToolTask() {

    companion object {

        /**
         * The default port number for VICC (Virtual Integrated Circuit Card)
         * which is documented as 0x8C7B (or 35963, in decimal).
         *
         * See the [vsmartcard/virtualsmartcard documentation](https://frankmorgner.github.io/vsmartcard/virtualsmartcard/api.html).
         */
        const val DEFAULT_VICC_PORT = 0x8C7B

    }

    /**
     * The directory that contains (or will contain by the time the task is
     * invoked) the CAP file to run in the simulator.
     */
    @get:InputDirectory
    abstract val capFileDir: DirectoryProperty

    /**
     * Whether to enable interactive (control) mode in the emulator.
     */
    @get:Input
    abstract val interactive: Property<Boolean>

    /**
     * The simulated card's Answer-to-Reset (ATR) bytes.
     */
    @get:Input
    abstract val atr: Property<String>

    /**
     * The hex-encoded installation parameters to pass to the applet.
     */
    @get:Input
    abstract val installationParameters: Property<String>

    /**
     * The hostname of the virtual reader to connect to.
     */
    @get:Input
    abstract val host: Property<String>

    /**
     * The port of the virtual reader to connect to.
     */
    @get:Input
    abstract val port: Property<Int>

    /**
     * The simulated protocol.
     */
    @get:Input
    abstract val protocol: Property<String>

    init {
        interactive.convention(false)
        atr.convention("3B80800101")
        installationParameters.convention("")
        host.convention("127.0.0.1")
        port.convention(DEFAULT_VICC_PORT)
        protocol.convention("*")
    }

    @TaskAction
    fun executeTask() {
        val props = mapOf(
            Pair("--atr", atr.get()),
            Pair("--params", installationParameters.get()),
            Pair("--vsmartcard-atr", atr.get()),
            Pair("--vsmartcard-host", host.get()),
            Pair("--vsmartcard-port", port.map { if (it in 1..65535) it.toString() else null }.getOrElse("")),
            Pair("--vsmartcard-protocol", protocol.get()),
        ).filterValues { it.isNotBlank() }

        val capFiles = capFileDir.asFileTree.filter {
                file -> file.extension.equals(JavaCardAppletVerifyTask.Companion.CAP_FILE_EXTENSION, ignoreCase = true)
        }.toList()

        if (capFiles.isEmpty()) {
            throw FileNotFoundException("Failed to locate applet CAP file.")
        }

        if (capFiles.size > 1) {
            throw UnsupportedOperationException("Found multiple CAP files in built output.")
        }

        tool {
            it.args = listOfNotNull(
                capFiles.single().absolutePath,
                if (interactive.get()) "--control" else null,
                "--vsmartcard",
                *(props.entries.flatMap { e -> listOf(e.key, e.value) }.toTypedArray())
            )
        }
    }

}