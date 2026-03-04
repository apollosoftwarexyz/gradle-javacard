package xyz.apollosoftware.gradle.javacard

import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleEmulatorExtension
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension.Companion.TOOLS_PATH_ENV_VAR
import xyz.apollosoftware.gradle.javacard.dsl.JavaCardGradleExtension.Companion.TOOLS_PATH_PROPERTY
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import xyz.apollosoftware.gradle.javacard.tasks.JavaCardAppletConfigTask
import xyz.apollosoftware.gradle.javacard.tasks.JavaCardAppletEmulatorTask
import xyz.apollosoftware.gradle.javacard.tasks.JavaCardAppletFetchJCardEngineTask
import xyz.apollosoftware.gradle.javacard.tasks.JavaCardAppletTask
import xyz.apollosoftware.gradle.javacard.tasks.JavaCardAppletVerifyTask

/**
 * The main plugin entrypoint for the Java Card Gradle plugin.
 */
abstract class JavaCardGradlePlugin: Plugin<Project> {

    companion object {
        /**
         * The ID of the plugin (same as the package name).
         */
        val PLUGIN_ID: String = JavaCardGradlePlugin::class.java.`package`.name

        /**
         * The ID for the group of applet-related tasks.
         */
        const val APPLET_TASK_GROUP_ID: String = "applet"

        /**
         * The ID for the group of applet emulator-related tasks.
         */
        const val APPLET_EMULATOR_TASK_GROUP_ID: String = "applet emulator"
    }

