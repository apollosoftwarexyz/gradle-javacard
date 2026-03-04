# Java Card Gradle Plugin

A modern, fast, and highly flexible Gradle plugin for working with the Java Card
SDK.

## Getting Started

Ensure you have `java` and the `xyz.apollosoftware.gradle.javacard` plugins
active:

```groovy
plugins {
    id 'java'
    id 'xyz.apollosoftware.gradle.javacard' version '1.0'
}
```

Add a configuration block to your project. At a minimum, you must specify the
version of the Java Card API to use, the Application ID (AID), package name and
applet definition.

```groovy
import xyz.apollosoftware.gradle.javacard.JavaCardAPIVersion

// ...

javaCard {
    apiVersion = JavaCardAPIVersion.of("3.0.4")

    packageName = "xyz.apollosoftware.javacard.helloworld"
    aid = "0x00:0x00:0x00:0x00:0x00"
    
    applets {
        "0x01" {
            appletClass = "HelloWorldApplet"
        }
    }
}
```

Optionally, you can specify the location to the Java Card SDK tools. If you do
not explicitly set it, the tools will be resolved to either the value of the
`xyz.apollosoftware.gradle.javacard.tools-path` system property, or to the
`JC_HOME_TOOLS` environment variable (in that order).

You can set the system property by adding the following to
`~/.gradle/gradle.properties`:

```properties
systemProp.xyz.apollosoftware.gradle.javacard.tools-path=/path/to/tools
```

## Usage

Once your project is configured, do the following to build your Java Card
applet:

```bash
./gradlew applet
```

To run your built applet in an emulator, use the following:

```bash
./gradlew emulateApplet
```

The `emulateApplet` task assumes that you have [vsmartcard](https://frankmorgner.github.io/vsmartcard/index.html)
already set up on your machine. Provided this is the case, the `emulateApplet`
task is defined with sensible defaults that should enable it to be
plug-and-play.

The task triggers a build for the applet (if it is not already built) and, if a
specific location for JCardEngine is not specified, will fetch and run
[JCardEngine](https://github.com/martinpaljak/JCardEngine) automatically (with
the vsmartcard options).

## Advanced Usage

All plugin Gradle tasks are pre-configured with sensible defaults to allow for
a simple plug-and-play configuration.

They are all, however, also designed to be independently composable and
configurable with custom Gradle configuration such that it should be possible to
leverage Gradle and this plugin to satisfy even highly bespoke and complex
configurations.

To see how the tasks are wired together by default, see the [JavaCardGradlePlugin](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/JavaCardGradlePlugin.kt)
plugin entrypoint.

Each task takes its default (convention) configuration from either the
[JavaCardGradleExtension](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/dsl/JavaCardGradleExtension.kt),
which defines the `javaCard` configuration block, or from the [JavaCardGradleEmulatorExtension](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/dsl/JavaCardGradleEmulatorExtension.kt)
which defines the `javaCardEmulator` configuration block.

Tasks that use the Java Card SDK tools inherit some default configuration and
behavior from the [JavaCardAppletToolTask](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/tasks/base/JavaCardAppletToolTask.kt)
base class. Similarly, tasks that use the JCardEngine emulator inherit from
[JavaCardAppletEmulatorToolTask](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/tasks/base/JavaCardAppletEmulatorToolTask.kt).

Finally, each task is defined in the following locations:

**applet** tasks

- `appletConfig`: [JavaCardAppletConfigTask](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/tasks/JavaCardAppletConfigTask.kt)
  - *(builds the configuration for the Java Card SDK converter tool)*
- `applet`: [JavaCardAppletTask](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/tasks/JavaCardAppletTask.kt)
  - *(builds the Java Card applet with the Java Card SDK converter tool)*
- `verifyApplet`: [JavaCardAppletVerifyTask](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/tasks/JavaCardAppletVerifyTask.kt)
  - *(verifies the Java Card applet with the Java Card SDK offline verifier tool)*

**applet emulator** tasks

- `fetchJCardEngine`: [JavaCardAppletFetchJCardEngineTask](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/tasks/JavaCardAppletVerifyTask.kt)
  - *(fetches JCardEngine and places it in a cache directory for other tools - used only if JCardEngine is not explicitly specified)*
- `emulateApplet`: [JavaCardAppletEmulatorTask](./src/main/kotlin/xyz/apollosoftware/gradle/javacard/tasks/JavaCardAppletEmulatorTask.kt)
    - *(runs a built Java Card applet in the JCardEngine emulator)*

### Multiple Source Sets

For simplicity in projects that only build a Java Card applet, the Java Card
Gradle Plugin uses the default (`main`) source set.

The source set used for the Java Card applet is automatically configured with
Java source and target version 7, JDK 17 and appropriate compiler options.

For more advanced projects, where multiple source sets with different
configuration are desired, the source set can be changed by setting the
`sourceSet` property. This is useful if, for example, you want to create a
source set for a Java Card applet and a source set for the client that
communicates with the applet.

To do this, first define a new source set for the applet code, in the standard
Gradle manner (where `applet` is the name of your source set):

```groovy
sourceSets {
    applet {
        java {
        }
    }
}
```

...then configure the plugin to use it instead:

```groovy
javaCard {
    
    // ...
    
    sourceSet = sourceSets.applet
    
    // ...
    
}
```

Note that only the source set configured for the Java Card Gradle plugin is used
for applet classes and all other source sets will use the default Java compiler
toolchain and options (such as flags, version, etc.)
