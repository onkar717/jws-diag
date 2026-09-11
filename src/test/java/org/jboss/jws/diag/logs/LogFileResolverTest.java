package org.jboss.jws.diag.logs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LogFileResolverTest {

    @TempDir
    Path base;

    @Test
    void catalinaOutUnset_usesLogsCatalinaOut() throws IOException {
        Path expected = writeLog("logs/catalina.out");

        LogFileResolver.Resolution resolution = LogFileResolver.resolve(base, null);

        assertThat(resolution.isFound()).isTrue();
        assertThat(resolution.getLogFile()).isEqualTo(expected);
    }

    @Test
    void catalinaOutBlank_treatedAsUnset() throws IOException {
        Path expected = writeLog("logs/catalina.out");

        assertThat(LogFileResolver.resolve(base, "  ").getLogFile()).isEqualTo(expected);
    }

    @Test
    void catalinaOutAbsolute_usesConfiguredFile(@TempDir Path elsewhere) throws IOException {
        Path configured = elsewhere.resolve("tomcat-stdout.log");
        Files.writeString(configured, "INFO line\n");
        writeLog("logs/catalina.out");

        LogFileResolver.Resolution resolution = LogFileResolver.resolve(base, configured.toString());

        assertThat(resolution.getLogFile()).isEqualTo(configured);
    }

    @Test
    void catalinaOutRelative_resolvedAgainstCatalinaBase() throws IOException {
        Path expected = writeLog("custom/out.log");

        assertThat(LogFileResolver.resolve(base, "custom/out.log").getLogFile()).isEqualTo(expected);
    }

    @Test
    void catalinaOutNotARegularFile_isSkippedRatherThanRead() throws IOException {
        // Stands in for /dev/stdout: present, but not a file that can be scanned.
        Path stream = Files.createDirectories(base.resolve("stream"));
        writeLog("logs/catalina.out");

        LogFileResolver.Resolution resolution = LogFileResolver.resolve(base, stream.toString());

        assertThat(resolution.isFound()).isFalse();
        assertThat(resolution.getSkipReason()).contains("not a regular file");
    }

    @Test
    void catalinaOutMissing_isSkippedWithoutFallingBackToStaleCatalinaOut() throws IOException {
        writeLog("logs/catalina.out");

        LogFileResolver.Resolution resolution =
                LogFileResolver.resolve(base, base.resolve("gone.log").toString());

        assertThat(resolution.isFound()).isFalse();
        assertThat(resolution.getSkipReason()).contains("does not exist");
    }

    @Test
    void noCatalinaOutFile_fallsBackToNewestDatedLog() throws IOException {
        writeLog("logs/catalina.2026-09-09.log");
        Path newest = writeLog("logs/catalina.2026-09-10.log");
        writeLog("logs/localhost.2026-09-10.log");

        assertThat(LogFileResolver.resolve(base, null).getLogFile()).isEqualTo(newest);
    }

    @Test
    void catalinaOutFile_preferredOverDatedLogs() throws IOException {
        Path catalinaOut = writeLog("logs/catalina.out");
        writeLog("logs/catalina.2026-09-10.log");

        assertThat(LogFileResolver.resolve(base, null).getLogFile()).isEqualTo(catalinaOut);
    }

    @Test
    void emptyLogsDirectory_isSkippedWithReason() throws IOException {
        Files.createDirectories(base.resolve("logs"));

        LogFileResolver.Resolution resolution = LogFileResolver.resolve(base, null);

        assertThat(resolution.isFound()).isFalse();
        assertThat(resolution.getSkipReason()).contains("no catalina.out");
    }

    @Test
    void missingLogsDirectory_isSkipped() {
        assertThat(LogFileResolver.resolve(base, null).isFound()).isFalse();
    }

    private Path writeLog(String relative) throws IOException {
        Path file = base.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "INFO line\n");
        return file;
    }
}
