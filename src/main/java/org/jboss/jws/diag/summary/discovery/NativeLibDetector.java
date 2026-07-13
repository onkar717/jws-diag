package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.NativeInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * <p>Version is extracted from a {@code tomcat-native-<version>*.jar} file in
 * {@code CATALINA_HOME/lib/} when present.
 *
 * <p>Returns {@code null} when no native listener is found.
 */
class NativeLibDetector {

    static final String APR_LISTENER =
            "org.apache.catalina.core.AprLifecycleListener";
    static final String OPENSSL_LISTENER =
            "org.apache.tomcat.util.net.openssl.OpenSSLLifecycleListener";

    private static final Pattern NATIVE_VERSION_PATTERN =
            Pattern.compile("tomcat-native-(\\d+\\.\\d+\\.\\d+)");

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

        String aprVersion = scanNativeJarVersion();

        return NativeInfo.builder()
                .aprVersion(aprVersion)
                .loaded(true)
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
        } catch (Exception ignored) {
        }
        return false;
    }

    private String scanNativeJarVersion() {
        Path lib = catalinaHome.resolve("lib");
        if (!Files.isDirectory(lib)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(lib, "tomcat-native-*.jar")) {
            for (Path jar : stream) {
                Matcher m = NATIVE_VERSION_PATTERN.matcher(jar.getFileName().toString());
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
