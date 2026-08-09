package org.jboss.jws.diag.modcluster;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.modcluster.formatter.ModClusterHumanFormatter;
import org.jboss.jws.diag.modcluster.formatter.ModClusterJsonFormatter;
import org.jboss.jws.diag.modcluster.model.ModClusterConfig;
import org.jboss.jws.diag.summary.discovery.CatalinaDiscovery;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Command(name = "modcluster",
        description = "Display mod_cluster/mod_proxy_cluster configuration from server.xml",
        mixinStandardHelpOptions = true)
public class ModClusterCommand implements Runnable {

    @Option(names = "--catalina-home", description = "Path to CATALINA_HOME")
    private Path catalinaHome;

    @Option(names = "--catalina-base", description = "Path to CATALINA_BASE (defaults to CATALINA_HOME)")
    private Path catalinaBase;

    @Mixin
    private OutputFormatMixin outputFormat;

    @Override
    public void run() {
        Path base = resolveBase();
        if (base == null) {
            System.err.println("ERROR: Could not determine CATALINA_BASE. "
                    + "Use --catalina-home or --catalina-base.");
            System.exit(ExitCodes.ERRORS);
            return;
        }

        Path serverXml = base.resolve("conf/server.xml");
        if (!Files.exists(serverXml)) {
            System.err.println("ERROR: server.xml not found at: " + serverXml);
            System.exit(ExitCodes.ERRORS);
            return;
        }

        List<ModClusterConfig> configs;
        try {
            configs = new ModClusterParser().parse(serverXml);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to parse server.xml: " + e.getMessage());
            System.exit(ExitCodes.ERRORS);
            return;
        }

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new ModClusterJsonFormatter().format(configs);
        } else {
            output = new ModClusterHumanFormatter().format(configs);
        }

        System.out.println(output);
        System.exit(configs.isEmpty() ? ExitCodes.WARNINGS : ExitCodes.OK);
    }

    private Path resolveBase() {
        if (catalinaBase != null) {
            if (!Files.isDirectory(catalinaBase)) {
                System.err.println("ERROR: --catalina-base is not a valid directory: " + catalinaBase);
                System.exit(ExitCodes.ERRORS);
            }
            return catalinaBase;
        }
        if (catalinaHome != null && !Files.isDirectory(catalinaHome)) {
            System.err.println("ERROR: --catalina-home is not a valid directory: " + catalinaHome);
            System.exit(ExitCodes.ERRORS);
        }
        CatalinaDiscovery.Result result = CatalinaDiscovery.create(catalinaHome, null).discover();
        return result.getCatalinaBase();
    }
}
