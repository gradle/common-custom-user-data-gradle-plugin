package com.gradle;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gradle.Utils.toWebRepoUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UtilsTest {

    @ParameterizedTest
    @ArgumentsSource(WebRepoUriArgumentsProvider.class)
    public void testToWebRepoUri(String repositoryHost, String repositoryUri) {
        URI expectedWebRepoUri = URI.create(String.format("https://%s.com/acme-inc/my-project", repositoryHost));
        assertEquals(Optional.of(expectedWebRepoUri), toWebRepoUri(String.format(repositoryUri, repositoryHost)));
    }

    @ParameterizedTest
    @ArgumentsSource(EnterpriseWebRepoUriArgumentsProvider.class)
    public void testToWebRepoUri_enterpriseUri(String repositoryHost, String repositoryUri) {
        URI expectedWebRepoUri = URI.create(String.format("https://%s.acme.com/acme-inc/my-project", repositoryHost));
        assertEquals(Optional.of(expectedWebRepoUri), toWebRepoUri(String.format(repositoryUri, repositoryHost)));
    }

    /**
     * The web repo URI becomes the 'GitHub source' / 'GitLab source' link in the Build Scan, so it must never contain
     * credentials from the git remote URL. Every input below embeds the string {@code SECRET} in the credentials.
     */
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CredentialsInRemoteUrlArgumentsProvider.class)
    public void testToWebRepoUri_neverExposesCredentials(String scenario, String remoteUrl, String expectedWebRepoUri) {
        Optional<URI> webRepoUri = toWebRepoUri(remoteUrl);

        assertEquals(Optional.ofNullable(expectedWebRepoUri).map(URI::create), webRepoUri);
        webRepoUri.ifPresent(uri -> assertFalse(uri.toString().contains("SECRET"), "credentials leaked into " + uri));
    }

    @ParameterizedTest
    @ArgumentsSource(UserInfoArgumentsProvider.class)
    public void testUserInfoRedacted(String inputUrl, String expectedRedactedUrl) {
        Optional<String> redactedUrl = Utils.redactUserInfo(inputUrl);

        assertEquals(expectedRedactedUrl, redactedUrl.orElse(null));
        redactedUrl.ifPresent(url -> assertFalse(url.contains("SECRET"), "credentials leaked into " + url));
    }

    private static class WebRepoUriArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            Set<String> host = Stream.of("github", "gitlab").collect(Collectors.toSet());
            Set<String> remoteRepositoryUris = Stream.of(
                    "https://%s.com/acme-inc/my-project",
                    "https://%s.com:443/acme-inc/my-project",
                    "https://user:secret@%s.com/acme-inc/my-project",
                    "https://user:secret%%1Fpassword@%s.com/acme-inc/my-project",
                    "https://user:secret%%1password@%s.com/acme-inc/my-project",
                    "ssh://git@%s.com/acme-inc/my-project.git",
                    "ssh://git@%s.com:22/acme-inc/my-project.git",
                    "git://%s.com/acme-inc/my-project.git",
                    "git@%s.com/acme-inc/my-project.git"
            ).collect(Collectors.toSet());
            return host.stream().flatMap(h -> remoteRepositoryUris.stream().map(r -> Arguments.arguments(h, r)));
        }
    }

    private static class EnterpriseWebRepoUriArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            Set<String> host = Stream.of("github", "gitlab").collect(Collectors.toSet());
            Set<String> remoteRepositoryUris = Stream.of(
                    "https://%s.acme.com/acme-inc/my-project",
                    "git@%s.acme.com/acme-inc/my-project.git"
            ).collect(Collectors.toSet());
            return host.stream().flatMap(h -> remoteRepositoryUris.stream().map(r -> Arguments.arguments(h, r)));
        }
    }

    private static class CredentialsInRemoteUrlArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                // GitLab Runner <= 18.5 (FF_GIT_URLS_WITHOUT_TOKENS=false, the default) sets remote.origin.url to the
                // repo URL with the CI job token embedded, see https://docs.gitlab.com/ci/jobs/ci_job_token/
                Arguments.arguments(
                    "GitLab CI job token, self-managed host without 'gitlab' in its name",
                    "https://gitlab-ci-token:glcbt-SECRET@scm.acme.internal/acme-inc/my-project.git",
                    null), // host not recognizable as GitLab -> no link (previously leaked the token)
                Arguments.arguments(
                    "Token-only user info",
                    "https://ghp_SECRET@github.com/acme-inc/my-project.git",
                    "https://github.com/acme-inc/my-project"),
                Arguments.arguments(
                    "User name containing 'github' on a host without 'github' in its name",
                    "https://github-bot:SECRET@scm.acme.internal/acme-inc/my-project.git",
                    null),
                Arguments.arguments(
                    "Plain HTTP with user and password",
                    "http://user:SECRET@gitlab.acme.com/acme-inc/my-project.git",
                    "https://gitlab.acme.com/acme-inc/my-project")
            );
        }
    }

    private static class UserInfoArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            Map<String, String> cases = new HashMap<>();
            cases.put("https://user:password@acme.com/acme-inc/my-project", "https://******@acme.com/acme-inc/my-project");
            cases.put("https://user%1Fname:password@acme.com/acme-inc/my-project", "https://******@acme.com/acme-inc/my-project");
            cases.put("https://user:secret%1Fpassword@acme.com/acme-inc/my-project", "https://******@acme.com/acme-inc/my-project");
            cases.put("https://user:secret%1password@acme.com/acme-inc/my-project", null);
            cases.put("git@github.com:gradle/common-custom-user-data-gradle-plugin.git", "git@github.com:gradle/common-custom-user-data-gradle-plugin.git");
            cases.put("https://ghp_SECRET@github.com/acme-inc/my-project.git", "https://******@github.com/acme-inc/my-project.git");

            return cases.entrySet().stream()
                    .map(entry -> Arguments.arguments(entry.getKey(), entry.getValue()));
        }
    }
}
