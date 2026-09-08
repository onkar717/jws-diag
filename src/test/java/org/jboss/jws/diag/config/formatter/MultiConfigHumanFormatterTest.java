package org.jboss.jws.diag.config.formatter;

import org.jboss.jws.diag.config.model.InstanceConfigResult;
import org.jboss.jws.diag.config.model.MultiConfigReport;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultiConfigHumanFormatterTest {

    private static final Path BASE_A = Path.of("/opt/jws-6.0/standalone");
    private static final Path BASE_B = Path.of("/opt/jws-5.7/standalone");

    private final MultiConfigHumanFormatter formatter = new MultiConfigHumanFormatter();

    @Test
    void header_showsInstanceCount() {
        MultiConfigReport report = new MultiConfigReport(2, List.of(
                result(1234, BASE_A),
                result(5678, BASE_B)
        ));

        String out = formatter.format(report);

        assertThat(out).contains("Auto-discovered 2 instance(s).");
    }

    @Test
    void eachInstance_showsPidAndBase() {
        MultiConfigReport report = new MultiConfigReport(2, List.of(
                result(1234, BASE_A),
                result(5678, BASE_B)
        ));

        String out = formatter.format(report);

        assertThat(out).contains("Instance PID 1234");
        assertThat(out).contains(BASE_A.toString().replace('\\', '/'));
        assertThat(out).contains("Instance PID 5678");
        assertThat(out).contains(BASE_B.toString().replace('\\', '/'));
    }

    @Test
    void eachInstance_separatedByRuleLine() {
        MultiConfigReport report = new MultiConfigReport(2, List.of(
                result(1234, BASE_A),
                result(5678, BASE_B)
        ));

        String out = formatter.format(report);

        long ruleCount = out.lines()
                .filter(l -> l.startsWith("─"))
                .count();
        assertThat(ruleCount).isEqualTo(2);
    }

    @Test
    void singleInstance_oneRuleLine() {
        MultiConfigReport report = new MultiConfigReport(1, List.of(result(1234, BASE_A)));

        String out = formatter.format(report);

        long ruleCount = out.lines()
                .filter(l -> l.startsWith("─"))
                .count();
        assertThat(ruleCount).isEqualTo(1);
    }

    private InstanceConfigResult result(int pid, Path base) {
        ServerConfig config = ServerConfig.builder().build();
        return new InstanceConfigResult(pid, base, config);
    }
}
