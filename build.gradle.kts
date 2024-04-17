plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.github.breadmoirai.github-release") version "2.5.2"
    id("org.gradle.wrapper-upgrade") version "0.11.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val releaseVersion = releaseVersion()
val releaseNotes = releaseNotes()

group = "com.gradle"
version = releaseVersion.get()
description = "A Gradle plugin to capture common custom user data used for Gradle Build Scans in Develocity"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradle:develocity-gradle-plugin-adapters:1.0.2")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

wrapperUpgrade {
    gradle {
        register("common-custom-user-data-gradle-plugin") {
            repo = "gradle/common-custom-user-data-gradle-plugin"
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.jar { enabled = false }

tasks.shadowJar {
    // required by the plugin-publish-plugin
    archiveClassifier = ""
}

gradlePlugin {
    website = "https://github.com/gradle/common-custom-user-data-gradle-plugin"
    vcsUrl = "https://github.com/gradle/common-custom-user-data-gradle-plugin.git"

    isAutomatedPublishing = true

    plugins {
        register("commonCustomUserData") {
            id = "com.gradle.common-custom-user-data-gradle-plugin"
            displayName = "Develocity Common Custom User Data Gradle Plugin"
            description = releaseNotes.get()
            implementationClass = "com.gradle.CommonCustomUserDataGradlePlugin"
            tags.addAll("android", "java", "develocity")
        }
    }
}

tasks.withType<ValidatePlugins>().configureEach {
    failOnWarning = true
    enableStricterValidation = true
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    dependsOn(tasks.named("shadowJar"))
}

/*
The rest of the build logic in this file is only required for publishing to the Gradle Plugin Portal.
When using this project as a template for your own plugin to publish internally, you should delete all code following this comment.
You may also remove `plugin-publish` and `signing` from the `plugins {}` block above.
 */

signing {
    // Require publications to be signed on CI. Otherwise, publication will be signed only if keys are provided.
    isRequired = providers.environmentVariable("CI").isPresent

    useInMemoryPgpKeys(
        providers.environmentVariable("PGP_SIGNING_KEY").orNull,
        providers.environmentVariable("PGP_SIGNING_KEY_PASSPHRASE").orNull
    )
}

githubRelease {
    token(System.getenv("CCUD_GIT_TOKEN") ?: "")
    owner = "gradle"
    repo = "common-custom-user-data-gradle-plugin"
    targetCommitish = "main"
    releaseName = releaseVersion
    tagName = releaseVersion.map { "v$it" }
    prerelease = false
    overwrite = false
    generateReleaseNotes = false
    body = releaseNotes
}

val createReleaseTag by tasks.registering(CreateGitTag::class) {
    // Ensure tag is created only after a successful publishing
    mustRunAfter("publishPlugins")
    tagName = githubRelease.tagName.map { it.toString() }
}

tasks.githubRelease {
    dependsOn(createReleaseTag)
}

tasks.withType<com.gradle.publish.PublishTask>().configureEach {
    notCompatibleWithConfigurationCache("$name task does not support configuration caching")
}

fun releaseVersion(): Provider<String> {
    val releaseVersionFile = layout.projectDirectory.file("release/version.txt")
    return providers.fileContents(releaseVersionFile).asText.map(String::trim)
}

fun releaseNotes(): Provider<String> {
    val releaseNotesFile = layout.projectDirectory.file("release/changes.md")
    return providers.fileContents(releaseNotesFile).asText.map(String::trim)
}
