package xyz.apollosoftware.gradle.javacard.dsl;

import xyz.apollosoftware.gradle.javacard.JavaCardAPIVersion;
import xyz.apollosoftware.gradle.javacard.JavaCardGradlePlugin;
import org.gradle.api.GradleException;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class JavaCardGradleExtensionTest {

    @Test
    void testLoads() {
        final var project = ProjectBuilder.builder().build();

        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(JavaCardGradlePlugin.class);

        // Assert that the extension has been registered with the name
        // 'javaCard'.
        final var extension = project.getExtensions().findByType(JavaCardGradleExtension.class);
        assertNotNull(extension);
        assertEquals(extension, project.getExtensions().findByName("javaCard"));
    }

    @Test
    void testRequiresJavaPlugin() {
        final var project = ProjectBuilder.builder().build();

        final var ex = assertThrows(GradleException.class, () -> project.getPluginManager().apply(JavaCardGradlePlugin.class));
        assertEquals("`xyz.apollosoftware.gradle.javacard` requires the java plugin to be applied.", ex.getCause().getMessage());
    }

    @Test
    void testRequiresAPIVersion() {
        final var project = ProjectBuilder.builder().build();

        final var extension = project.getExtensions().create(JavaCardGradleExtension.NAME, JavaCardGradleExtension.class);

        final var ex = assertThrows(GradleException.class, extension::getValidApiVersion);
        assertEquals(
            """
            Java Card API Version is not set. Please set the apiVersion in the javaCard { } block in your configuration, like so:

                import xyz.apollosoftware.gradle.javacard.JavaCardAPIVersion

                /* ... */

                javaCard {
                    apiVersion = JavaCardAPIVersion.of("3.0.4")

                    /* ... */
                }
            """.stripTrailing(),
            ex.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
    })
    void testAcceptsValidAidPattern(final String aid) {
        final var project = ProjectBuilder.builder().build();

        final var extension = project.getExtensions().create(JavaCardGradleExtension.NAME, JavaCardGradleExtension.class);
        extension.getApiVersion().set(JavaCardAPIVersion.VERSION_304);
        extension.getAid().set(aid);

        assertDoesNotThrow(extension::getValidAid);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0x00:0x00:0x00:0x00",
        "0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00:0x00",
    })
    void testRejectsInvalidAidPattern(final String aid) {
        final var project = ProjectBuilder.builder().build();

        final var extension = project.getExtensions().create(JavaCardGradleExtension.NAME, JavaCardGradleExtension.class);
        extension.getApiVersion().set(JavaCardAPIVersion.VERSION_304);
        extension.getAid().set(aid);

        final var ex = assertThrowsExactly(IllegalArgumentException.class, extension::getValidAid);
        assertEquals("Invalid Java Card Application ID (AID): `%s` (the AID must be 5 to 16 bytes where each byte is represented as hex: 0x00-0xFF)".formatted(aid), ex.getMessage());
    }

}
