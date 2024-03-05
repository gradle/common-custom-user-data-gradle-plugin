package com.gradle.ccud.adapters.enterprise;

import com.gradle.ccud.adapters.BuildResultAdapter;
import com.gradle.ccud.adapters.DevelocityAdapter;
import com.gradle.ccud.adapters.PublishedBuildScanAdapter;
import com.gradle.cuud.adapters.ActionMockFixtures.ArgCapturingAction;
import com.gradle.scan.plugin.BuildScanExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static com.gradle.cuud.adapters.ActionMockFixtures.doExecuteActionWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BuildScanExtension_1_X_AdapterTest {

    private BuildScanExtension extension;
    private DevelocityAdapter adapter;

    @BeforeEach
    void setup() {
        extension = mock();
        // bypass Gradle version check
        adapter = new BuildScanExtension_1_X_Adapter(extension);
    }

    @Test
    @DisplayName("can set the server but not retrieve it")
    void testServer() {
        // given
        String server = "https://irrelevant.com";

        // when
        adapter.setServer(server);

        // then
        verify(extension).setServer(server);
        assertNull(adapter.getServer());
    }

    @Test
    @DisplayName("configuring project ID is not supported")
    void testProjectId() {
        // when
        adapter.setProjectId("irrelevant");
        adapter.getProjectId();

        // then
        verifyNoInteractions(extension);
    }

    @Test
    @DisplayName("can set the allow untrusted property but not retrieve it")
    void testAllowUntrusted() {
        // when
        adapter.setAllowUntrustedServer(true);

        // then
        verify(extension).setAllowUntrustedServer(true);
        assertFalse(adapter.getAllowUntrustedServer());
    }

    @Test
    @DisplayName("configuring access key is not supported")
    void testAccessKey() {
        // when
        adapter.setAccessKey("irrelevant");
        adapter.getAccessKey();

        // then
        verifyNoInteractions(extension);
    }

    @Test
    @DisplayName("build cache class is not available")
    void testBuildCache() {
        // expect
        assertNull(adapter.getBuildCache());
        verifyNoInteractions(extension);
    }

    @Test
    @DisplayName("tags can be set via an adapter")
    void testTag() {
        //given
        String tag = "tag";

        // when
        adapter.getBuildScan().tag(tag);

        // then
        verify(extension).tag(tag);
    }

    @Test
    @DisplayName("values can be set via an adapter")
    void testValue() {
        //given
        String name = "name";
        String value = "value";

        // when
        adapter.getBuildScan().value(name, value);

        // then
        verify(extension).value(name, value);
    }

    @Test
    @DisplayName("links can be set via an adapter")
    void testLink() {
        //given
        String name = "name";
        String value = "https://value.com";

        // when
        adapter.getBuildScan().link(name, value);

        // then
        verify(extension).link(name, value);
    }

    @Test
    @DisplayName("terms of service URL can be set via an adapter but not retrieved")
    void testTermsOfServiceUrl() {
        //given
        String value = "https://value.com";

        // when
        adapter.getBuildScan().setTermsOfServiceUrl(value);

        // then
        verify(extension).setTermsOfServiceUrl(value);

        // and
        assertNull(adapter.getBuildScan().getTermsOfServiceUrl());
    }

    @Test
    @DisplayName("terms of service agreement can be set via an adapter but not retrieved")
    void testTermsOfServiceAgree() {
        //given
        String value = "yes";

        // when
        adapter.getBuildScan().setTermsOfServiceAgree(value);

        // then
        verify(extension).setTermsOfServiceAgree(value);
        assertNull(adapter.getBuildScan().getTermsOfServiceAgree());
    }

    @Test
    @DisplayName("background upload cannot be configured")
    void testUploadInBackground() {
        // when
        adapter.getBuildScan().setUploadInBackground(true);

        // then
        assertFalse(adapter.getBuildScan().isUploadInBackground());
        verifyNoInteractions(extension);
    }

    @Test
    @DisplayName("multiple properties can be configured via buildScan action")
    void testBuildScanAction() {
        // when
        adapter.buildScan(b -> {
            b.setTermsOfServiceUrl("value");
            b.setTermsOfServiceAgree("yes");
        });

        // then
        verify(extension).setTermsOfServiceUrl("value");
        verify(extension).setTermsOfServiceAgree("yes");
    }

    @Test
    @DisplayName("background action can be configured via an adapter")
    void testBackgroundAction() {
        //given
        doExecuteActionWith(extension).when(extension).background(any());

        // when
        adapter.getBuildScan().background(b -> {
            b.setTermsOfServiceUrl("value");
            b.setTermsOfServiceAgree("yes");
        });

        // then
        verify(extension).setTermsOfServiceUrl("value");
        verify(extension).setTermsOfServiceAgree("yes");
    }

    @Test
    @DisplayName("build finished action can be configured via an adapter using the new build result model")
    void testBuildFinishedAction() {
        // given
        Throwable failure = new RuntimeException("Old build failure!");
        com.gradle.scan.plugin.BuildResult buildResult = mock();
        when(buildResult.getFailure()).thenReturn(failure);

        // and
        doExecuteActionWith(buildResult).when(extension).buildFinished(any());

        // when
        ArgCapturingAction<BuildResultAdapter> capturedNewBuildResult = new ArgCapturingAction<>();
        adapter.getBuildScan().buildFinished(capturedNewBuildResult);

        // then
        assertEquals(Collections.singletonList(failure), capturedNewBuildResult.getValue().getFailures());
    }

    @Test
    @DisplayName("build scan published action can be configured via an adapter using the new scan model")
    void testBuildScanPublishedAction() {
        // given
        com.gradle.scan.plugin.PublishedBuildScan publishedScan = mock();
        when(publishedScan.getBuildScanId()).thenReturn("scanId");
        doExecuteActionWith(publishedScan).when(extension).buildScanPublished(any());

        // when
        ArgCapturingAction<PublishedBuildScanAdapter> capturedPublishedBuildScan = new ArgCapturingAction<>();
        adapter.getBuildScan().buildScanPublished(capturedPublishedBuildScan);

        // then
        assertEquals("scanId", capturedPublishedBuildScan.getValue().getBuildScanId());
    }

    @Test
    @DisplayName("publishing can be configured on build scan extension via an adapter")
    void testPublishing() {
        // when
        adapter.getBuildScan().publishAlways();
        adapter.getBuildScan().publishAlwaysIf(false);
        adapter.getBuildScan().publishOnFailure();
        adapter.getBuildScan().publishOnFailureIf(false);

        // then
        verify(extension).publishAlways();
        verify(extension).publishAlwaysIf(false);
        verify(extension).publishOnFailure();
        verify(extension).publishOnFailureIf(false);
    }

    @Test
    @DisplayName("obfuscation cannot be configured")
    void testObfuscation() {
        // expect
        assertNull(adapter.getBuildScan().getObfuscation());
    }

    @Test
    @DisplayName("capture cannot be configured")
    void testCapture() {
        // expect
        assertNull(adapter.getBuildScan().getCapture());
    }
}
