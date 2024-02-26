package com.gradle.ccud.adapters.develocity;

import com.gradle.ccud.adapters.BuildCacheAdapter;
import com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache;
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
public class DevelocityBuildCacheAdapterTest {

    private DevelocityBuildCache cache;
    private BuildCacheAdapter adapter;

    @BeforeEach
    void setup() {
        cache = mock();
        adapter = BuildCacheAdapter.create(cache, DevelocityBuildCache.class);
    }

    @Test
    @DisplayName("can set and retrieve the server value using adapter")
    void testServer() {
        //given
        String server = "https://ge-server.com";

        // when
        adapter.setServer(server);

        // then
        verify(cache).setServer(server);

        // when
        when(cache.getServer()).thenReturn(server);

        // then
        assertEquals(server, adapter.getServer());
    }

    @Test
    @DisplayName("can set and retrieve the cache path using adapter")
    void testPath() {
        //given
        String path = "path";

        // when
        adapter.setPath(path);

        // then
        verify(cache).setPath(path);

        // when
        when(cache.getPath()).thenReturn(path);

        // then
        assertEquals(path, adapter.getPath());
    }

    @Test
    @DisplayName("can set and retrieve the allowUntrustedServer value using adapter")
    void testAllowUntrustedServer() {
        // when
        adapter.setAllowUntrustedServer(true);

        // then
        verify(cache).setAllowUntrustedServer(true);

        // when
        when(cache.getAllowUntrustedServer()).thenReturn(true);

        // then
        assertTrue(adapter.getAllowUntrustedServer());
    }

    @Test
    @DisplayName("can set and retrieve the allowInsecureProtocol value using adapter")
    void testAllowInsecureProtocol() {
        // when
        adapter.setAllowInsecureProtocol(true);

        // then
        verify(cache).setAllowInsecureProtocol(true);

        // when
        when(cache.getAllowInsecureProtocol()).thenReturn(true);

        // then
        assertTrue(adapter.getAllowInsecureProtocol());
    }

    @Test
    @DisplayName("can set the username and password value using adapter")
    void testUsernameAndPassword() {
        // given
        String username = "user";
        String password = "pass";

        // when
        adapter.usernameAndPassword(username, password);

        // then
        verify(cache).usernameAndPassword(username, password);
    }

    @Test
    @DisplayName("can set and retrieve the useExpectContinue value using adapter")
    void testUseExpectContinue() {
        // when
        adapter.setUseExpectContinue(true);

        // then
        verify(cache).setUseExpectContinue(true);

        // when
        when(cache.getUseExpectContinue()).thenReturn(true);

        // then
        assertTrue(adapter.getUseExpectContinue());
    }
}
