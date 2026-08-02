# jws-diag

Diagnostic and configuration analysis toolkit for
[JBoss Web Server](https://www.redhat.com/en/technologies/jboss-middleware/web-server) /
[Apache Tomcat](https://tomcat.apache.org/).

**Status:** Under active development as part of [Google Summer of Code 2026](https://summerofcode.withgoogle.com/).

## What It Does

jws-diag is a read-only CLI that helps SREs and support engineers quickly understand
and validate a Tomcat/JWS installation without modifying any files or sending network requests.

| Command              | Description                                                                   |
|----------------------|-------------------------------------------------------------------------------|
| `jws-diag summary`  | Show installed versions, JVM info, OS/container signals, native library status |
| `jws-diag config`   | Parse and display effective connector, TLS, proxy, and executor configuration  |
| `jws-diag validate` | Run diagnostic rules and report findings as INFO/WARN/ERROR                   |
| `jws-diag bundle`   | Generate a redacted `.tar.gz` support bundle for safe sharing                 |

## Quickstart

**Requirements:** JDK 11+, Maven 3.6+

### 1. Build

```bash
git clone https://github.com/web-servers/jws-diag.git
cd jws-diag
mvn package -q
```

The self-contained jar is at `target/jws-diag-0.1.0-SNAPSHOT.jar`.

### 2. Point at your Tomcat installation

jws-diag auto-discovers CATALINA_HOME through five sources (in priority order):

1. `--catalina-home` CLI flag
2. `CATALINA_HOME` / `CATALINA_BASE` environment variables
3. systemd override files (`/etc/sysconfig/tomcat`, `/etc/default/tomcat`)
4. Well-known paths (`/opt/rh/jws*/root/usr/share/tomcat`, `/usr/share/tomcat`)
5. Running process detection via `/proc/*/cmdline`

If Tomcat is installed at a standard path, no flags are required.

### 3. Run

```bash
# Auto-discover Tomcat
java -jar target/jws-diag-0.1.0-SNAPSHOT.jar summary

# Explicit path
java -jar target/jws-diag-0.1.0-SNAPSHOT.jar summary --catalina-home /opt/tomcat

# JSON output
java -jar target/jws-diag-0.1.0-SNAPSHOT.jar config --format json
```

## Example Output

### `jws-diag summary`

```
Tomcat 10.1.49 | JWS 6.1.0
CATALINA_HOME: /opt/rh/jws6/root/usr/share/tomcat
CATALINA_BASE: /opt/rh/jws6/root/etc/tomcat
JVM: 17.0.10 (Red Hat, Inc.) | OS: RHEL 9.3 (x86_64)
Container: Podman (detected via /run/.containerenv)
Native: APR 1.7.2 | OpenSSL 3.0.9
PID: 12345
```

### `jws-diag config` (human)

```
Connector :8080 [HTTP/1.1]
  protocol: HTTP/1.1 (explicit)
  sslEnabled: false (default)
  maxThreads: 200 (default)
  connectionTimeout: 20000 (explicit)
  maxConnections: 8192 (default)
  compression: off (default)
Connector :8443 [HTTP/1.1] ★ SSL
  maxThreads: 150 (explicit)
  connectionTimeout: 60000 (default)
  SSLHostConfig:
    protocols: TLSv1.2+TLSv1.3
    Certificate:
      keystoreFile: conf/localhost-rsa.jks
      keystoreType: JKS (default)
      keystorePass: ***REDACTED***
Executor "tomcatThreadPool"
  maxThreads: 150 (explicit)
  minSpareThreads: 4 (explicit)
  threadPriority: 5 (default)
  maxIdleTime: 60000 (default)
  namePrefix: catalina-exec-
Host: localhost
  appBase: webapps (explicit)
  autoDeploy: true (explicit)
  unpackWARs: true (explicit)
  Valve: AccessLog [org.apache.catalina.valves.AccessLogValve]
    directory: logs
    pattern: %h %l %u %t "%r" %s %b
```

Each attribute is annotated `(explicit)` when set in `server.xml` or `(default)` when
the Tomcat compiled-in default was applied. Passwords and secrets are always `***REDACTED***`.

### `jws-diag config --format json` (excerpt)

```json
{
  "schemaVersion": "1.0",
  "shutdownPort": 8005,
  "services": [{
    "name": "Catalina",
    "connectors": [{
      "port": 8080,
      "protocol": { "value": "HTTP/1.1", "explicit": true },
      "sslEnabled": { "value": false, "explicit": false },
      "maxThreads": { "value": 200, "explicit": false },
      "connectionTimeout": { "value": 20000, "explicit": true }
    }],
    "executors": [{
      "name": "tomcatThreadPool",
      "maxThreads": { "value": 150, "explicit": true },
      "minSpareThreads": { "value": 4, "explicit": true },
      "threadPriority": { "value": 5, "explicit": false },
      "maxIdleTime": { "value": 60000, "explicit": false }
    }]
  }]
}
```

The `{ "value": ..., "explicit": true/false }` pattern makes it easy for CI pipelines
and automation to distinguish operator-set values from Tomcat defaults.

## CLI Reference

### Global options

| Option | Description |
|--------|-------------|
| `--help` | Show help and exit |
| `--version` | Show version and exit |
| `--format HUMAN\|JSON` | Output format (default: `HUMAN`) |

### `summary` options

| Option | Description |
|--------|-------------|
| `--catalina-home <path>` | Override CATALINA_HOME discovery |

### `config` options

| Option | Description |
|--------|-------------|
| `--catalina-home <path>` | Override CATALINA_HOME discovery |
| `--catalina-base <path>` | Override CATALINA_BASE (defaults to CATALINA_HOME) |

## Configuration Parsing Details

### Property resolution

Placeholders in `server.xml` are resolved in this order:

1. JVM system properties (`-Dkey=value`)
2. `CATALINA_BASE/conf/catalina.properties`
3. Environment variables (`${env.VAR_NAME}`)
4. `${VAULT::block::attr::}` tokens — preserved as-is (resolved at runtime by tomcat-vault)
5. Unresolved `${...}` — kept verbatim in output

### Tomcat defaults

Attributes absent from `server.xml` receive their Tomcat 10.1.x compiled-in defaults:

| Attribute | Default | Element |
|-----------|---------|---------|
| `maxThreads` | 200 | Connector |
| `connectionTimeout` | 60000 ms | Connector |
| `maxConnections` | 8192 | Connector |
| `protocol` | `HTTP/1.1` | Connector |
| `SSLEnabled` | `false` | Connector |
| `compression` | `off` | Connector |
| `secretRequired` | `true` | AJP Connector |
| `autoDeploy` | `true` | Host |
| `unpackWARs` | `true` | Host |
| `keystoreType` | `JKS` | SSLHostConfig |
| `maxThreads` | 200 | Executor |
| `minSpareThreads` | 25 | Executor |
| `threadPriority` | 5 | Executor |
| `maxIdleTime` | 60000 ms | Executor |

### Security

- **Read-only:** No file writes, no process signals, no network calls
- **Secrets redacted:** Attribute names matching `*password*`, `*secret*`, `*credential*` → `***REDACTED***`
- **VAULT tokens preserved:** `${VAULT::...}` passed through opaque — never resolved or redacted
- **Minimal privileges:** Runs as the invoking user; no privilege escalation required

## Native Library Detection

`jws-diag summary` detects APR/OpenSSL native library configuration by:

1. Scanning `CATALINA_BASE/conf/server.xml` for `AprLifecycleListener` or `OpenSSLLifecycleListener`
2. Scanning `CATALINA_HOME/lib/` for versioned jars (`tomcat-native-*.jar`, `openssl-*.jar`)
3. Selecting the highest semver when multiple versions are present

Note: listener presence indicates the library is **configured**, not confirmed loaded at runtime.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow, code standards,
and PR process.

## License

[Apache License 2.0](LICENSE)
