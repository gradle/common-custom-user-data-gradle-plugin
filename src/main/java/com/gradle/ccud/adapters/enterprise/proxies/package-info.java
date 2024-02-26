/**
 * Develocity plugins starting w/ version 4.0 won't provide Gradle Enterprise extension classes.
 * To continue supporting older Gradle Enterprise plugin versions, we access extension classes using reflective proxies.
 * This package contains proxy interfaces for Gradle Enterprise classes.
 */
package com.gradle.ccud.adapters.enterprise.proxies;