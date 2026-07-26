package org.jboss.jws.diag.config;

import org.jboss.jws.diag.config.formatter.ConfigHumanFormatter;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.parser.PropertyResolver;
import org.jboss.jws.diag.config.parser.ServerXmlParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden output tests for the config command human formatter.
 *
 * <p>On the first run, if a golden file does not exist it is generated from
 * the current formatter output and the test passes (capturing baseline). On
 * subsequent runs the generated output must match the stored golden file.
 *
 * <p>To regenerate all golden files, delete them and re-run {@code mvn verify}.
 */
class ConfigHumanGoldenTest {

    private static final Path GOLDEN_DIR =
            Paths.get("src/test/resources/golden/config/human");

    @ParameterizedTest
    @ValueSource(strings = {
            "server-valid-basic",
            "server-all-defaults",
            "server-multi-connector",
            "server-executor",
            "server-proxy-valve",
            "server-multi-service",
            "server-full-tls",
            "server-empty-executor",
            "server-unknown-valve"
    })
    void goldenHumanOutputMatchesExpected(String fixtureName) throws Exception {
        ServerConfig config = parseFixture(fixtureName + ".xml");
        String actual = new ConfigHumanFormatter().format(config);

        Path goldenFile = GOLDEN_DIR.resolve(fixtureName + ".txt");
        if (!Files.exists(goldenFile)) {
            Files.createDirectories(goldenFile.getParent());
            Files.writeString(goldenFile, actual);
            return;
        }

        String expected = Files.readString(goldenFile);
        assertThat(actual)
                .as("Human output for %s differs from golden file", fixtureName)
                .isEqualTo(expected);
    }

    private static ServerConfig parseFixture(String fileName) throws IOException, URISyntaxException {
        Path path = Paths.get(ConfigHumanGoldenTest.class.getClassLoader()
                .getResource("fixtures/config/" + fileName).toURI());
        PropertyResolver resolver = new PropertyResolver(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        return new ServerXmlParser(resolver).parse(path);
    }
}
