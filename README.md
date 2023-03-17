> _This repository is maintained by the Gradle Enterprise Solutions team, as one of several publicly available repositories:_
> - _[Gradle Enterprise Build Configuration Samples][ge-build-config-samples]_
> - _[Gradle Enterprise Build Optimization Experiments][ge-build-optimization-experiments]_
> - _[Gradle Enterprise Build Validation Scripts][ge-build-validation-scripts]_
> - _[Gradle Enterprise Open Source Projects][ge-oss-projects]_
> - _[Common Custom User Data Maven Extension][ccud-maven-extension]_
> - _[Common Custom User Data Gradle Plugin][ccud-gradle-plugin] (this repository)_
> - _[Android Cache Fix Gradle Plugin][android-cache-fix-plugin]_
> - _[Wrapper Upgrade Gradle Plugin][wrapper-upgrade-gradle-plugin]_

# Common Custom User Data Gradle Plugin

[![Verify Build](https://github.com/gradle/common-custom-user-data-gradle-plugin/actions/workflows/build-verification.yml/badge.svg?branch=main)](https://github.com/gradle/common-custom-user-data-gradle-plugin/actions/workflows/build-verification.yml)
[![Plugin Portal](https://img.shields.io/maven-metadata/v?metadataUrl=https://plugins.gradle.org/m2/com/gradle/common-custom-user-data-gradle-plugin/maven-metadata.xml&label=Plugin%20Portal)](https://plugins.gradle.org/plugin/com.gradle.common-custom-user-data-gradle-plugin)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.solutions-team.gradle.com/scans)

The Common Custom User Data Gradle plugin for Gradle Enterprise enhances published build scans
by adding a set of tags, links and custom values that have proven to be useful for many projects building with Gradle Enterprise.

You can leverage this plugin for your project in one of two ways:
1. [Apply the published plugin](#applying-the-published-plugin) directly in your build and immediately benefit from enhanced build scans
2. Copy this repository and [develop a customized version of the plugin](#developing-a-customized-version-of-the-plugin) to standardize Gradle Enterprise usage across multiple projects

## Applying the published plugin

The Common Custom User Data Gradle plugin is available in the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.gradle.common-custom-user-data-gradle-plugin). This
plugin requires the [Gradle Enterprise Gradle plugin](https://plugins.gradle.org/plugin/com.gradle.enterprise) to also be applied in your build in order to have an effect.

In order for the Common Custom User Gradle plugin to become active, you need to register it in the `settings.gradle` file of your project. The `settings.gradle` file is the same
file where you have already declared the Gradle Enterprise Gradle plugin.

See [here](settings.gradle) for an example.

### Version compatibility

This table details the version compatibility of the Common Custom User Data Gradle plugin with the Gradle Enterprise Gradle plugin.

| Common Custom User Data Gradle plugin versions | Gradle Enterprise Gradle plugin versions | Gradle version |
| ---------------------------------------------- | ---------------------------------------- | -------------- |
| `1.0+`                                         | `3.0+`                                   | `5.0+`         | 
| `1.7+`                                         | `1.8+`                                   | `4.0+`         |

## Captured data

The additional tags, links and custom values captured by this plugin include:
- A tag representing the operating system
- A tag representing how the build was invoked, be that from your IDE (IDEA, Eclipse, Android Studio) or from the command-line
- A tag representing builds run on CI, together with a set of tags, links and custom values specific to the CI server running the build
- For Git repositories, information on the commit id, branch name, status, and whether the checkout is dirty or not

See [CustomBuildScanEnhancements.java](./src/main/java/com/gradle/CustomBuildScanEnhancements.java) for details on what data is
captured and under which conditions.

## Configuration overrides

This plugin also allows overriding various Gradle Enterprise related settings via system properties and environment variables:
- Gradle Enterprise general configuration
- Remote build cache configuration
- Local build cache configuration

See [Overrides.java](./src/main/java/com/gradle/Overrides.java) for the override behavior.

You can use the system properties and environment variables to override Gradle Enterprise related settings temporarily without having
to modify the build scripts. For example, to disable the local build cache when running a build:

```bash
./gradlew -Dgradle.cache.local.enabled=false build
```

<details>
  <summary>Click to see the complete set of available system properties and environment variables in the table below. </summary>

### Gradle Enterprise settings

| Gradle Enterprise API                 | System property                        | Environment variable                   |
|:--------------------------------------|:---------------------------------------|:---------------------------------------|
| gradleEnterprise.server               | gradle.enterprise.url                  | GRADLE_ENTERPRISE_URL                  |
| gradleEnterprise.allowUntrustedServer | gradle.enterprise.allowUntrustedServer | GRADLE_ENTERPRISE_ALLOWUNTRUSTEDSERVER |

### Local Build Cache settings

| Local Build Cache API                            | System property                                 | Environment variable                            |
|:-------------------------------------------------|:------------------------------------------------|:------------------------------------------------|
| buildCache.local.setEnabled                      | gradle.cache.local.enabled                      | GRADLE_CACHE_LOCAL_ENABLED                      |
| buildCache.local.setPush                         | gradle.cache.local.push                         | GRADLE_CACHE_LOCAL_PUSH                         |
| buildCache.local.setDirectory                    | gradle.cache.local.directory                    | GRADLE_CACHE_LOCAL_DIRECTORY                    |
| buildCache.local.setRemoveUnusedEntriesAfterDays | gradle.cache.local.removeUnusedEntriesAfterDays | GRADLE_CACHE_LOCAL_REMOVEUNUSEDENTRIESAFTERDAYS |

### HTTP Build Cache settings

| HTTP Build Cache API                       | System property                           | Environment variable                      |
|:-------------------------------------------|:------------------------------------------|:------------------------------------------|
| buildCache.remote.setEnabled               | gradle.cache.remote.enabled               | GRADLE_CACHE_REMOTE_ENABLED               |
| buildCache.remote.setPush                  | gradle.cache.remote.push                  | GRADLE_CACHE_REMOTE_PUSH                  |
| buildCache.remote.setAllowUntrustedServer  | gradle.cache.remote.allowUntrustedServer  | GRADLE_CACHE_REMOTE_ALLOWUNTRUSTEDSERVER  |
| buildCache.remote.setAllowInsecureProtocol | gradle.cache.remote.allowInsecureProtocol | GRADLE_CACHE_REMOTE_ALLOWINSECUREPROTOCOL |
| buildCache.remote.setUrl                   | gradle.cache.remote.url                   | GRADLE_CACHE_REMOTE_URL                   |

### Gradle Enterprise Build Cache settings

| Gradle Enterprise Build Cache API          | System property                              | Environment variable                      |
|:-------------------------------------------|:---------------------------------------------|:------------------------------------------|
| buildCache.remote.setEnabled               | gradle.cache.remote.enabled                  | GRADLE_CACHE_REMOTE_ENABLED               |
| buildCache.remote.setPush                  | gradle.cache.remote.push                     | GRADLE_CACHE_REMOTE_PUSH                  |
| buildCache.remote.setAllowUntrustedServer  | gradle.cache.remote.allowUntrustedServer     | GRADLE_CACHE_REMOTE_ALLOWUNTRUSTEDSERVER  |
| buildCache.remote.setAllowInsecureProtocol | gradle.cache.remote.setAllowInsecureProtocol | GRADLE_CACHE_REMOTE_ALLOWINSECUREPROTOCOL |
| buildCache.remote.setServer                | gradle.cache.remote.server                   | GRADLE_CACHE_REMOTE_SERVER                |
| buildCache.remote.setPath                  | gradle.cache.remote.path                     | GRADLE_CACHE_REMOTE_PATH                  |

</details>

## Developing a customized version of the plugin

For more flexibility, we recommend creating a copy of this repository so that you may develop a customized version of the plugin and publish it internally for your projects to consume.

This approach has a number of benefits:
- Tailor the build scan enhancements to exactly the set of tags, links and custom values you require
- Standardize the configuration for connecting to Gradle Enterprise and the remote build cache in your organization, removing the need for each project to specify this configuration

If your customized plugin provides all required Gradle Enterprise configuration, then a consumer project will get all the benefits of Gradle Enterprise simply by applying the plugin. The
project sources provide a good template to get started with your own plugin.

Refer to the [Javadoc](https://docs.gradle.com/enterprise/gradle-plugin/api/) for more details on the key types available for use.

See the [Gradle User Manual](https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html#custom-plugin-repositories) for more details on publishing Gradle plugins to an internal repository.

## Changelog

Refer to the [release history](https://github.com/gradle/common-custom-user-data-gradle-plugin/releases) to see detailed changes on the versions.

## Learn more

Visit our website to learn more about [Gradle Enterprise][gradle-enterprise].

## License

The Gradle Enterprise Common Custom User Data Gradle plugin is open-source software released under the [Apache 2.0 License][apache-license].

[ge-build-config-samples]: https://github.com/gradle/gradle-enterprise-build-config-samples
[ge-build-optimization-experiments]: https://github.com/gradle/gradle-enterprise-build-optimization-experiments
[ge-build-validation-scripts]: https://github.com/gradle/gradle-enterprise-build-validation-scripts
[ge-oss-projects]: https://github.com/gradle/gradle-enterprise-oss-projects
[ccud-gradle-plugin]: https://github.com/gradle/common-custom-user-data-gradle-plugin
[ccud-maven-extension]: https://github.com/gradle/common-custom-user-data-maven-extension
[android-cache-fix-plugin]: https://github.com/gradle/android-cache-fix-gradle-plugin
[wrapper-upgrade-gradle-plugin]: https://github.com/gradle/wrapper-upgrade-gradle-plugin
[gradle-enterprise]: https://gradle.com/enterprise
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0.html
