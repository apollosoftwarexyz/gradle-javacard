import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.3.10"
    id("com.gradle.plugin-publish") version "2.1.0"
    id("org.gradle.plugin-compatibility") version "1.0.0"
}

group = "xyz.apollosoftware.gradle"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    website = "https://github.com/apollosoftwarexyz/gradle-javacard"
    vcsUrl = "https://github.com/apollooftwarexyz/gradle-javacard"

    plugins {
        create("javaCard") {
            id = "xyz.apollosoftware.gradle.javacard"
            implementationClass = "xyz.apollosoftware.gradle.javacard.JavaCardGradlePlugin"
            displayName = "Java Card Gradle Plugin"
            description = "A modern, fast, and highly flexible Gradle plugin for working with the Java Card SDK."
            tags = listOf("javacard", "jcardengine", "sdk", "tools", "emulator")

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "local"
            url = layout.buildDirectory.dir("local-repo").get().asFile.toURI()
        }
    }
}
