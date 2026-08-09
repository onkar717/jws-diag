package org.jboss.jws.diag.diff;

import org.jboss.jws.diag.config.model.CertificateConfig;
import org.jboss.jws.diag.config.model.ConfigValue;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.model.ServiceConfig;
import org.jboss.jws.diag.config.model.SslHostConfig;
import org.jboss.jws.diag.diff.model.ChangeType;
import org.jboss.jws.diag.diff.model.DiffEntry;
import org.jboss.jws.diag.diff.model.DiffReport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes a structural diff between two parsed {@link ServerConfig} trees.
 *
 * <p>Connectors are keyed by port; executors by name. Structural differences
 * (ADDED/REMOVED items) are reported before field-level CHANGED entries.
 */
public final class ConfigDiffer {

    public DiffReport diff(Path leftBase, Path rightBase,
                           ServerConfig left, ServerConfig right) {
        List<DiffEntry> entries = new ArrayList<>();

        compareServer(left, right, entries);

        Map<String, ServiceConfig> leftServices = indexServicesByName(left.getServices());
        Map<String, ServiceConfig> rightServices = indexServicesByName(right.getServices());

        for (String name : union(leftServices.keySet(), rightServices.keySet())) {
            ServiceConfig lSvc = leftServices.get(name);
            ServiceConfig rSvc = rightServices.get(name);
            String svcPath = "services[" + name + "]";
            if (lSvc == null) {
                entries.add(new DiffEntry(svcPath, ChangeType.ADDED, null, name));
            } else if (rSvc == null) {
                entries.add(new DiffEntry(svcPath, ChangeType.REMOVED, name, null));
            } else {
                compareConnectors(svcPath, lSvc, rSvc, entries);
                compareExecutors(svcPath, lSvc, rSvc, entries);
            }
        }

        return new DiffReport(leftBase, rightBase, entries);
    }

    private void compareServer(ServerConfig left, ServerConfig right, List<DiffEntry> out) {
        if (left.getShutdownPort() != right.getShutdownPort()) {
            out.add(new DiffEntry("server.shutdownPort", ChangeType.CHANGED,
                    String.valueOf(left.getShutdownPort()),
                    String.valueOf(right.getShutdownPort())));
        }
        diffNullable("server.shutdownCommand",
                left.getShutdownCommand(), right.getShutdownCommand(), out);
    }

    private void compareConnectors(String svcPath,
                                   ServiceConfig left, ServiceConfig right,
                                   List<DiffEntry> out) {
        Map<Integer, ConnectorConfig> lMap = indexConnectorsByPort(left.getConnectors());
        Map<Integer, ConnectorConfig> rMap = indexConnectorsByPort(right.getConnectors());

        for (int port : union(lMap.keySet(), rMap.keySet())) {
            String cPath = svcPath + ".connectors[" + port + "]";
            ConnectorConfig lc = lMap.get(port);
            ConnectorConfig rc = rMap.get(port);
            if (lc == null) {
                out.add(new DiffEntry(cPath, ChangeType.ADDED, null, "port " + port));
            } else if (rc == null) {
                out.add(new DiffEntry(cPath, ChangeType.REMOVED, "port " + port, null));
            } else {
                diffCv(cPath + ".protocol", lc.getProtocol(), rc.getProtocol(), out);
                diffCv(cPath + ".sslEnabled", lc.getSslEnabled(), rc.getSslEnabled(), out);
                diffCv(cPath + ".maxThreads", lc.getMaxThreads(), rc.getMaxThreads(), out);
                diffCv(cPath + ".connectionTimeout", lc.getConnectionTimeout(), rc.getConnectionTimeout(), out);
                diffCv(cPath + ".maxConnections", lc.getMaxConnections(), rc.getMaxConnections(), out);
                diffCv(cPath + ".compression", lc.getCompression(), rc.getCompression(), out);
                diffCv(cPath + ".secretRequired", lc.getSecretRequired(), rc.getSecretRequired(), out);
                diffNullable(cPath + ".executorRef", lc.getExecutorRef(), rc.getExecutorRef(), out);
                diffNullable(cPath + ".proxyName", lc.getProxyName(), rc.getProxyName(), out);
                diffNullable(cPath + ".proxyPort",
                        lc.getProxyPort() == null ? null : String.valueOf(lc.getProxyPort()),
                        rc.getProxyPort() == null ? null : String.valueOf(rc.getProxyPort()), out);
                compareSslHostConfigs(cPath,
                        lc.getSslHostConfigs() != null ? lc.getSslHostConfigs() : Collections.emptyList(),
                        rc.getSslHostConfigs() != null ? rc.getSslHostConfigs() : Collections.emptyList(),
                        out);
            }
        }
    }

    private void compareSslHostConfigs(String cPath,
                                       List<SslHostConfig> left, List<SslHostConfig> right,
                                       List<DiffEntry> out) {
        Map<String, SslHostConfig> lMap = indexSslByHost(left);
        Map<String, SslHostConfig> rMap = indexSslByHost(right);

        for (String host : union(lMap.keySet(), rMap.keySet())) {
            String sPath = cPath + ".ssl[" + host + "]";
            SslHostConfig ls = lMap.get(host);
            SslHostConfig rs = rMap.get(host);
            if (ls == null) {
                out.add(new DiffEntry(sPath, ChangeType.ADDED, null, host));
            } else if (rs == null) {
                out.add(new DiffEntry(sPath, ChangeType.REMOVED, host, null));
            } else {
                diffNullable(sPath + ".protocols", ls.getProtocols(), rs.getProtocols(), out);
                diffNullable(sPath + ".sslEnabledProtocols",
                        ls.getSslEnabledProtocols(), rs.getSslEnabledProtocols(), out);
                diffNullable(sPath + ".ciphers", ls.getCiphers(), rs.getCiphers(), out);
                diffNullable(sPath + ".certificateVerification",
                        ls.getCertificateVerification(), rs.getCertificateVerification(), out);
                compareCertificates(sPath, ls.getCertificates(), rs.getCertificates(), out);
            }
        }
    }

