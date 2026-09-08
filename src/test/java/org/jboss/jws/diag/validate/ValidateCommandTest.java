package org.jboss.jws.diag.validate;

import org.jboss.jws.diag.common.ExitCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidateCommandTest {

    private void writeConfFile(Path catalinaBase, String fileName, String content) throws IOException {
        Path confDir = catalinaBase.resolve("conf");
        Files.createDirectories(confDir);
        Files.writeString(confDir.resolve(fileName), content, StandardCharsets.UTF_8);
    }

    @Test
    void shouldExecuteValidationAgainstCatalinaBase(@TempDir Path catalinaBase) throws IOException {
        writeConfFile(catalinaBase, "server.xml",
                "<Server port=\"-1\" shutdown=\"SHUTDOWN\"><Service name=\"Catalina\"/></Server>");

        ValidateCommand command = new ValidateCommand();
        new CommandLine(command).parseArgs("--catalina-base", catalinaBase.toString());

        int exitCode = command.execute();

        assertThat(exitCode).isIn(ExitCodes.OK, ExitCodes.WARNINGS, ExitCodes.ERRORS);
    }

    @Test
    @org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable(named = "CATALINA_BASE", matches = ".+")
    void shouldReturnToolFailureExitCodeWhenCatalinaBaseNotSet() {
        ValidateCommand command = new ValidateCommand();
        new CommandLine(command).parseArgs();

        int exitCode = command.execute();

        assertThat(exitCode).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void shouldRunSuccessfullyWithJsonOutputFormat(@TempDir Path catalinaBase) throws IOException {
        writeConfFile(catalinaBase, "server.xml", "<Server port=\"-1\" shutdown=\"SHUTDOWN\"/>");

        ValidateCommand command = new ValidateCommand();
        new CommandLine(command).parseArgs(
                "--catalina-base", catalinaBase.toString(),
                "--format", "JSON");

        int exitCode = command.execute();

        assertThat(exitCode).isIn(ExitCodes.OK, ExitCodes.WARNINGS, ExitCodes.ERRORS);
    }
}