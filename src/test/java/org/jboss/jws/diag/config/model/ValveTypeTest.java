package org.jboss.jws.diag.config.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValveTypeTest {

    @Test
    void accessLogValveRecognised() {
        assertThat(ValveType.fromClassName("org.apache.catalina.valves.AccessLogValve"))
                .isEqualTo(ValveType.ACCESS_LOG);
    }

    @Test
    void remoteIpValveRecognised() {
        assertThat(ValveType.fromClassName("org.apache.catalina.valves.RemoteIpValve"))
                .isEqualTo(ValveType.REMOTE_IP);
    }

    @Test
    void stuckThreadValveRecognised() {
        assertThat(ValveType.fromClassName("org.apache.catalina.valves.StuckThreadDetectionValve"))
                .isEqualTo(ValveType.STUCK_THREAD);
    }

    @Test
    void errorReportValveRecognised() {
        assertThat(ValveType.fromClassName("org.apache.catalina.valves.ErrorReportValve"))
                .isEqualTo(ValveType.ERROR_REPORT);
    }

    @Test
    void unknownClassMapsToUnknown() {
        assertThat(ValveType.fromClassName("com.example.CustomValve"))
                .isEqualTo(ValveType.UNKNOWN);
    }

    @Test
    void nullClassMapsToUnknown() {
        assertThat(ValveType.fromClassName(null))
                .isEqualTo(ValveType.UNKNOWN);
    }

    @Test
    void labelsAreDistinctAndNonEmpty() {
        for (ValveType t : ValveType.values()) {
            assertThat(t.getLabel()).isNotBlank();
        }
    }

    @Test
    void valveConfigReturnsNullForUnknownType() {
        ValveConfig valve = ValveConfig.builder()
                .className("com.example.CustomValve")
                .build();
        assertThat(valve.getValveType()).isNull();
    }

    @Test
    void valveConfigReturnsTypeForKnownClass() {
        ValveConfig valve = ValveConfig.builder()
                .className("org.apache.catalina.valves.RemoteIpValve")
                .build();
        assertThat(valve.getValveType()).isEqualTo(ValveType.REMOTE_IP);
    }
}
