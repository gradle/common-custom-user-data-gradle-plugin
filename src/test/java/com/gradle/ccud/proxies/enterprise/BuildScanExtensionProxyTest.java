package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyFactory;
import com.gradle.scan.plugin.BuildResult;
import com.gradle.scan.plugin.BuildScanCaptureSettings;
import com.gradle.scan.plugin.BuildScanDataObfuscation;
import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.PublishedBuildScan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildScanExtensionProxyTest extends BaseProxyTest {

    private BuildScanExtension extension;
    private BuildScanExtensionProxy proxy;

    @BeforeEach
    void setup() {
        extension = mock();
        proxy = ProxyFactory.createProxy(extension, BuildScanExtensionProxy.class);
    }

    @Test
    @DisplayName("can set tags using the extension proxy")
    void testTag() {
        // when
        proxy.tag("tag");

        // then
        verify(extension).tag("tag");
    }

    @Test
    @DisplayName("can set custom values using the extension proxy")
    void testValue() {
        // when
        proxy.value("name", "value");

        // then
        verify(extension).value("name", "value");
    }

    @Test
    @DisplayName("can set custom links using the extension proxy")
    void testLink() {
        // when
        proxy.link("name", "value");

        // then
        verify(extension).link("name", "value");
    }

    @Test
    @DisplayName("can set and retrieve the terms of service URL using the extension proxy")
    void testTermsOfServiceUrl() {
        // given
        String url = "https://terms-of-service.com";

        // when
        proxy.setTermsOfServiceUrl(url);

        // then
        verify(extension).setTermsOfServiceUrl(url);

        // when
        when(extension.getTermsOfServiceUrl()).thenReturn(url);

        // then
        assertEquals(url, proxy.getTermsOfServiceUrl());
    }

    @Test
    @DisplayName("can set and retrieve the terms of service agreement using the extension proxy")
    void testTermsOfServiceAgree() {
        // given
        String agree = "yes";

        // when
        proxy.setTermsOfServiceAgree(agree);

        // then
        verify(extension).setTermsOfServiceAgree(agree);

        // when
        when(extension.getTermsOfServiceAgree()).thenReturn(agree);

        // then
        assertEquals(agree, proxy.getTermsOfServiceAgree());
    }

    @Test
    @DisplayName("can set and retrieve the server value using proxy")
    void testServer() {
        //given
        String server = "https://ge-server.com";

        // when
        proxy.setServer(server);

        // then
        verify(extension).setServer(server);

        // when
        when(extension.getServer()).thenReturn(server);

        // then
        assertEquals(server, proxy.getServer());
    }

    @Test
    @DisplayName("can set and retrieve the allowUntrustedServer value using proxy")
    void testAllowUntrustedServer() {
        // when
        proxy.setAllowUntrustedServer(true);

        // then
        verify(extension).setAllowUntrustedServer(true);

        // when
        when(extension.getAllowUntrustedServer()).thenReturn(true);

        // then
        assertTrue(proxy.getAllowUntrustedServer());
    }

    @Test
    @DisplayName("can set and retrieve the uploadInBackground value using proxy")
    void testUploadInBackground() {
        // when
        proxy.setUploadInBackground(true);

        // then
        verify(extension).setUploadInBackground(true);

        // when
        when(extension.isUploadInBackground()).thenReturn(true);

        // then
        assertTrue(proxy.isUploadInBackground());
    }

    @Test
    @DisplayName("can configure the build scan publication using proxy")
    void testPublishing() {
        // when
        proxy.publishAlways();
        proxy.publishAlwaysIf(true);
        proxy.publishOnFailure();
        proxy.publishOnFailureIf(false);

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
        proxy.background(buildScan -> {
            buildScan.setAllowUntrustedServer(true);
            buildScan.setServer("server");
        });

        // then
        verify(extension).setAllowUntrustedServer(true);
        verify(extension).setServer("server");
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
        ArgCapturingAction<BuildResult> capturer = new ArgCapturingAction<>();
        proxy.buildFinished(capturer);

        // then
        assertEquals(failure, capturer.getValue().getFailure());
    }

    @Test
    @DisplayName("can run the build scan published action using the proxy")
    void testBuildScanPublishedAction() {
        // given
        PublishedBuildScan scan = mock();
        when(scan.getBuildScanId()).thenReturn("scanId");
        doExecuteActionWith(scan).when(extension).buildScanPublished(any());

        // when
        ArgCapturingAction<PublishedBuildScan> capturer = new ArgCapturingAction<>();
        proxy.buildScanPublished(capturer);

        // then
        assertEquals("scanId", capturer.getValue().getBuildScanId());
    }

    @Test
    @DisplayName("can configure the data obfuscation using an action")
    void testObfuscationAction() {
        // given
        BuildScanDataObfuscation obfuscation = mock();
        doExecuteActionWith(obfuscation).when(extension).obfuscation(any());

        // when
        proxy.obfuscation(o -> {
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

        // when
        proxy.capture(c -> {
            c.setBuildLogging(true);
            c.setTestLogging(false);
        });

        // then
        verify(capture).setBuildLogging(true);
        verify(capture).setTestLogging(false);
    }

}