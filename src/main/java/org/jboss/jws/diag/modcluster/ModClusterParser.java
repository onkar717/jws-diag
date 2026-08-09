package org.jboss.jws.diag.modcluster;

import org.jboss.jws.diag.modcluster.model.ModClusterConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans {@code server.xml} for {@code <Listener>} elements whose
 * {@code className} contains {@code "ModCluster"} and extracts their
 * configuration into {@link ModClusterConfig} objects.
 *
 * <p>Parses independently of the main {@code ServerXmlParser} to avoid any
 * coupling with the config command's data model.
 */
public final class ModClusterParser {

    private static final String MOD_CLUSTER_MARKER = "ModCluster";

    /** Known attributes handled explicitly; everything else goes into extraAttributes. */
    private static final java.util.Set<String> KNOWN_ATTRS = java.util.Set.of(
            "className", "connector", "advertise", "advertiseGroupAddress",
            "advertisePort", "proxyList", "balancer", "stickySession", "stickySessionCookie"
    );

    /**
     * Parses the given {@code server.xml} and returns all mod_cluster listener
     * configurations found. Returns an empty list if none are present.
     *
     * @throws IOException if the file cannot be read or parsed
     */
    public List<ModClusterConfig> parse(Path serverXml) throws IOException {
        Document doc = parseXml(serverXml);
        List<ModClusterConfig> results = new ArrayList<>();

        NodeList listeners = doc.getElementsByTagName("Listener");
        for (int i = 0; i < listeners.getLength(); i++) {
            Node node = listeners.item(i);
            if (!(node instanceof Element)) continue;
            Element el = (Element) node;
            String className = el.getAttribute("className");
            if (className == null || !className.contains(MOD_CLUSTER_MARKER)) continue;
            results.add(extract(el));
        }
        return results;
    }

    private ModClusterConfig extract(Element el) {
        Map<String, String> extra = new LinkedHashMap<>();
        NamedNodeMap attrs = el.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String name = attr.getNodeName();
            if (!KNOWN_ATTRS.contains(name)) {
                extra.put(name, attr.getNodeValue());
            }
        }

        ModClusterConfig.Builder b = ModClusterConfig.builder()
                .listenerClassName(el.getAttribute("className"))
                .extraAttributes(extra);

        String connector = el.getAttribute("connector");
        if (!connector.isEmpty()) b.connector(connector);

        String advertise = el.getAttribute("advertise");
        if (!advertise.isEmpty()) b.advertise(Boolean.parseBoolean(advertise));

        String advertiseGroup = el.getAttribute("advertiseGroupAddress");
        if (!advertiseGroup.isEmpty()) b.advertiseGroupAddress(advertiseGroup);

        String advertisePort = el.getAttribute("advertisePort");
        if (!advertisePort.isEmpty()) {
            try { b.advertisePort(Integer.parseInt(advertisePort)); } catch (NumberFormatException ignored) {}
        }

        String proxyList = el.getAttribute("proxyList");
        if (!proxyList.isEmpty()) b.proxyList(proxyList);

        String balancer = el.getAttribute("balancer");
        if (!balancer.isEmpty()) b.balancer(balancer);

        String stickySession = el.getAttribute("stickySession");
        if (!stickySession.isEmpty()) b.stickySession(Boolean.parseBoolean(stickySession));

        String stickySessionCookie = el.getAttribute("stickySessionCookie");
        if (!stickySessionCookie.isEmpty()) b.stickySessionCookie(stickySessionCookie);

        return b.build();
    }

    private static Document parseXml(Path path) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(path.toFile());
        } catch (Exception e) {
            throw new IOException("Failed to parse " + path + ": " + e.getMessage(), e);
        }
    }
}
