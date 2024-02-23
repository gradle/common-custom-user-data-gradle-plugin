package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyType;

import java.net.URI;

public interface PublishedBuildScanProxy extends ProxyType {

    String getBuildScanId();

    URI getBuildScanUri();

}