    override fun apply(project: Project) {
        if (!project.pluginManager.hasPlugin("java")) {
            throw GradleException("`%s` requires the java plugin to be applied.".format(PLUGIN_ID))
        }

        val javaToolchainService = project.serviceOf<JavaToolchainService>()

        val javaCardExtension = project.extensions.create(JavaCardGradleExtension.NAME, JavaCardGradleExtension::class.java).apply {
            // Provide defaults for the compiler and toolLauncher.
            //
            // By default, use JDK 17 which is the recommended compiler (later
            // versions also removed support for compiling to Java Language
            // version 7).
            compiler.convention(javaToolchainService.compilerFor {
                it.languageVersion.set(JavaLanguageVersion.of(17))
            })
            toolLauncher.convention(javaToolchainService.launcherFor {
                it.languageVersion.set(JavaLanguageVersion.of(17))
            })

            // Provide the default source set.
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceSet.convention(sourceSets.named("main"))

            // Resolve the `tools` path.
            val fallbackToolsPath = project.providers.systemProperty(TOOLS_PATH_PROPERTY).orElse(project.provider {
                System.getenv(TOOLS_PATH_ENV_VAR)
                    ?: throw GradleException("Failed to resolve path to Java Card SDK Tools (`JC_HOME_TOOLS`). Please set either the `JC_HOME_TOOLS` environment variable, or the `xyz.apollosoftware.gradle.javacard.tools-path` Gradle property.")
            })
            toolsPath.convention(project.layout.projectDirectory.dir(fallbackToolsPath))
        }

        project.extensions.create(JavaCardGradleEmulatorExtension.NAME, JavaCardGradleEmulatorExtension::class.java)

        project.afterEvaluate {
            project.tasks.named(
                javaCardExtension.sourceSet.get().compileJavaTaskName,
                JavaCompile::class.java
            ) { compileTask ->
                compileTask.javaCompiler.set(javaCardExtension.compiler)

                // Per https://docs.oracle.com/en/java/javacard/3.2/jctug/setting-java-compiler-options.html
                // it is necessary to set the Java Language version to 7 for class
                // version 51.
                compileTask.sourceCompatibility = "1.7"
                compileTask.targetCompatibility = "1.7"

                // Suppress warnings about the source and target compatibility.
                compileTask.options.compilerArgs.add("-Xlint:-options")

                // Per the above, generate the LocalVariableTable in the class file.
                compileTask.options.compilerArgs.add("-g")

                // Validate other compiler options.
                compileTask.doFirst {
                    if (compileTask.options.compilerArgs.contains("-O")) {
                        throw IllegalArgumentException("Do not compile with the -O option. This removes metadata required by the Java Card SDK Converter and optimizes for speed rather than memory.")
                    }
                }
            }
        }

        val appletClassesDir = javaCardExtension.sourceSet.map { it.java.classesDirectory.get() }
        val appletDestinationLocation = project.layout.buildDirectory.dir("applet")
        val appletConfigLocation = project.layout.buildDirectory.file("applet.conf")
        val appletVerificationReportLocation = project.layout.buildDirectory.file("applet_verification_report.txt")

        val appletConfigTask = project.tasks.register("appletConfig", JavaCardAppletConfigTask::class.java).apply {
            configure {
                it.group = APPLET_TASK_GROUP_ID
                it.description = "Build the configuration for the Java Card SDK converter tool"
            }
        }

        val appletTask = project.tasks.register("applet", JavaCardAppletTask::class.java)
        val verifyAppletTask = project.tasks.register("verifyApplet", JavaCardAppletVerifyTask::class.java)

        appletTask.configure  {
            it.group = APPLET_TASK_GROUP_ID
            it.description = "Build the Java Card applet with the Java Card SDK converter tool"
            it.appletConfigLocation.convention(appletConfigLocation)
            it.appletDestinationLocation.convention(appletDestinationLocation)
            it.inputFiles.convention(javaCardExtension.sourceSet.map { sources -> sources.allSource.sourceDirectories })
            it.dependsOn(appletConfigTask, project.tasks.withType(JavaCompile::class.java))
            it.finalizedBy(verifyAppletTask)
        }

        verifyAppletTask.configure {
            it.group = APPLET_TASK_GROUP_ID
            it.description = "Verify the built Java Card applet with the Java Card SDK offline verifier tool"
            it.appletDir.convention(appletDestinationLocation)
            it.reportOutput.convention(appletVerificationReportLocation)
            it.dependsOn(appletTask)
        }

        project.afterEvaluate { project ->
            project.configurations.named(javaCardExtension.sourceSet.get().annotationProcessorConfigurationName).configure {
                // Add the Java Card SDK annotations.
                val version = javaCardExtension.getValidApiVersion().version()
                it.dependencies.add(project.dependencies.create(javaCardExtension.toolsPath.files("lib/api_classic_annotations-${version}.jar")))
            }

            project.configurations.named(javaCardExtension.sourceSet.get().compileOnlyConfigurationName).configure {
                // Add the Java Card SDK dependencies.
                val version = javaCardExtension.getValidApiVersion().version()
                it.dependencies.add(project.dependencies.create(javaCardExtension.toolsPath.files("lib/api_classic-${version}.jar")))
                it.dependencies.add(project.dependencies.create(javaCardExtension.toolsPath.files("lib/tools.jar")))
            }

            appletConfigTask.configure {
                it.classesDir.convention(appletClassesDir.get().asFile.absolutePath)
                it.appletDestinationDir.convention(appletDestinationLocation.get().asFile.absolutePath)
                it.appletConfigLocation.convention(appletConfigLocation)
            }
        }

        val fetchJCardEngineTask = project.tasks.register("fetchJCardEngine", JavaCardAppletFetchJCardEngineTask::class.java).apply {
            configure {
                it.group = APPLET_EMULATOR_TASK_GROUP_ID
                it.description = "Fetch JCardEngine and place it in the cache directory directory so it is available for other tasks."
            }
        }

        val emulateAppletTask = project.tasks.register("emulateApplet", JavaCardAppletEmulatorTask::class.java).apply {
            configure {
                it.group = APPLET_EMULATOR_TASK_GROUP_ID
                it.description = "Run a built Java Card applet in the JCardEngine emulator."
                it.dependsOn(appletTask)
                it.capFileDir.set(appletTask.flatMap { task -> task.appletDestinationLocation })
            }
        }

        // If the emulateAppletTask is not configured with the path to the
        // JCardEngine emulator, use the fetchJCardEngineTask to fetch it.
        project.afterEvaluate { _ ->
            emulateAppletTask.configure {
                if (!it.emulatorClasspath.isPresent) {
                    val fetchJCardEngineTask = fetchJCardEngineTask.get()
                    it.dependsOn(fetchJCardEngineTask)
                    it.emulatorClasspath.convention(fetchJCardEngineTask.jarDestinationFolder)
                }
            }
        }
    }

}
