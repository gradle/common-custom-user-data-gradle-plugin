package com.gradle;

import java.util.Optional;
import java.util.function.Supplier;

import static com.gradle.Utils.execAndGetStdOut;

class GitMetadata {

    private final String[] args;
    private final boolean isGitInstalled;
    private final Supplier<Optional<String>> fromEnv;

    private GitMetadata(GitMetadataBuilder metadataBuilder) {
        this.args = metadataBuilder.args;
        this.isGitInstalled = metadataBuilder.isGitInstalled;
        this.fromEnv = metadataBuilder.fromEnv;
    }

    Optional<String> resolve() {
        if (fromEnv != null) {
            return fromEnv.get();
        }
        if (isGitInstalled && args != null) {
            return execAndGetStdOut(args);
        }
        return Optional.empty();
    }

    static class GitMetadataBuilder {
        private String[] args;
        private final boolean isGitInstalled;
        private Supplier<Optional<String>> fromEnv;

        GitMetadataBuilder(boolean isGitInstalled) {
            this.isGitInstalled = isGitInstalled;
        }

        GitMetadataBuilder fromCmd(String... args) {
            this.args = args;
            return this;
        }

        GitMetadataBuilder fromEnv(Supplier<Optional<String>> fromEnv) {
            this.fromEnv = fromEnv;
            return this;
        }

        GitMetadata build() {
            return new GitMetadata(this);
        }
    }

}
