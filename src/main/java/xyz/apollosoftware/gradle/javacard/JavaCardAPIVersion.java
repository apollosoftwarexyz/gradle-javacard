package xyz.apollosoftware.gradle.javacard;

// This file is written in Java for compatibility reasons.

/**
 * The Java Card API version.
 *
 * <p>
 * This refers specifically to the classic edition (as opposed to the connected
 * edition which is so rarely deployed that it is often implicitly excluded).
 */
public enum JavaCardAPIVersion {

    /**
     * Java Card 3.0.4 Platform Specification
     *
     * <p>
     * <a href="https://www.oracle.com/java/technologies/java-card/platform-specification-v304.html">Java Card 3.0.4 Specification, Classic Edition</a>
     */
    VERSION_304("3.0.4"),

    /**
     * Java Card 3.0.5 Platform Specification
     *
     * <p>
     * <a href="https://docs.oracle.com/en/java/javacard/3.0.5/jcrn/jcrn.pdf">Java Card 3.0.5 Specification, Classic Edition</a>
     */
    VERSION_305("3.0.5"),

    /**
     * Java Card 3.1.0 Platform Specification
     *
     * <p>
     * <a href="https://docs.oracle.com/en/java/javacard/3.1/specnotes/index.html">Java Card 3.1.0 Specification, Classic Edition</a>
     */
    VERSION_310("3.1.0"),

    /**
     * Java Card 3.2.0 Platform Specification
     *
     * <p>
     * <a href="https://docs.oracle.com/en/java/javacard/3.2/specnotes/index.html">Java Card 3.2.0 Specification, Classic Edition</a>
     */
    VERSION_320("3.2.0");

    private final String version;

    JavaCardAPIVersion(String version) {
        this.version = version;
    }

    /**
     * Get the canonical string representation of the version.
     *
     * <p>
     * For example, for {@link #VERSION_304}, this function returns "3.0.4".
     *
     * @return the version string.
     */
    public String version() {
        return version;
    }

    /**
     * Attempt to resolve a known {@link JavaCardAPIVersion} given a version
     * string.
     *
     * @param version the API version to resolve.
     * @return the resolved Java Card API version.
     */
    public static JavaCardAPIVersion of(final String version) {
        for (final var value : JavaCardAPIVersion.values()) {
            if (value.version.equals(version)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unrecognized or unsupported Java Card API version: %s".formatted(version));
    }

}
