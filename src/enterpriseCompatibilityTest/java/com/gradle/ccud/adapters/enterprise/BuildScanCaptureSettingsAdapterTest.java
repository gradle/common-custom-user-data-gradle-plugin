package com.gradle.ccud.adapters.enterprise;

import com.gradle.ccud.adapters.BuildScanCaptureAdapter;
import com.gradle.ccud.adapters.enterprise.proxies.BuildScanCaptureSettingsProxy;
import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.scan.plugin.BuildScanCaptureSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildScanCaptureSettingsAdapterTest {

    private BuildScanCaptureSettings capture;
    private BuildScanCaptureAdapter adapter;

    @BeforeEach
    void setup() {
        capture = mock();
        adapter = new BuildScanCaptureSettingsAdapter(ProxyFactory.createProxy(capture, BuildScanCaptureSettingsProxy.class));
    }

    @Test
    @DisplayName("can capture build logging value using adapter")
    void testBuildLogging() {
        // when
        adapter.setBuildLogging(true);

        // then
        verify(capture).setBuildLogging(true);

        // when
        when(capture.isBuildLogging()).thenReturn(true);

        // then
        assertTrue(adapter.isBuildLogging());
    }

    @Test
    @DisplayName("can capture test logging value using adapter")
    void testTestLogging() {
        // when
        adapter.setTestLogging(true);

        // then
        verify(capture).setTestLogging(true);

        // when
        when(capture.isTestLogging()).thenReturn(true);

        // then
        assertTrue(adapter.isTestLogging());
    }

    @Test
    @DisplayName("can capture task input files value using adapter")
    void testTaskInputFiles() {
        // when
        adapter.setFileFingerprints(true);

        // then
        verify(capture).setTaskInputFiles(true);

        // when
        when(capture.isTaskInputFiles()).thenReturn(true);

        // then
        assertTrue(adapter.isFileFingerprints());
    }

}