    private void compareCertificates(String sPath,
                                     List<CertificateConfig> left, List<CertificateConfig> right,
                                     List<DiffEntry> out) {
        Map<String, CertificateConfig> lMap = indexCertsByType(left);
        Map<String, CertificateConfig> rMap = indexCertsByType(right);

        for (String type : union(lMap.keySet(), rMap.keySet())) {
            String certPath = sPath + ".certificate[" + type + "]";
            CertificateConfig lc = lMap.get(type);
            CertificateConfig rc = rMap.get(type);
            if (lc == null) {
                out.add(new DiffEntry(certPath, ChangeType.ADDED, null, type));
            } else if (rc == null) {
                out.add(new DiffEntry(certPath, ChangeType.REMOVED, type, null));
            } else {
                diffNullable(certPath + ".keystoreFile", lc.getKeystoreFile(), rc.getKeystoreFile(), out);
                diffCv(certPath + ".keystoreType", lc.getKeystoreType(), rc.getKeystoreType(), out);
                diffNullable(certPath + ".type", lc.getType(), rc.getType(), out);
            }
        }
    }

    private void compareExecutors(String svcPath,
                                  ServiceConfig left, ServiceConfig right,
                                  List<DiffEntry> out) {
        Map<String, ExecutorConfig> lMap = indexExecutorsByName(left.getExecutors());
        Map<String, ExecutorConfig> rMap = indexExecutorsByName(right.getExecutors());

        for (String name : union(lMap.keySet(), rMap.keySet())) {
            String ePath = svcPath + ".executors[" + name + "]";
            ExecutorConfig le = lMap.get(name);
            ExecutorConfig re = rMap.get(name);
            if (le == null) {
                out.add(new DiffEntry(ePath, ChangeType.ADDED, null, name));
            } else if (re == null) {
                out.add(new DiffEntry(ePath, ChangeType.REMOVED, name, null));
            } else {
                diffCv(ePath + ".maxThreads", le.getMaxThreads(), re.getMaxThreads(), out);
                diffCv(ePath + ".minSpareThreads", le.getMinSpareThreads(), re.getMinSpareThreads(), out);
                diffCv(ePath + ".threadPriority", le.getThreadPriority(), re.getThreadPriority(), out);
                diffCv(ePath + ".maxIdleTime", le.getMaxIdleTime(), re.getMaxIdleTime(), out);
                diffNullable(ePath + ".namePrefix", le.getNamePrefix(), re.getNamePrefix(), out);
            }
        }
    }

    private <T> void diffCv(String path, ConfigValue<T> left, ConfigValue<T> right,
                             List<DiffEntry> out) {
        if (left == null && right == null) return;
        String lStr = left == null ? null : renderCv(left);
        String rStr = right == null ? null : renderCv(right);
        if (!Objects.equals(lStr, rStr)) {
            out.add(new DiffEntry(path, ChangeType.CHANGED, lStr, rStr));
        }
    }

    private void diffNullable(String path, String left, String right, List<DiffEntry> out) {
        if (!Objects.equals(left, right)) {
            out.add(new DiffEntry(path, ChangeType.CHANGED, left, right));
        }
    }

    static <T> String renderCv(ConfigValue<T> cv) {
        return cv.getValue() + " (" + (cv.isExplicit() ? "explicit" : "default") + ")";
    }

    private static Map<String, ServiceConfig> indexServicesByName(List<ServiceConfig> items) {
        Map<String, ServiceConfig> map = new LinkedHashMap<>();
        for (ServiceConfig s : items) map.put(s.getName(), s);
        return map;
    }

    private static Map<String, ExecutorConfig> indexExecutorsByName(List<ExecutorConfig> items) {
        Map<String, ExecutorConfig> map = new LinkedHashMap<>();
        for (ExecutorConfig e : items) map.put(e.getName(), e);
        return map;
    }

    private static Map<Integer, ConnectorConfig> indexConnectorsByPort(List<ConnectorConfig> items) {
        Map<Integer, ConnectorConfig> map = new LinkedHashMap<>();
        for (ConnectorConfig c : items) map.put(c.getPort(), c);
        return map;
    }

    private static Map<String, SslHostConfig> indexSslByHost(List<SslHostConfig> items) {
        Map<String, SslHostConfig> map = new LinkedHashMap<>();
        int idx = 0;
        for (SslHostConfig s : items) {
            String key = s.getHostName() != null ? s.getHostName() : String.valueOf(idx);
            map.put(key, s);
            idx++;
        }
        return map;
    }

    private static Map<String, CertificateConfig> indexCertsByType(List<CertificateConfig> items) {
        Map<String, CertificateConfig> map = new LinkedHashMap<>();
        int idx = 0;
        for (CertificateConfig c : items) {
            String key = c.getType() != null ? c.getType() : String.valueOf(idx);
            map.put(key, c);
            idx++;
        }
        return map;
    }

    private static <T> Iterable<T> union(Iterable<T> a, Iterable<T> b) {
        java.util.LinkedHashSet<T> set = new java.util.LinkedHashSet<>();
        a.forEach(set::add);
        b.forEach(set::add);
        return set;
    }
}
