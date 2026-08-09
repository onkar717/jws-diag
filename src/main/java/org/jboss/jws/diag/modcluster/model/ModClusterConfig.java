package org.jboss.jws.diag.modcluster.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Collections;
import java.util.Map;

/**
 * Configuration extracted from a {@code ModClusterListener} element in {@code server.xml}.
 *
 * <p>Default values are Tomcat/mod_cluster compiled-in defaults; only attributes
 * explicitly set in server.xml are marked {@code explicit=true} in the raw map.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"listenerClassName", "connector", "advertise",
        "advertiseGroupAddress", "advertisePort", "proxyList",
        "balancer", "stickySession", "stickySessionCookie", "extraAttributes"})
public final class ModClusterConfig {

    private final String listenerClassName;
    private final String connector;
    private final boolean advertise;
    private final String advertiseGroupAddress;
    private final int advertisePort;
    private final String proxyList;
    private final String balancer;
    private final boolean stickySession;
    private final String stickySessionCookie;
    private final Map<String, String> extraAttributes;

    private ModClusterConfig(Builder b) {
        this.listenerClassName = b.listenerClassName;
        this.connector = b.connector;
        this.advertise = b.advertise;
        this.advertiseGroupAddress = b.advertiseGroupAddress;
        this.advertisePort = b.advertisePort;
        this.proxyList = b.proxyList;
        this.balancer = b.balancer;
        this.stickySession = b.stickySession;
        this.stickySessionCookie = b.stickySessionCookie;
        this.extraAttributes = b.extraAttributes != null
                ? Collections.unmodifiableMap(b.extraAttributes) : Collections.emptyMap();
    }

    @JsonProperty("listenerClassName")
    public String getListenerClassName() { return listenerClassName; }

    @JsonProperty("connector")
    public String getConnector() { return connector; }

    @JsonProperty("advertise")
    public boolean isAdvertise() { return advertise; }

    @JsonProperty("advertiseGroupAddress")
    public String getAdvertiseGroupAddress() { return advertiseGroupAddress; }

    @JsonProperty("advertisePort")
    public int getAdvertisePort() { return advertisePort; }

    @JsonProperty("proxyList")
    public String getProxyList() { return proxyList; }

    @JsonProperty("balancer")
    public String getBalancer() { return balancer; }

    @JsonProperty("stickySession")
    public boolean isStickySession() { return stickySession; }

    @JsonProperty("stickySessionCookie")
    public String getStickySessionCookie() { return stickySessionCookie; }

    @JsonProperty("extraAttributes")
    public Map<String, String> getExtraAttributes() {
        return extraAttributes.isEmpty() ? null : extraAttributes;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String listenerClassName;
        private String connector = "ajp";
        private boolean advertise = true;
        private String advertiseGroupAddress = "224.0.1.105";
        private int advertisePort = 23364;
        private String proxyList;
        private String balancer = "mycluster";
        private boolean stickySession = true;
        private String stickySessionCookie = "JSESSIONID";
        private Map<String, String> extraAttributes;

        public Builder listenerClassName(String v) { this.listenerClassName = v; return this; }
        public Builder connector(String v) { this.connector = v; return this; }
        public Builder advertise(boolean v) { this.advertise = v; return this; }
        public Builder advertiseGroupAddress(String v) { this.advertiseGroupAddress = v; return this; }
        public Builder advertisePort(int v) { this.advertisePort = v; return this; }
        public Builder proxyList(String v) { this.proxyList = v; return this; }
        public Builder balancer(String v) { this.balancer = v; return this; }
        public Builder stickySession(boolean v) { this.stickySession = v; return this; }
        public Builder stickySessionCookie(String v) { this.stickySessionCookie = v; return this; }
        public Builder extraAttributes(Map<String, String> v) { this.extraAttributes = v; return this; }

        public ModClusterConfig build() { return new ModClusterConfig(this); }
    }
}
