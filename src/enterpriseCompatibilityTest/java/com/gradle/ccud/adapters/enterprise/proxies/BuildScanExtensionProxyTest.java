package com.gradle.ccud.adapters.enterprise.proxies;

import com.gradle.ccud.adapters.reflection.ProxyFactory;
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
class BuildScanExtensionProxyTest {

    private BuildScanExtension extension;
    private BuildScanExtensionProxy proxy;

    @BeforeEach
    void setup() {
        extension = mock();
        proxy = ProxyFactory.createProxy(extension, BuildScanExtensionProxy.class);
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

}
