package org.jboss.jws.diag.summary.formatter;

import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.MultiSummaryReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultiSummaryHumanFormatterTest {

    private static final Path BASE_A = Path.of("/opt/jws-6.0/standalone");
    private static final Path BASE_B = Path.of("/opt/jws-5.7/standalone");

    private final MultiSummaryHumanFormatter formatter = new MultiSummaryHumanFormatter();

    @Test
    void header_showsInstanceCount() {
        MultiSummaryReport report = reportWith(List.of(
                installation(1234, BASE_A),
                installation(5678, BASE_B)
        ));

        String out = formatter.format(report);

        assertThat(out).contains("Auto-discovered 2 instance(s).");
    }

    @Test
    void eachInstance_showsPidAndBase() {
        MultiSummaryReport report = reportWith(List.of(
                installation(1234, BASE_A),
                installation(5678, BASE_B)
        ));

        String out = formatter.format(report);

        assertThat(out).contains("Instance PID 1234");
        assertThat(out).contains(BASE_A.toString().replace('\\', '/'));
        assertThat(out).contains("Instance PID 5678");
        assertThat(out).contains(BASE_B.toString().replace('\\', '/'));
    }

    @Test
    void eachInstance_containsVersionLine() {
        MultiSummaryReport report = reportWith(List.of(
                installation(1234, BASE_A)
        ));

        String out = formatter.format(report);

        assertThat(out).contains("Tomcat 10.1.49");
    }

    @Test
    void singleInstance_noTrailingSeparator() {
        MultiSummaryReport report = reportWith(List.of(installation(1234, BASE_A)));

        String out = formatter.format(report);

        // Only one rule line should appear (before the single instance)
        long ruleCount = out.lines()
                .filter(l -> l.startsWith("─"))
                .count();
        assertThat(ruleCount).isEqualTo(1);
    }

    @Test
    void multipleInstances_eachSeparated() {
        MultiSummaryReport report = reportWith(List.of(
                installation(1234, BASE_A),
                installation(5678, BASE_B)
        ));

        String out = formatter.format(report);

        long ruleCount = out.lines()
                .filter(l -> l.startsWith("─"))
                .count();
        assertThat(ruleCount).isEqualTo(2);
    }

    private MultiSummaryReport reportWith(List<JwsInstallation> installations) {
        return new MultiSummaryReport(installations.size(), installations);
    }

    private JwsInstallation installation(int pid, Path base) {
        return JwsInstallation.builder()
                .pid(pid)
                .catalinaBase(base)
                .catalinaHome(base.getParent())
                .tomcatVersion("10.1.49")
                .build();
    }
}
