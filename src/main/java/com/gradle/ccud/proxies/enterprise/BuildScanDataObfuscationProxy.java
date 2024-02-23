package com.gradle.ccud.proxies.enterprise;

import com.gradle.ccud.proxies.ProxyType;

import java.net.InetAddress;
import java.util.List;
import java.util.function.Function;

/**
 * Proxy interface for {@code com.gradle.scan.plugin.BuildScanDataObfuscation}
 */
public interface BuildScanDataObfuscationProxy extends ProxyType {

    void username(Function<? super String, ? extends String> obfuscator);

    void hostname(Function<? super String, ? extends String> obfuscator);

    void ipAddresses(Function<? super List<InetAddress>, ? extends List<String>> obfuscator);

}
