package com.gradle;

import java.util.Optional;
import java.util.function.Supplier;

class GitMetadataResolver {
    private final boolean isGitInstalled;

    GitMetadataResolver(boolean isGitInstalled) {
        this.isGitInstalled = isGitInstalled;
    }

    Optional<String> resolve(Supplier<Optional<String>> fromGit) {
        return resolve(null, fromGit);
    }

    Optional<String> resolve(Supplier<Optional<String>> fromEnv, Supplier<Optional<String>> fromGit) {
        Optional<String> resolved = Optional.empty();
        if (fromEnv != null) {
            resolved = fromEnv.get().flatMap(str -> str.isEmpty() ? Optional.empty() : Optional.of(str));
        }
        if (isGitInstalled && fromGit != null && !resolved.isPresent()) {
            resolved = fromGit.get().flatMap(str -> str.isEmpty() ? Optional.empty() : Optional.of(str));
        }
        return resolved;
    }
}
