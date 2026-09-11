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
        System.exit(execute());
    }

    public int execute() {
        Path base;
        try {
            base = resolveBase();
        } catch (IllegalStateException e) {
            System.err.println("ERROR: " + e.getMessage());
            return ExitCodes.TOOL_FAILURE;
        }

        if (base == null) {
            System.err.println("ERROR: Could not determine CATALINA_BASE. "
                    + "Use --catalina-home or --catalina-base.");
            return ExitCodes.TOOL_FAILURE;
        }

        Path serverXml = base.resolve("conf/server.xml");
        if (!Files.exists(serverXml)) {
            System.err.println("ERROR: server.xml not found at: " + serverXml);
            return ExitCodes.TOOL_FAILURE;
        }

        List<ModClusterConfig> configs;
        try {
            configs = new ModClusterParser().parse(serverXml);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to parse server.xml: " + e.getMessage());
            return ExitCodes.TOOL_FAILURE;
        }

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new ModClusterJsonFormatter().format(configs);
        } else {
            output = new ModClusterHumanFormatter().format(configs);
        }

        System.out.println(output);

        if (configs.isEmpty()) {
            System.out.println("No mod_cluster configuration found in " + serverXml + ".");
        }

        // mod_cluster is optional. Its absence is a result, not a failure.
        return ExitCodes.OK;
    }

    private Path resolveBase() {
        if (catalinaBase != null) {
            if (!Files.isDirectory(catalinaBase)) {
                throw new IllegalStateException(
                        "--catalina-base is not a valid directory: " + catalinaBase);
            }
            return catalinaBase;
        }
        if (catalinaHome != null && !Files.isDirectory(catalinaHome)) {
            throw new IllegalStateException(
                    "--catalina-home is not a valid directory: " + catalinaHome);
        }
        CatalinaDiscovery.Result result = CatalinaDiscovery.create(catalinaHome, null).discover();
        return result.getCatalinaBase();
    }
}
