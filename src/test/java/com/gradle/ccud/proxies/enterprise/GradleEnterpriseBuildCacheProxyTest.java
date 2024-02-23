package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyFactory;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseBuildCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GradleEnterpriseBuildCacheProxyTest extends BaseProxyTest {

    private GradleEnterpriseBuildCache cache;
    private GradleEnterpriseBuildCacheProxy proxy;

    @BeforeEach
    void setup() {
        cache = mock();
        proxy = ProxyFactory.createProxy(cache, GradleEnterpriseBuildCacheProxy.class);
    }

    @Test
    @DisplayName("can set and retrieve the server value using proxy")
    void testServer() {
        //given
        String server = "https://ge-server.com";

        // when
        proxy.setServer(server);

        // then
        verify(cache).setServer(server);

        // when
        when(cache.getServer()).thenReturn(server);

        // then
        assertEquals(server, proxy.getServer());
    }

    @Test
    @DisplayName("can set and retrieve the cache path using proxy")
    void testPath() {
        //given
        String path = "path";

        // when
        proxy.setPath(path);

        // then
        verify(cache).setPath(path);

        // when
        when(cache.getPath()).thenReturn(path);

        // then
        assertEquals(path, proxy.getPath());
    }

    @Test
    @DisplayName("can set and retrieve the allowUntrustedServer value using proxy")
    void testAllowUntrustedServer() {
        // when
        proxy.setAllowUntrustedServer(true);

        // then
        verify(cache).setAllowUntrustedServer(true);

        // when
        when(cache.getAllowUntrustedServer()).thenReturn(true);

        // then
        assertTrue(proxy.getAllowUntrustedServer());
    }

    @Test
    @DisplayName("can set and retrieve the allowInsecureProtocol value using proxy")
    void testAllowInsecureProtocol() {
        // when
        proxy.setAllowInsecureProtocol(true);

        // then
        verify(cache).setAllowInsecureProtocol(true);

        // when
        when(cache.getAllowInsecureProtocol()).thenReturn(true);

        // then
        assertTrue(proxy.getAllowInsecureProtocol());
    }

    @Test
    @DisplayName("can set the username and password value using proxy")
    void testUsernameAndPassword() {
        // given
        String username = "user";
        String password = "pass";

        // when
        proxy.usernameAndPassword(username, password);

        // then
        verify(cache).usernameAndPassword(username, password);
    }

    @Test
    @DisplayName("can set and retrieve the useExpectContinue value using proxy")
    void testUseExpectContinue() {
        // when
        proxy.setUseExpectContinue(true);

        // then
        verify(cache).setUseExpectContinue(true);

        // when
        when(cache.getUseExpectContinue()).thenReturn(true);

        // then
        assertTrue(proxy.getUseExpectContinue());
    }
}
