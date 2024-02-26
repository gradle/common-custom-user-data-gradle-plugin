package com.gradle.ccud.adapters.enterprise.proxies;

import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.scan.plugin.BuildScanCaptureSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildScanCaptureSettingsProxyTest extends BaseProxyTest {

    private BuildScanCaptureSettings capture;
    private BuildScanCaptureSettingsProxy proxy;

    @BeforeEach
    void setup() {
        capture = mock();
        proxy = ProxyFactory.createProxy(capture, BuildScanCaptureSettingsProxy.class);
    }

    @Test
    @DisplayName("can capture build logging value using proxy")
    void testBuildLogging() {
        // when
        proxy.setBuildLogging(true);

        // then
        verify(capture).setBuildLogging(true);

        // when
        when(capture.isBuildLogging()).thenReturn(true);

        // then
        assertTrue(proxy.isBuildLogging());
    }

    @Test
    @DisplayName("can capture test logging value using proxy")
    void testTestLogging() {
        // when
        proxy.setTestLogging(true);

        // then
        verify(capture).setTestLogging(true);

        // when
        when(capture.isTestLogging()).thenReturn(true);

        // then
        assertTrue(proxy.isTestLogging());
    }

    @Test
    @DisplayName("can capture task input files value using proxy")
    void testTaskInputFiles() {
        // when
        proxy.setTaskInputFiles(true);

        // then
        verify(capture).setTaskInputFiles(true);

        // when
        when(capture.isTaskInputFiles()).thenReturn(true);

        // then
        assertTrue(proxy.isTaskInputFiles());
    }

}
