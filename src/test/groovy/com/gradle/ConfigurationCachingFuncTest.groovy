package com.gradle

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

class ConfigurationCachingFuncTest extends Specification {
    static final String CC_PROBLEMS_FOUND = "problems were found storing the configuration cache"
    static final String PLUGIN_VERSION_SYSTEM_PROPERTY = 'com.gradle.common-custom-user-data-gradle-plugin.version'

    @TempDir
    File testProjectDir
    File settingsFile
    File buildFile
    File srcFile
    File testFile

    def setup() {
        settingsFile = new File(testProjectDir, 'settings.gradle')
        buildFile = new File(testProjectDir, 'build.gradle')
        srcFile = file(testProjectDir, "src/main/java/Adder.java")
        testFile = file(testProjectDir, "src/test/java/AdderTest.java")

        settingsFile << """
            buildscript {
                repositories {
                    gradlePluginPortal()
                    maven {
                        url = '${localRepo}'
                    }
                }
                dependencies {
                    classpath 'com.gradle:gradle-enterprise-gradle-plugin:3.9'
                    classpath 'com.gradle:common-custom-user-data-gradle-plugin:${pluginVersion}'
                }
            }

            apply plugin: "com.gradle.enterprise"
            apply plugin: "com.gradle.common-custom-user-data-gradle-plugin"

            rootProject.name = 'ccud-gradle-functional-test-project'
        """

        buildFile << """
            plugins {
                id 'java'
            }

            repositories {
               mavenCentral()
            }

            dependencies {
               testImplementation 'org.junit.jupiter:junit-jupiter:5.7.1'
            }

            tasks.withType(Test).configureEach {
                useJUnitPlatform()
            }
        """

        srcFile << """
            class Adder {
                public static int add(int x, int y) {
                    return x + y;
                }
            }
        """

        testFile << """
            import static org.junit.jupiter.api.Assertions.assertEquals;
            import org.junit.jupiter.api.Test;

            class AdderTest {
                @Test
                public void addTest() {
                    assertEquals(3, Adder.add(1, 2));
                }
            }
        """
    }

    def "plugin is compatible with configuration cache"() {
        given:
        def runner = GradleRunner.create()
            .withProjectDir(testProjectDir)

        when:
        def result = runner
            .withArguments('build', '--configuration-cache')
            .build()

        then:
        assert !result.output.contains(CC_PROBLEMS_FOUND) || result.output.contains("0 ${CC_PROBLEMS_FOUND}")


        when:
        result = runner
            .withArguments('build', '--configuration-cache')
            .build()

        then:
        assert result.output.contains("Configuration cache entry reused.")
    }

    static String getPluginVersion() {
        def pluginVersion = System.getProperty(PLUGIN_VERSION_SYSTEM_PROPERTY)
        if (pluginVersion == null) {
            throw new IllegalStateException("The '${PLUGIN_VERSION_SYSTEM_PROPERTY}' system property must be set in order to apply the plugin under test!")
        }
        return pluginVersion
    }

    static String getLocalRepo() {
        return Paths.get(System.getProperty("local.repo")).toUri()
    }

    def file(File projectDir, String path) {
        def file = new File(projectDir, path)
        file.parentFile.mkdirs()
        return file
    }
}
