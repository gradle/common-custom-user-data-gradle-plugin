/**
 * Develocity plugins starting w/ version 4.0 won't provide Gradle Enterprise extension classes.
 * Similarly, Gradle Enterprise plugins w/ version 3.16 and below do not provide Develocity extension classes.
 * Therefore, it makes no sense to compile against any of the version of the plugin, as some classes will be missing
 * and only reflective access is possible.
 * This package contains proxy interfaces for Gradle Enterprise and Develocity classes to support reflective access.
 */
package com.gradle.ccud.proxies;