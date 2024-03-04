package com.gradle.ccud.adapters.enterprise;

import com.gradle.ccud.adapters.BuildResultAdapter;
import com.gradle.ccud.adapters.BuildScanAdapter;
import com.gradle.ccud.adapters.PublishedBuildScanAdapter;
import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.cuud.adapters.ActionMockFixtures;
import com.gradle.cuud.adapters.ActionMockFixtures.ArgCapturingAction;
import com.gradle.scan.plugin.BuildResult;
import com.gradle.scan.plugin.BuildScanCaptureSettings;
import com.gradle.scan.plugin.BuildScanDataObfuscation;
import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.PublishedBuildScan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static com.gradle.cuud.adapters.ActionMockFixtures.doExecuteActionWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildScanExtensionAdapterTest {

    private BuildScanExtension extension;
    private BuildScanAdapter adapter;

    @BeforeEach
    void setup() {
        extension = mock();
        adapter = new BuildScanExtensionAdapter(ProxyFactory.createProxy(extension, BuildScanExtension.class));
    }

    @Test
    @DisplayName("can set tags using the extension adapter")
    void testTag() {
        // when
        adapter.tag("tag");

        // then
        verify(extension).tag("tag");
    }

    @Test
    @DisplayName("can set custom values using the extension adapter")
    void testValue() {
        // when
        adapter.value("name", "value");

        // then
        verify(extension).value("name", "value");
    }

    @Test
    @DisplayName("can set custom links using the extension adapter")
    void testLink() {
        // when
        adapter.link("name", "value");

        // then
        verify(extension).link("name", "value");
    }

    @Test
    @DisplayName("can set and retrieve the terms of service URL using the extension adapter")
    void testTermsOfServiceUrl() {
        // given
        String url = "https://terms-of-service.com";

        // when
        adapter.setTermsOfServiceUrl(url);

        // then
        verify(extension).setTermsOfServiceUrl(url);

        // when
        when(extension.getTermsOfServiceUrl()).thenReturn(url);

        // then
        assertEquals(url, adapter.getTermsOfServiceUrl());
    }

    @Test
    @DisplayName("can set and retrieve the terms of service agreement using the extension adapter")
    void testTermsOfServiceAgree() {
        // given
        String agree = "yes";

        // when
        adapter.setTermsOfServiceAgree(agree);

        // then
        verify(extension).setTermsOfServiceAgree(agree);

        // when
        when(extension.getTermsOfServiceAgree()).thenReturn(agree);

        // then
        assertEquals(agree, adapter.getTermsOfServiceAgree());
    }

    @Test
    @DisplayName("can set and retrieve the uploadInBackground value using proxy")
    void testUploadInBackground() {
        // when
        adapter.setUploadInBackground(true);

        // then
        verify(extension).setUploadInBackground(true);

        // when
        when(extension.isUploadInBackground()).thenReturn(true);

        // then
        assertTrue(adapter.isUploadInBackground());
    }

    @Test
    @DisplayName("can configure the build scan publication using proxy")
    void testPublishing() {
        // when
        adapter.publishAlways();
        adapter.publishAlwaysIf(true);
        adapter.publishOnFailure();
        adapter.publishOnFailureIf(false);

        // then
        verify(extension).publishAlways();
        verify(extension).publishAlwaysIf(true);
        verify(extension).publishOnFailure();
        verify(extension).publishOnFailureIf(false);
    }

    @Test
    @DisplayName("can configure the build scan extension using the background action")
    void testBackgroundAction() {
        // given
        doExecuteActionWith(extension).when(extension).background(any());

        // when
        adapter.background(buildScan -> {
            buildScan.setUploadInBackground(true);
            buildScan.setTermsOfServiceUrl("server");
        });

        // then
        verify(extension).setUploadInBackground(true);
        verify(extension).setTermsOfServiceUrl("server");
    }

    @Test
    @DisplayName("can run the build finished action using the proxy")
    void testBuildFinishedAction() {
        // given
        Throwable failure = new RuntimeException("Boom!");
        BuildResult buildResult = mock();
        when(buildResult.getFailure()).thenReturn(failure);
        doExecuteActionWith(buildResult).when(extension).buildFinished(any());

        // when
        ArgCapturingAction<BuildResultAdapter> capturer = new ActionMockFixtures.ArgCapturingAction<>();
        adapter.buildFinished(capturer);

        // then
        assertEquals(Collections.singletonList(failure), capturer.getValue().getFailures());
    }

    @Test
    @DisplayName("can run the build scan published action using the proxy")
    void testBuildScanPublishedAction() {
        // given
        PublishedBuildScan scan = mock();
        when(scan.getBuildScanId()).thenReturn("scanId");
        doExecuteActionWith(scan).when(extension).buildScanPublished(any());

        // when
        ArgCapturingAction<PublishedBuildScanAdapter> capturer = new ArgCapturingAction<>();
        adapter.buildScanPublished(capturer);

        // then
        assertEquals("scanId", capturer.getValue().getBuildScanId());
    }

    @Test
    @DisplayName("can configure the data obfuscation using an action")
    void testObfuscationAction() {
        // given
        BuildScanDataObfuscation obfuscation = mock();
        when(extension.getObfuscation()).thenReturn(obfuscation);

        // and
        adapter = new BuildScanExtensionAdapter(ProxyFactory.createProxy(extension, BuildScanExtension.class));

        // when
        adapter.obfuscation(o -> {
            o.hostname(it -> "<obfuscated>");
            o.username(it -> "<obfuscated>");
        });

        // then
        verify(obfuscation).hostname(any());
        verify(obfuscation).username(any());
    }

    @Test
    @DisplayName("can configure the data capturing using an action")
    void testCaptureAction() {
        // given
        BuildScanCaptureSettings capture = mock();
        when(extension.getCapture()).thenReturn(capture);

        // and
        adapter = new BuildScanExtensionAdapter(ProxyFactory.createProxy(extension, BuildScanExtension.class));

        // when
        adapter.capture(c -> {
            c.setBuildLogging(true);
            c.setTestLogging(false);
        });

        // then
        verify(capture).setBuildLogging(true);
        verify(capture).setTestLogging(false);
    }

}
