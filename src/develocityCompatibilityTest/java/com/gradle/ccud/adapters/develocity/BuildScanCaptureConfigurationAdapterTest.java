package com.gradle.ccud.adapters.develocity;

import com.gradle.ccud.adapters.BuildScanCaptureAdapter;
import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.develocity.agent.gradle.scan.BuildScanCaptureConfiguration;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.gradle.cuud.adapters.PropertyMockFixtures.mockPropertyReturning;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildScanCaptureConfigurationAdapterTest {

    private BuildScanCaptureConfiguration capture;
    private BuildScanCaptureAdapter adapter;

    @BeforeEach
    void setup() {
        capture = mock();
        adapter = new BuildScanCaptureConfigurationAdapter(ProxyFactory.createProxy(capture, BuildScanCaptureConfiguration.class));
    }

    @Test
    @DisplayName("can capture build logging value using adapter")
    void testBuildLogging() {
        // given
        Property<Boolean> prop = mockPropertyReturning(false);
        when(capture.getBuildLogging()).thenReturn(prop);

        // when
        adapter.setBuildLogging(true);

        // then
        verify(prop).set(true);
        assertFalse(adapter.isBuildLogging());
    }

    @Test
    @DisplayName("can capture test logging value using adapter")
    void testTestLogging() {
        // given
        Property<Boolean> prop = mockPropertyReturning(false);
        when(capture.getTestLogging()).thenReturn(prop);

        // when
        adapter.setTestLogging(true);

        // then
        verify(prop).set(true);
        assertFalse(adapter.isTestLogging());
    }

    @Test
    @DisplayName("can capture file fingerprints value using adapter")
    void testFileFingerprints() {
        // given
        Property<Boolean> prop = mockPropertyReturning(false);
        when(capture.getFileFingerprints()).thenReturn(prop);

        // when
        adapter.setFileFingerprints(true);

        // then
        verify(prop).set(true);
        assertFalse(adapter.isFileFingerprints());
    }

}
