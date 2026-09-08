package org.jboss.jws.diag.bundle;

import org.jboss.jws.diag.common.RedactionLevel;

import java.nio.file.Path;
import java.util.Objects;

public final class BundleContext {

    private final Path catalinaBase;
    private final Path catalinaHome;
    private final Path stagingDir;
    private final RedactionLevel redactionLevel;
    private int skippedFileCount;

    public BundleContext(Path catalinaBase, Path catalinaHome, Path stagingDir, RedactionLevel redactionLevel) {
        this.catalinaBase = Objects.requireNonNull(catalinaBase, "catalinaBase");
        this.catalinaHome =  Objects.requireNonNull(catalinaHome, "catalinaHome");
        this.stagingDir = Objects.requireNonNull(stagingDir, "stagingDir");
        this.redactionLevel = Objects.requireNonNull(redactionLevel, "redactionLevel");
    }

    public Path getCatalinaBase() {
        return catalinaBase;
    }

    public Path getCatalinaHome() {
        return catalinaHome;
    }

    public Path getStagingDir() {
        return stagingDir;
    }

    public RedactionLevel getRedactionLevel() {
        return redactionLevel;
    }

    /**
     * Records that one file which was present could not be collected, so the bundle
     * is usable but incomplete. A file that simply does not exist is not a skip;
     * there was nothing to collect. Callers report the reason on stderr.
     */
    public void recordSkippedFile() {
        skippedFileCount++;
    }

    /**
     * Number of files left out of the bundle. Greater than zero means the bundle was
     * written but is missing content, which the command reports as a warning.
     */
    public int getSkippedFileCount() {
        return skippedFileCount;
    }
}