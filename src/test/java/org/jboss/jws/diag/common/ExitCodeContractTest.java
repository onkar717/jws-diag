package org.jboss.jws.diag.common;

import org.jboss.jws.diag.instances.InstancesCommand;
import org.jboss.jws.diag.modcluster.ModClusterCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the CLI-wide exit code contract: 0 to 2 describe what was found, 3 means the
 * tool could not run, and the absence of optional configuration is reported as 0.
 */
class ExitCodeContractTest {

    private static final String SERVER_XML_NO_MODCLUSTER =
            "<Server><Service name=\"Catalina\"><Engine name=\"Catalina\" defaultHost=\"localhost\">"
                    + "<Host name=\"localhost\"/></Engine></Service></Server>";

    @Test
    void codesAreDistinctAndOrderedBySeverity() {
        assertThat(ExitCodes.OK).isEqualTo(0);
        assertThat(ExitCodes.WARNINGS).isEqualTo(1);
        assertThat(ExitCodes.ERRORS).isEqualTo(2);
        assertThat(ExitCodes.TOOL_FAILURE).isEqualTo(3);
    }

    @Test
    void modcluster_whenNoModClusterConfigured_returnsOk(@TempDir Path catalinaBase) throws IOException {
        writeServerXml(catalinaBase, SERVER_XML_NO_MODCLUSTER);

        ModClusterCommand command = new ModClusterCommand();
        new CommandLine(command).parseArgs("--catalina-base", catalinaBase.toString());

        assertThat(command.execute()).isEqualTo(ExitCodes.OK);
    }

    @Test
    void modcluster_whenServerXmlMissing_returnsToolFailure(@TempDir Path catalinaBase) {
        ModClusterCommand command = new ModClusterCommand();
        new CommandLine(command).parseArgs("--catalina-base", catalinaBase.toString());

        assertThat(command.execute()).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void modcluster_whenCatalinaBaseIsNotADirectory_returnsToolFailure(@TempDir Path parent) throws IOException {
        Path notADirectory = parent.resolve("server.xml");
        Files.writeString(notADirectory, "<Server/>", StandardCharsets.UTF_8);

        ModClusterCommand command = new ModClusterCommand();
        new CommandLine(command).parseArgs("--catalina-base", notADirectory.toString());

        assertThat(command.execute()).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void modcluster_whenServerXmlIsMalformed_returnsToolFailure(@TempDir Path catalinaBase) throws IOException {
        writeServerXml(catalinaBase, "<Server><unclosed>");

        ModClusterCommand command = new ModClusterCommand();
        new CommandLine(command).parseArgs("--catalina-base", catalinaBase.toString());

        assertThat(command.execute()).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void instances_whenNoneRunning_returnsOk() {
        InstancesCommand command = new InstancesCommand();
        new CommandLine(command).parseArgs();

        // Whether this host runs Tomcat or not, "none found" must never be a failure.
        assertThat(command.execute()).isEqualTo(ExitCodes.OK);
    }

    private static void writeServerXml(Path catalinaBase, String content) throws IOException {
        Path confDir = catalinaBase.resolve("conf");
        Files.createDirectories(confDir);
        Files.writeString(confDir.resolve("server.xml"), content, StandardCharsets.UTF_8);
    }
}
