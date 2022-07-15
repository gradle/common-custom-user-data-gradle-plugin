package com.gradle

import spock.lang.Specification

class GitMetadataResolverTest extends Specification {
    def "Resolve from #label"() {
        given:
        def resolver = new GitMetadataResolver(isGitInstalled)

        when:
        def resolved = fromEnv ? resolver.resolve(fromEnv, fromGit) : resolver.resolve(fromGit)

        then:
        resolved == expected

        where:
        label                    | isGitInstalled | fromGit                         | fromEnv                        | expected
        'nothing'                | true           | null                            | null                           | Optional.empty()
        'nothing, no CLI'        | false          | null                            | null                           | Optional.empty()
        'git only'               | true           | (() -> Optional.of('fromGit'))  | null                           | Optional.of('fromGit')
        'git only, no CLI'       | false          | (() -> Optional.of('whatever')) | null                           | Optional.empty()
        'git blank'              | true           | (() -> Optional.of(''))         | null                           | Optional.empty()
        'env only'               | true           | null                            | (() -> Optional.of('fromEnv')) | Optional.of('fromEnv')
        'env, git'               | true           | (() -> Optional.of('fromGit'))  | (() -> Optional.of('fromEnv')) | Optional.of('fromEnv')
        'env, git, no CLI'       | false          | (() -> Optional.of('whatever')) | (() -> Optional.of('fromEnv')) | Optional.of('fromEnv')
        'env empty, git'         | true           | (() -> Optional.of('fromGit'))  | (() -> Optional.empty())       | Optional.of('fromGit')
        'env empty, git, no CLI' | false          | (() -> Optional.of('whatever')) | (() -> Optional.empty())       | Optional.empty()
        'env blank, git'         | true           | (() -> Optional.of('fromGit'))  | (() -> Optional.of(''))        | Optional.of('fromGit')
        'env blank, git, no CLI' | false          | (() -> Optional.of('whatever')) | (() -> Optional.of(''))        | Optional.empty()
    }
}
