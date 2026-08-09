package org.jboss.jws.diag;

import org.jboss.jws.diag.bundle.BundleCommand;
import org.jboss.jws.diag.config.ConfigCommand;
import org.jboss.jws.diag.diff.DiffCommand;
import org.jboss.jws.diag.instances.InstancesCommand;
import org.jboss.jws.diag.logs.LogsCommand;
import org.jboss.jws.diag.modcluster.ModClusterCommand;
import org.jboss.jws.diag.summary.SummaryCommand;
import org.jboss.jws.diag.validate.ValidateCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "jws-diag",
        description = "Diagnostic and configuration validation toolkit for JBoss Web Server / Apache Tomcat",
        mixinStandardHelpOptions = true,
        version = "jws-diag 0.1.0-SNAPSHOT",
        subcommands = {
                SummaryCommand.class,
                ConfigCommand.class,
                ValidateCommand.class,
                BundleCommand.class,
                LogsCommand.class,
                InstancesCommand.class,
                ModClusterCommand.class,
                DiffCommand.class
        }
)
public class Main implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
