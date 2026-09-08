package org.jboss.jws.diag.bundle;

import org.jboss.jws.diag.common.ExitCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class BundleCommandTest {

    private void writeConfFile(Path catalinaBase, String fileName, String content) throws IOException {
        Path confDir = catalinaBase.resolve("conf");
        Files.createDirectories(confDir);
        Files.writeString(confDir.resolve(fileName), content, StandardCharsets.UTF_8);
    }

    @Test
    void shouldReturnOkExitCodeWhenBundleIsGeneratedSuccessfully(
            @TempDir Path catalinaBase, @TempDir Path stagingDir) throws IOException {
        writeConfFile(catalinaBase, "server.xml", "<Server/>");

        BundleCommand command = new BundleCommand();
        new CommandLine(command).parseArgs(
                "--catalina-base", catalinaBase.toString(),
                "--staging-dir", stagingDir.toString(),
                "--output-dir", stagingDir.toString());

        int exitCode = command.execute();

        try (Stream<Path> files = Files.list(stagingDir)) {
            assertThat(files)
                    .anyMatch(path -> path.getFileName().toString().endsWith(".tar.gz"));
        }

        assertThat(exitCode).isEqualTo(ExitCodes.OK);
    }

    @Test
    void shouldReturnToolFailureExitCodeWhenCatalinaBaseNotSet() {
        BundleCommand command = new BundleCommand();
        new CommandLine(command).parseArgs();

        int exitCode = command.execute();

        assertThat(exitCode).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void shouldStageFilesAtSpecifiedStagingDir(@TempDir Path catalinaBase, @TempDir Path stagingDir) throws IOException {
        writeConfFile(catalinaBase, "server.xml", "<Server password=\"secret\"/>");

        BundleCommand command = new BundleCommand();
        new CommandLine(command).parseArgs(
                "--catalina-base", catalinaBase.toString(),
                "--staging-dir", stagingDir.toString(),
                "--output-dir", stagingDir.toString());

        command.execute();

        assertThat(stagingDir.resolve("conf/server.xml")).exists();
    }

    @Test
    void shouldFallBackToTempDirectoryWhenStagingDirNotSpecified() throws IOException {
        BundleCommand command = new BundleCommand();
        new CommandLine(command).parseArgs();

        Path created = command.resolveStagingDir();
        try {
            assertThat(created).exists();
            assertThat(created.getFileName().toString()).startsWith("jws-diag-bundle-");
        } finally {
            Files.deleteIfExists(created);
        }
    }
}