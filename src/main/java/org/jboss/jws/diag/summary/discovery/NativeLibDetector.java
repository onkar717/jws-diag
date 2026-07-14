package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.NativeInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects native library configuration (APR/OpenSSL) from server.xml listeners
 * and optional version information from native JARs in {@code CATALINA_HOME/lib/}.
 *
 * <p>Two signals are checked:
 * <ol>
 *   <li>{@code <Listener className="org.apache.catalina.core.AprLifecycleListener"/>} in
 *       {@code CATALINA_BASE/conf/server.xml} — presence means APR native is configured.</li>
 *   <li>{@code <Listener className="org.apache.tomcat.util.net.openssl.OpenSSLLifecycleListener"/>}
 *       — legacy OpenSSL listener (Tomcat 8.x).</li>
 * </ol>
 *
 * <p>Listener presence indicates the native library is <em>configured</em>, not necessarily
 * loaded at runtime. {@link NativeInfo#isLoaded()} is therefore left {@code null}; runtime
 * confirmation would require process introspection.
 *
 * <p>APR version is extracted from the highest-versioned {@code tomcat-native-<version>*.jar}
 * in {@code CATALINA_HOME/lib/}. OpenSSL version is extracted similarly from
 * {@code openssl-<version>*.jar}. When multiple matching JARs exist the highest semver wins.
 *
 * <p>Returns {@code null} when no native listener is found or {@code catalinaHome} is null.
 */
class NativeLibDetector {

    static final String APR_LISTENER =
            "org.apache.catalina.core.AprLifecycleListener";
    static final String OPENSSL_LISTENER =
            "org.apache.tomcat.util.net.openssl.OpenSSLLifecycleListener";

    private static final Pattern APR_VERSION_PATTERN =
            Pattern.compile("tomcat-native-(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern OPENSSL_VERSION_PATTERN =
            Pattern.compile("openssl-(\\d+\\.\\d+\\.\\d+)");

    private final Path catalinaHome;
    private final Path catalinaBase;

    NativeLibDetector(Path catalinaHome, Path catalinaBase) {
        this.catalinaHome = catalinaHome;
        this.catalinaBase = catalinaBase;
    }

    NativeInfo detect() {
        if (catalinaHome == null) {
            return null;
        }
        Path base = catalinaBase != null ? catalinaBase : catalinaHome;
        Path serverXml = base.resolve("conf/server.xml");
        if (!Files.isReadable(serverXml)) {
            return null;
        }

        if (!hasNativeListener(serverXml)) {
            return null;
        }

        String aprVersion = scanHighestVersion(APR_VERSION_PATTERN, "tomcat-native-*.jar");
        String opensslVersion = scanHighestVersion(OPENSSL_VERSION_PATTERN, "openssl-*.jar");

        return NativeInfo.builder()
                .aprVersion(aprVersion)
                .opensslVersion(opensslVersion)
                // loaded is intentionally null: listener presence means "configured",
                // confirming the lib actually loaded at runtime requires process introspection
                .build();
    }

    private boolean hasNativeListener(Path serverXml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(serverXml.toFile());

            NodeList listeners = doc.getElementsByTagName("Listener");
            for (int i = 0; i < listeners.getLength(); i++) {
                String cn = ((Element) listeners.item(i)).getAttribute("className");
                if (APR_LISTENER.equals(cn) || OPENSSL_LISTENER.equals(cn)) {
                    return true;
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ignored) {
        }
        return false;
    }

    private String scanHighestVersion(Pattern pattern, String glob) {
        Path lib = catalinaHome.resolve("lib");
        if (!Files.isDirectory(lib)) {
            return null;
        }
        List<String> versions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(lib, glob)) {
            for (Path jar : stream) {
                Matcher m = pattern.matcher(jar.getFileName().toString());
                if (m.find()) {
                    versions.add(m.group(1));
                }
            }
        } catch (IOException ignored) {
        }
        return versions.stream()
                .max(Comparator.comparingInt((String v) -> versionPart(v, 0))
                        .thenComparingInt(v -> versionPart(v, 1))
                        .thenComparingInt(v -> versionPart(v, 2)))
                .orElse(null);
    }

    private static int versionPart(String version, int index) {
        String[] parts = version.split("\\.", -1);
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
