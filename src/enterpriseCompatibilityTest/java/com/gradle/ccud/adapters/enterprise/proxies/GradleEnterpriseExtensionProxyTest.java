package com.gradle.ccud.adapters.enterprise.proxies;

import com.gradle.ccud.adapters.reflection.ProxyFactory;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.scan.plugin.BuildScanExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradleEnterpriseExtensionProxyTest {

    private GradleEnterpriseExtension extension;
    private GradleEnterpriseExtensionProxy proxy;

    @BeforeEach
    void setup() {
        extension = mock();
        proxy = ProxyFactory.createProxy(extension, GradleEnterpriseExtensionProxy.class);
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
    @DisplayName("can set and retrieve the project ID value using proxy")
    void testProjectId() {
        //given
        String projectId = "awesomeProject";

        // when
        proxy.setProjectId(projectId);

        // then
        verify(extension).setProjectId(projectId);

        // when
        when(extension.getProjectId()).thenReturn(projectId);

        // then
        assertEquals(projectId, proxy.getProjectId());
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
    @DisplayName("can set and retrieve the access key value using proxy")
    void testAccessKey() {
        // given
        String accessKey = "key";

        // when
        proxy.setAccessKey(accessKey);

        // then
        verify(extension).setAccessKey(accessKey);

        // when
        when(extension.getAccessKey()).thenReturn(accessKey);

        // then
        assertEquals(accessKey, proxy.getAccessKey());
    }

    @Test
    @DisplayName("can retrieve the build cache class using proxy")
    void testBuildCache() {
        // when
        proxy.getBuildCache();

        // then
        verify(extension).getBuildCache();
    }

    @Test
    @DisplayName("can configure the build scan extension using an action")
    void testBuildScanAction() {
        // given
        BuildScanExtension buildScanExtension = mock();
        when(extension.getBuildScan()).thenReturn(buildScanExtension);

        // when
        proxy.buildScan(buildScan -> {
            buildScan.setAllowUntrustedServer(true);
            buildScan.setServer("server");
        });

        // then
        verify(buildScanExtension).setAllowUntrustedServer(true);
        verify(buildScanExtension).setServer("server");
    }

}
