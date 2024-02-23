package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyType;

public interface BuildResultProxy extends ProxyType {

    Throwable getFailure();

}
