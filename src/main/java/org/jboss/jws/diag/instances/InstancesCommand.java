package org.jboss.jws.diag.instances;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.instances.formatter.InstancesHumanFormatter;
import org.jboss.jws.diag.instances.formatter.InstancesJsonFormatter;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.List;

@Command(name = "instances",
        description = "Detect all running Tomcat/JWS instances on this host by scanning /proc",
        mixinStandardHelpOptions = true)
public class InstancesCommand implements Runnable {

    @Mixin
    private OutputFormatMixin outputFormat;

    @Override
    public void run() {
        List<TomcatInstance> instances = new InstanceScanner().scan();

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new InstancesJsonFormatter().format(instances);
        } else {
            output = new InstancesHumanFormatter().format(instances);
        }

        System.out.println(output);
        System.exit(instances.isEmpty() ? ExitCodes.WARNINGS : ExitCodes.OK);
    }
}
