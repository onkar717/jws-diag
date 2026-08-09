package org.jboss.jws.diag.modcluster.formatter;

import org.jboss.jws.diag.modcluster.model.ModClusterConfig;

import java.util.List;
import java.util.Map;

/**
 * Renders mod_cluster configuration as human-readable text.
 *
 * <p>Example:
 * <pre>
 * mod_cluster Configuration (1 listener)
 *
 * Listener: org.jboss.modcluster.container.catalina.standalone.ModClusterListener
 *   Connector:           ajp
 *   Advertise:           true (224.0.1.105:23364)
 *   Proxy list:          httpd1:6666, httpd2:6666
 *   Balancer:            mycluster
 *   Sticky sessions:     true (cookie: JSESSIONID)
 * </pre>
 */
public final class ModClusterHumanFormatter {

    public String format(List<ModClusterConfig> configs) {
        StringBuilder sb = new StringBuilder();

        if (configs.isEmpty()) {
            sb.append("No mod_cluster listener found in server.xml.\n");
            sb.append("Tip: Add <Listener className=\"org.jboss.modcluster.container.catalina."
                    + "standalone.ModClusterListener\" .../> to enable mod_cluster.\n");
            return sb.toString();
        }

        sb.append(String.format("mod_cluster Configuration (%d listener%s)%n",
                configs.size(), configs.size() == 1 ? "" : "s"));

        for (ModClusterConfig cfg : configs) {
            sb.append('\n');
            sb.append(String.format("Listener: %s%n", cfg.getListenerClassName()));
            sb.append(String.format("  %-22s %s%n", "Connector:", cfg.getConnector()));

            String advertiseAddr = String.format("%s:%d",
                    cfg.getAdvertiseGroupAddress(), cfg.getAdvertisePort());
            sb.append(String.format("  %-22s %s (%s)%n",
                    "Advertise:", cfg.isAdvertise(), advertiseAddr));

            if (cfg.getProxyList() != null) {
                sb.append(String.format("  %-22s %s%n", "Proxy list:", cfg.getProxyList()));
            } else {
                sb.append(String.format("  %-22s (auto-discover via advertise)%n", "Proxy list:"));
            }

            sb.append(String.format("  %-22s %s%n", "Balancer:", cfg.getBalancer()));
            sb.append(String.format("  %-22s %s (cookie: %s)%n",
                    "Sticky sessions:", cfg.isStickySession(), cfg.getStickySessionCookie()));

            if (cfg.getExtraAttributes() != null && !cfg.getExtraAttributes().isEmpty()) {
                sb.append(String.format("  %-22s%n", "Additional attributes:"));
                for (Map.Entry<String, String> e : cfg.getExtraAttributes().entrySet()) {
                    sb.append(String.format("    %-20s %s%n", e.getKey() + ":", e.getValue()));
                }
            }
        }
        return sb.toString();
    }
}
