package com.gradle;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@SuppressWarnings("unused")
@AnalyzeClasses(importOptions = ImportOption.DoNotIncludeTests.class)
public class CommonCustomUserDataGradlePluginArchitectureTest {

    @ArchTest
    public static final ArchRule PLUGIN_INTERACTS_ONLY_WITH_ADAPTERS =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("reflection").definedBy("com.gradle.ccud.adapters.reflection..")
            .layer("proxies").definedBy("com.gradle.ccud.adapters.enterprise.proxies..")
            .layer("adapters").definedBy("com.gradle.ccud.adapters..")
            .layer("plugin").definedBy("com.gradle..")
            .whereLayer("reflection").mayOnlyBeAccessedByLayers("proxies", "adapters")
            .whereLayer("proxies").mayOnlyBeAccessedByLayers("adapters")
            .whereLayer("plugin").mayOnlyAccessLayers("adapters");

    @ArchTest
    public static final ArchRule DEPRECATED_PLUGIN_CLASSES_ARE_NOT_USED = noClasses()
        .should()
        .accessClassesThat()
        .resideInAnyPackage("com.gradle.scan.plugin..", "com.gradle.enterprise.gradleplugin..");

    @ArchTest
    public static final ArchRule PLUGIN_CLASSES_ARE_ONLY_USED_BY_ADAPTERS = noClasses()
        .that()
        .resideOutsideOfPackage("com.gradle.ccud.adapters..")
        .should()
        .accessClassesThat()
        .resideInAPackage("com.gradle.develocity.agent.gradle");

}
