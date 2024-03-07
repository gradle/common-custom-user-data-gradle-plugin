package com.gradle.ccud.adapters.develocity;

import com.gradle.ccud.adapters.BuildResultAdapter;
import com.gradle.ccud.adapters.BuildScanAdapter;
import com.gradle.ccud.adapters.PublishedBuildScanAdapter;
import com.gradle.ccud.adapters.ProxyFactory;
import com.gradle.cuud.adapters.ActionMockFixtures.ArgCapturingAction;
import com.gradle.develocity.agent.gradle.scan.BuildResult;
import com.gradle.develocity.agent.gradle.scan.BuildScanCaptureConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanDataObfuscationConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanPublishingConfiguration;
import com.gradle.develocity.agent.gradle.scan.PublishedBuildScan;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static com.gradle.cuud.adapters.ActionMockFixtures.doExecuteActionWith;
import static com.gradle.cuud.adapters.PropertyMockFixtures.mockProperty;
import static com.gradle.cuud.adapters.PropertyMockFixtures.mockPropertyReturning;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BuildScanConfigurationAdapterTest {

    private BuildScanConfiguration configuration;
    private BuildScanAdapter adapter;

    @BeforeEach
    void setup() {
        configuration = mock();
        adapter = new BuildScanConfigurationAdapter(ProxyFactory.createProxy(configuration, BuildScanConfiguration.class));
    }

    @Test
    @DisplayName("tags can be set via an adapter")
    void testTag() {
        //given
        String tag = "tag";

        // when
        adapter.tag(tag);

        // then
        verify(configuration).tag(tag);
    }

    @Test
    @DisplayName("values can be set via an adapter")
    void testValue() {
        //given
        String name = "name";
        String value = "value";

        // when
        adapter.value(name, value);

        // then
        verify(configuration).value(name, value);
    }

    @Test
    @DisplayName("links can be set via an adapter")
    void testLink() {
        //given
        String name = "name";
        String value = "https://value.com";

        // when
        adapter.link(name, value);

        // then
        verify(configuration).link(name, value);
    }

    @Test
    @DisplayName("terms of service URL can be set via an adapter")
    void testTermsOfServiceUrl() {
        //given
        String value = "https://value.com";
        Property<String> prop = mockPropertyReturning(value);
        when(configuration.getTermsOfUseUrl()).thenReturn(prop);

        // when
        adapter.setTermsOfUseUrl(value);

        // then
        verify(prop).set(value);
        assertEquals(value, adapter.getTermsOfUseUrl());
    }

    @Test
    @DisplayName("terms of service agreement can be set via an adapter")
    void testTermsOfServiceAgree() {
        //given
        String value = "no";
        Property<String> prop = mockPropertyReturning(value);
        when(configuration.getTermsOfUseAgree()).thenReturn(prop);

        // when
        adapter.setTermsOfUseAgree(value);

        // then
        verify(prop).set(value);
        assertEquals(value, adapter.getTermsOfUseAgree());
    }

    @Test
    @DisplayName("background upload can be set via an adapter")
    void testUploadInBackground() {
        //given
        Property<Boolean> backgroundUploadProp = mockPropertyReturning(false);
        when(configuration.getUploadInBackground()).thenReturn(backgroundUploadProp);

        // when
        adapter.setUploadInBackground(false);

        // then
        verify(configuration.getUploadInBackground()).set(false);

        // when
        adapter.isUploadInBackground();

        // then
        verify(configuration.getUploadInBackground()).get();
    }

    @Test
    @DisplayName("background action can be configured via an adapter")
    void testBackgroundAction() {
        //given
        doExecuteActionWith(configuration).when(configuration).background(any());

        // and
        when(configuration.getTermsOfUseUrl()).thenReturn(mockProperty());
        when(configuration.getTermsOfUseAgree()).thenReturn(mockProperty());

        // when
        adapter.background(b -> {
            b.setTermsOfUseUrl("other url");
            b.setTermsOfUseAgree("no");
        });

        // then
        verify(configuration.getTermsOfUseUrl()).set("other url");
        verify(configuration.getTermsOfUseAgree()).set("no");
    }

    @Test
    @DisplayName("build finished action can be configured via an adapter using the new build result model")
    void testBuildFinishedAction() {
        // given
        Throwable failure = new RuntimeException("New build failure!");
        BuildResult buildResult = mock();
        when(buildResult.getFailures()).thenReturn(Collections.singletonList(failure));

        // and
        doExecuteActionWith(buildResult).when(configuration).buildFinished(any());

        // when
        ArgCapturingAction<BuildResultAdapter> capturedNewBuildResult = new ArgCapturingAction<>();
        adapter.buildFinished(capturedNewBuildResult);

        // then
        assertEquals(Collections.singletonList(failure), capturedNewBuildResult.getValue().getFailures());
    }

    @Test
    @DisplayName("build scan published action can be configured via an adapter using the new scan model")
    void testBuildScanPublishedAction() {
        // given
        PublishedBuildScan publishedScan = mock();
        when(publishedScan.getBuildScanId()).thenReturn("scanId");
        doExecuteActionWith(publishedScan).when(configuration).buildScanPublished(any());

        // when
        ArgCapturingAction<PublishedBuildScanAdapter> capturedPublishedBuildScan = new ArgCapturingAction<>();
        adapter.buildScanPublished(capturedPublishedBuildScan);

        // then
        assertEquals("scanId", capturedPublishedBuildScan.getValue().getBuildScanId());
    }

    @Test
    @DisplayName("publishing always can be configured on the Develocity configuration via an adapter")
    void testPublishAlwaysDevelocity() {
        // given
        ArgumentCaptor<Spec<? super BuildScanPublishingConfiguration.PublishingContext>> specCaptor = withCapturedPublishingSpec(configuration);

        // when
        adapter.publishAlways();

        // then
        assertTrue(specCaptor.getValue().isSatisfiedBy(publishingCtxForSuccessfulBuild()));
        assertTrue(specCaptor.getValue().isSatisfiedBy(publishingCtxForFailingBuild()));
    }

    @Test
    @DisplayName("conditional publishing always can be configured on the Develocity configuration via an adapter")
    void testPublishAlwaysIfDevelocity() {
        // given
        ArgumentCaptor<Spec<? super BuildScanPublishingConfiguration.PublishingContext>> specCaptor = withCapturedPublishingSpec(configuration);

        // when
        adapter.publishAlwaysIf(true);

        // then
        assertTrue(specCaptor.getValue().isSatisfiedBy(publishingCtxForSuccessfulBuild()));
        assertTrue(specCaptor.getValue().isSatisfiedBy(publishingCtxForFailingBuild()));

        // when
        adapter.publishAlwaysIf(false);

        // then
        assertFalse(specCaptor.getValue().isSatisfiedBy(publishingCtxForSuccessfulBuild()));
        assertFalse(specCaptor.getValue().isSatisfiedBy(publishingCtxForFailingBuild()));
    }

    @Test
    @DisplayName("publishing on failure can be configured on the Develocity configuration via an adapter")
    void testPublishOnFailureDevelocity() {
        // given
        ArgumentCaptor<Spec<? super BuildScanPublishingConfiguration.PublishingContext>> specCaptor = withCapturedPublishingSpec(configuration);

        // when
        adapter.publishOnFailure();

        // then
        assertFalse(specCaptor.getValue().isSatisfiedBy(publishingCtxForSuccessfulBuild()));
        assertTrue(specCaptor.getValue().isSatisfiedBy(publishingCtxForFailingBuild()));
    }

    @Test
    @DisplayName("conditional publishing on failure can be configured on the Develocity configuration via an adapter")
    void testPublishOnFailureIfDevelocity() {
        // given
        ArgumentCaptor<Spec<? super BuildScanPublishingConfiguration.PublishingContext>> specCaptor = withCapturedPublishingSpec(configuration);

        // when
        adapter.publishOnFailureIf(true);

        // then
        assertFalse(specCaptor.getValue().isSatisfiedBy(publishingCtxForSuccessfulBuild()));
        assertTrue(specCaptor.getValue().isSatisfiedBy(publishingCtxForFailingBuild()));

        // when
        adapter.publishOnFailureIf(false);

        // then
        assertFalse(specCaptor.getValue().isSatisfiedBy(publishingCtxForSuccessfulBuild()));
        assertFalse(specCaptor.getValue().isSatisfiedBy(publishingCtxForFailingBuild()));
    }

    @Test
    @DisplayName("can configure the data obfuscation using an action")
    void testObfuscationAction() {
        // given
        BuildScanDataObfuscationConfiguration obfuscation = mock();
        when(configuration.getObfuscation()).thenReturn(obfuscation);

        // and
        adapter = new BuildScanConfigurationAdapter(ProxyFactory.createProxy(configuration, BuildScanConfiguration.class));

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
        Property<Boolean> buildLoggingProp = mockProperty();
        Property<Boolean> testLoggingProp = mockProperty();
        BuildScanCaptureConfiguration capture = mock();
        when(capture.getBuildLogging()).thenReturn(buildLoggingProp);
        when(capture.getTestLogging()).thenReturn(testLoggingProp);
        when(configuration.getCapture()).thenReturn(capture);

        // and
        adapter = new BuildScanConfigurationAdapter(ProxyFactory.createProxy(configuration, BuildScanConfiguration.class));

        // when
        adapter.capture(c -> {
            c.setBuildLogging(true);
            c.setTestLogging(false);
        });

        // then
        verify(buildLoggingProp).set(true);
        verify(testLoggingProp).set(false);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Spec<? super BuildScanPublishingConfiguration.PublishingContext>> withCapturedPublishingSpec(BuildScanConfiguration buildScanConfiguration) {
        ArgumentCaptor<Spec<? super BuildScanPublishingConfiguration.PublishingContext>> specCaptor = ArgumentCaptor.forClass(Spec.class);
        BuildScanPublishingConfiguration publishing = mock();
        doNothing().when(publishing).onlyIf(specCaptor.capture());
        doExecuteActionWith(publishing).when(buildScanConfiguration).publishing(any());
        return specCaptor;
    }

    private BuildScanPublishingConfiguration.PublishingContext publishingCtxForSuccessfulBuild() {
        BuildScanPublishingConfiguration.PublishingContext ctx = mock();
        lenient().when(ctx.getBuildResult()).thenReturn(mock(BuildResult.class));
        lenient().when(ctx.getBuildResult().getFailures()).thenReturn(Collections.emptyList());
        return ctx;
    }

    private BuildScanPublishingConfiguration.PublishingContext publishingCtxForFailingBuild() {
        BuildScanPublishingConfiguration.PublishingContext ctx = mock();
        lenient().when(ctx.getBuildResult()).thenReturn(mock(BuildResult.class));
        lenient().when(ctx.getBuildResult().getFailures()).thenReturn(Collections.singletonList(new RuntimeException("Boom!")));
        return ctx;
    }

}
