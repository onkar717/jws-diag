package org.jboss.jws.diag.diff;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.parser.PropertyResolver;
import org.jboss.jws.diag.config.parser.ServerXmlParser;
import org.jboss.jws.diag.diff.formatter.DiffHumanFormatter;
import org.jboss.jws.diag.diff.formatter.DiffJsonFormatter;
import org.jboss.jws.diag.diff.model.DiffReport;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "diff",
        description = "Compare effective server.xml configuration between two CATALINA_BASE directories",
        mixinStandardHelpOptions = true)
public class DiffCommand implements Runnable {

    @Option(names = "--left",
            required = true,
            description = "Path to the first CATALINA_BASE directory (or its server.xml)")
    private Path left;

    @Option(names = "--right",
            required = true,
            description = "Path to the second CATALINA_BASE directory (or its server.xml)")
    private Path right;

    @Mixin
    private OutputFormatMixin outputFormat;

    @Override
    public void run() {
        Path leftXml = resolveServerXml("--left", left);
        Path rightXml = resolveServerXml("--right", right);
        if (leftXml == null || rightXml == null) {
            System.exit(ExitCodes.ERRORS);
            return;
        }

        ServerConfig leftConfig = parse(leftXml, left);
        ServerConfig rightConfig = parse(rightXml, right);
        if (leftConfig == null || rightConfig == null) {
            System.exit(ExitCodes.ERRORS);
            return;
        }

        DiffReport report = new ConfigDiffer().diff(left, right, leftConfig, rightConfig);

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new DiffJsonFormatter().format(report);
        } else {
            output = new DiffHumanFormatter().format(report);
        }

        System.out.println(output);
        System.exit(report.hasDifferences() ? ExitCodes.WARNINGS : ExitCodes.OK);
    }

    private Path resolveServerXml(String flag, Path path) {
        if (Files.isRegularFile(path)) {
            return path;
        }
        if (Files.isDirectory(path)) {
            Path xml = path.resolve("conf/server.xml");
            if (Files.exists(xml)) return xml;
            System.err.println("ERROR: server.xml not found under " + flag + ": " + path);
            return null;
        }
        System.err.println("ERROR: " + flag + " is not a directory or server.xml file: " + path);
        return null;
    }

    private ServerConfig parse(Path serverXml, Path base) {
        try {
            Path resolverBase;
            if (Files.isDirectory(base)) {
                resolverBase = base;
            } else {
                Path parent = base.getParent();
                resolverBase = (parent != null) ? parent.getParent() : null;
            }
            PropertyResolver resolver = PropertyResolver.create(resolverBase);
            return new ServerXmlParser(resolver).parse(serverXml);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to parse " + serverXml + ": " + e.getMessage());
            return null;
        }
    }
}
