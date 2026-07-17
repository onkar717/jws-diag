package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents an {@code <Executor>} element in {@code server.xml}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ExecutorConfig {

    private final String name;
    private final ConfigValue<Integer> maxThreads;
    private final ConfigValue<Integer> minSpareThreads;
    private final ConfigValue<Integer> threadPriority;
    private final ConfigValue<Integer> maxIdleTime;
    private final String namePrefix;

    private ExecutorConfig(Builder b) {
        this.name = b.name;
        this.maxThreads = b.maxThreads;
        this.minSpareThreads = b.minSpareThreads;
        this.threadPriority = b.threadPriority;
        this.maxIdleTime = b.maxIdleTime;
        this.namePrefix = b.namePrefix;
    }

    public String getName() { return name; }
    public ConfigValue<Integer> getMaxThreads() { return maxThreads; }
    public ConfigValue<Integer> getMinSpareThreads() { return minSpareThreads; }
    public ConfigValue<Integer> getThreadPriority() { return threadPriority; }
    public ConfigValue<Integer> getMaxIdleTime() { return maxIdleTime; }
    public String getNamePrefix() { return namePrefix; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private ConfigValue<Integer> maxThreads;
        private ConfigValue<Integer> minSpareThreads;
        private ConfigValue<Integer> threadPriority;
        private ConfigValue<Integer> maxIdleTime;
        private String namePrefix;

        public Builder name(String v) { this.name = v; return this; }
        public Builder maxThreads(ConfigValue<Integer> v) { this.maxThreads = v; return this; }
        public Builder minSpareThreads(ConfigValue<Integer> v) { this.minSpareThreads = v; return this; }
        public Builder threadPriority(ConfigValue<Integer> v) { this.threadPriority = v; return this; }
        public Builder maxIdleTime(ConfigValue<Integer> v) { this.maxIdleTime = v; return this; }
        public Builder namePrefix(String v) { this.namePrefix = v; return this; }

        public ConfigValue<Integer> getMaxThreads() { return maxThreads; }
        public ConfigValue<Integer> getMinSpareThreads() { return minSpareThreads; }
        public ConfigValue<Integer> getThreadPriority() { return threadPriority; }
        public ConfigValue<Integer> getMaxIdleTime() { return maxIdleTime; }

        public ExecutorConfig build() { return new ExecutorConfig(this); }
    }

    @Override
    public String toString() {
        return "ExecutorConfig{name='" + name + "', maxThreads=" + maxThreads
                + ", minSpareThreads=" + minSpareThreads
                + ", threadPriority=" + threadPriority
                + ", maxIdleTime=" + maxIdleTime + '}';
    }
}
