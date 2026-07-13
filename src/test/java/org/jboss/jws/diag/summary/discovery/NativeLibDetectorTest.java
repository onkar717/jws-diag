package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.NativeInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NativeLibDetectorTest {

    @TempDir
    Path tempDir;

    private Path buildHome() throws IOException {
        Path home = tempDir.resolve("home");
        Files.createDirectories(home.resolve("conf"));
        Files.createDirectories(home.resolve("lib"));
        return home;
    }

    private void writeServerXml(Path home, String content) throws IOException {
        Files.writeString(home.resolve("conf/server.xml"), content);
    }

    private static String serverXmlWith(String listenerClass) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Server port=\"8005\" shutdown=\"SHUTDOWN\">\n"
                + "  <Listener className=\"" + listenerClass + "\"/>\n"
                + "</Server>\n";
    }

    private static final String SERVER_XML_NO_NATIVE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<Server port=\"8005\" shutdown=\"SHUTDOWN\">\n"
                    + "  <Listener className=\"org.apache.catalina.startup.VersionLoggerListener\"/>\n"
                    + "</Server>\n";

    @Test
    void returnsNull_whenCatalinaHomeIsNull() {
        NativeInfo result = new NativeLibDetector(null, null).detect();
        assertThat(result).isNull();
    }

    @Test
    void returnsNull_whenServerXmlMissing() throws IOException {
        Path home = buildHome();
        // buildHome() creates conf/ dir but no server.xml

        NativeInfo result = new NativeLibDetector(home, home).detect();
        assertThat(result).isNull();
    }

    @Test
    void returnsNull_whenNoNativeListener() throws IOException {
        Path home = buildHome();
        writeServerXml(home, SERVER_XML_NO_NATIVE);

        NativeInfo result = new NativeLibDetector(home, home).detect();
        assertThat(result).isNull();
    }

    @Test
    void returnsLoaded_whenAprLifecycleListenerPresent() throws IOException {
        Path home = buildHome();
        writeServerXml(home, serverXmlWith(NativeLibDetector.APR_LISTENER));

        NativeInfo result = new NativeLibDetector(home, home).detect();

        assertThat(result).isNotNull();
        assertThat(result.isLoaded()).isTrue();
    }

    @Test
    void returnsLoaded_whenOpenSslLifecycleListenerPresent() throws IOException {
        Path home = buildHome();
        writeServerXml(home, serverXmlWith(NativeLibDetector.OPENSSL_LISTENER));

        NativeInfo result = new NativeLibDetector(home, home).detect();

        assertThat(result).isNotNull();
        assertThat(result.isLoaded()).isTrue();
    }

    @Test
    void aprVersionIsNull_whenNoNativeJarInLib() throws IOException {
        Path home = buildHome();
        writeServerXml(home, serverXmlWith(NativeLibDetector.APR_LISTENER));

        NativeInfo result = new NativeLibDetector(home, home).detect();

        assertThat(result).isNotNull();
        assertThat(result.getAprVersion()).isNull();
    }

    @Test
    void extractsAprVersion_fromNativeJarFilename() throws IOException {
        Path home = buildHome();
        writeServerXml(home, serverXmlWith(NativeLibDetector.APR_LISTENER));
        Files.createFile(home.resolve("lib/tomcat-native-1.2.35-jni.jar"));

        NativeInfo result = new NativeLibDetector(home, home).detect();

        assertThat(result).isNotNull();
        assertThat(result.getAprVersion()).isEqualTo("1.2.35");
        assertThat(result.isLoaded()).isTrue();
    }

    @Test
    void usesBase_forServerXmlLookup_whenBaseAndHomeDiffer() throws IOException {
        Path home = buildHome();
        Path base = tempDir.resolve("base");
        Files.createDirectories(base.resolve("conf"));
        Files.writeString(base.resolve("conf/server.xml"),
                serverXmlWith(NativeLibDetector.APR_LISTENER));

        NativeInfo result = new NativeLibDetector(home, base).detect();

        assertThat(result).isNotNull();
        assertThat(result.isLoaded()).isTrue();
    }

    @Test
    void returnsNull_whenServerXmlIsInvalidXml() throws IOException {
        Path home = buildHome();
        Files.writeString(home.resolve("conf/server.xml"), "not-xml");

        NativeInfo result = new NativeLibDetector(home, home).detect();
        assertThat(result).isNull();
    }
}
