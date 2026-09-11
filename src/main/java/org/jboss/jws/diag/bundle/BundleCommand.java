package org.jboss.jws.diag.bundle;

import org.jboss.jws.diag.bundle.manifest.ManifestGenerator;
import org.jboss.jws.diag.bundle.output.ArchiveWriter;
import org.jboss.jws.diag.bundle.validation.ValidationResultsWriter;
import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.common.RedactionLevel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@Command(name = "bundle",
        description = "Generate a redacted .tar.gz support bundle with configs, version manifest, and optional logs",
        mixinStandardHelpOptions = true)
public class BundleCommand implements Runnable {

    @Mixin
    private OutputFormatMixin outputFormat;

    @Option(names = "--catalina-home", description = "Path to CATALINA_HOME (defaults to $CATALINA_HOME env var)")
    private Path catalinaHome;

    @Option(names = "--catalina-base", description = "Path to CATALINA_BASE (defaults to $CATALINA_BASE env var)")
    private Path catalinaBase;

    @Option(names = {"--redaction-level"},
            description = "Redaction level: DEFAULT or STRICT. Default: DEFAULT")
    private RedactionLevel redactionLevel = RedactionLevel.DEFAULT;

    @Option(names = {"--staging-dir"},
            description = "Directory to stage bundle contents before archiving. Defaults to a temp directory.")
    private String stagingDirOption;

    @Option(names = {"--output-dir"},
            description = "Directory to write the final .tar.gz archive. Defaults to the current directory.")
    private String outputDirOption;

    @Override
    public void run() {
        System.exit(execute());
    }

    public int execute() {
        Path resolvedCatalinaBase;
        try {
            resolvedCatalinaBase = resolveCatalinaBase();
        } catch (IllegalStateException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return ExitCodes.TOOL_FAILURE;
        }

        Path resolvedCatalinaHome = resolveCatalinaHome(resolvedCatalinaBase);
        Path resolvedStagingDir;
        boolean stagingDirWasAutoCreated = (stagingDirOption == null);

        try {
            resolvedStagingDir = resolveStagingDir();
            BundleContext context = new BundleContext(
                    resolvedCatalinaBase, resolvedCatalinaHome, resolvedStagingDir, redactionLevel);

            new BundleEngine().run(context);
            String bundleTimestamp = new ManifestGenerator().writeToStagingDir(context);

            new ValidationResultsWriter().write(context);

            Path archivePath = resolveArchivePath(bundleTimestamp);
            new ArchiveWriter().createArchive(resolvedStagingDir, archivePath);

            if (stagingDirWasAutoCreated) {
                deleteRecursively(resolvedStagingDir);
            }

            System.out.println("Bundle created at: " + archivePath);

            int skipped = context.getSkippedFileCount();
            if (skipped > 0) {
                System.err.println("[WARN] Bundle is incomplete: " + skipped
                        + " file(s) could not be collected.");
                return ExitCodes.WARNINGS;
            }
            return ExitCodes.OK;
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to generate bundle: " + e.getMessage());
            return ExitCodes.TOOL_FAILURE;
        }
    }

    private Path resolveArchivePath(String bundleTimestamp) throws IOException {
        String safeTimestamp = bundleTimestamp
                .replace(":", "-")
                .replaceAll("\\.\\d+Z$", "Z");

        Path outputDir = resolveOutputDir();
        return outputDir.resolve("jws-support-bundle-" + safeTimestamp + ".tar.gz");
    }

    private Path resolveOutputDir() throws IOException {
        if (outputDirOption != null) {
            Path dir = Paths.get(outputDirOption);
            Files.createDirectories(dir);
            return dir;
        }
        return Paths.get(".");
    }

    private void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("[WARN] Could not delete temp file: " + path);
                        }
                    });
        }
    }

    private Path resolveCatalinaHome(Path resolvedCatalinaBase) {
        if (catalinaHome != null) {
            return catalinaHome;
        }
        String envValue = System.getenv("CATALINA_HOME");
        if (envValue != null && !envValue.isBlank()) {
            return Paths.get(envValue);
        }
        return resolvedCatalinaBase;
    }

    private Path resolveCatalinaBase() {
        if (catalinaBase != null) {
            return catalinaBase;
        }

        String envValue = System.getenv("CATALINA_BASE");
        if (envValue == null || envValue.isBlank()) {
            throw new IllegalStateException(
                    "Could not determine CATALINA_BASE. Use --catalina-base, "
                            + "or set the CATALINA_BASE environment variable.");
        }

        return Paths.get(envValue);
    }

    Path resolveStagingDir() throws IOException {
        if (stagingDirOption != null) {
            Path dir = Paths.get(stagingDirOption);
            Files.createDirectories(dir);
            return dir;
        }
        return Files.createTempDirectory("jws-diag-bundle-");
    }
}