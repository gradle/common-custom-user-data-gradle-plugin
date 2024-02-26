package com.gradle.ccud.adapters.develocity;

import com.gradle.ccud.adapters.DevelocityAdapter;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.gradle.cuud.adapters.PropertyMockFixtures.mockProperty;
import static com.gradle.cuud.adapters.PropertyMockFixtures.mockPropertyReturning;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevelocityConfigurationAdapterTest {

    private BuildScanConfiguration buildScan;
    private DevelocityConfiguration configuration;
    private DevelocityAdapter adapter;

    @BeforeEach
    void setup() {
        buildScan = mock();
        configuration = mock();
        when(configuration.getBuildScan()).thenReturn(buildScan);
        adapter = DevelocityAdapter.create(configuration);
    }

    @Test
    @DisplayName("can set and retrieve the server value using adapter")
    void testServer() {
        //given
        String server = "https://ge-server.com";
        Property<String> prop = mockPropertyReturning(server);
        when(configuration.getServer()).thenReturn(prop);

        // when
        adapter.setServer(server);

        // then
        verify(prop).set(server);
        assertEquals(server, adapter.getServer());
    }

    @Test
    @DisplayName("can set and retrieve the project ID value using adapter")
    void testProjectId() {
        //given
        String projectId = "awesomeProject";
        Property<String> prop = mockPropertyReturning(projectId);
        when(configuration.getProjectId()).thenReturn(prop);

        // when
        adapter.setProjectId(projectId);

        // then
        verify(prop).set(projectId);
        assertEquals(projectId, adapter.getProjectId());
    }

    @Test
    @DisplayName("can set and retrieve the allowUntrustedServer value using adapter")
    void testAllowUntrustedServer() {
        // given
        Property<Boolean> prop = mockPropertyReturning(false);
        when(configuration.getAllowUntrustedServer()).thenReturn(prop);

        // when
        adapter.setAllowUntrustedServer(true);

        // then
        verify(prop).set(true);
        assertFalse(adapter.getAllowUntrustedServer());
    }

    @Test
    @DisplayName("can set and retrieve the access key value using adapter")
    void testAccessKey() {
        // given
        String accessKey = "key";
        Property<String> prop = mockPropertyReturning(accessKey);
        when(configuration.getAccessKey()).thenReturn(prop);

        // when
        adapter.setAccessKey(accessKey);

        // then
        verify(prop).set(accessKey);
        assertEquals(accessKey, adapter.getAccessKey());
    }

    @Test
    @DisplayName("can retrieve the build cache class using adapter")
    void testBuildCache() {
        // when
        adapter.getBuildCache();

        // then
        verify(configuration).getBuildCache();
    }

    @Test
    @DisplayName("can configure the build scan extension using an action")
    void testBuildScanAction() {
        // given
        when(buildScan.getUploadInBackground()).thenReturn(mockProperty());
        when(buildScan.getTermsOfServiceUrl()).thenReturn(mockProperty());


        // when
        adapter.buildScan(buildScan -> {
            buildScan.setUploadInBackground(true);
            buildScan.setTermsOfServiceUrl("server");
        });

        // then
        verify(buildScan.getUploadInBackground()).set(true);
        verify(buildScan.getTermsOfServiceUrl()).set("server");
    }

}
