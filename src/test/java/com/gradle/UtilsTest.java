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

    @ParameterizedTest
    @ArgumentsSource(UserInfoArgumentsProvider.class)
    public void testUserInfoRedacted(String inputUrl, String expectedRedactedUrl) {
        assertEquals(expectedRedactedUrl, Utils.redactUserInfo(inputUrl).orElse(null));
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

    private static class UserInfoArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            Map<String, String> cases = new HashMap<>();
            cases.put("https://user:password@acme.com/acme-inc/my-project", "https://******@acme.com/acme-inc/my-project");
            cases.put("https://user%1Fname:password@acme.com/acme-inc/my-project", "https://******@acme.com/acme-inc/my-project");
            cases.put("https://user:secret%1Fpassword@acme.com/acme-inc/my-project", "https://******@acme.com/acme-inc/my-project");
            cases.put("https://user:secret%1password@acme.com/acme-inc/my-project", null);
            cases.put("git@github.com:gradle/common-custom-user-data-gradle-plugin.git", "git@github.com:gradle/common-custom-user-data-gradle-plugin.git");

            return cases.entrySet().stream()
                    .map(entry -> Arguments.arguments(entry.getKey(), entry.getValue()));
        }
    }
}
