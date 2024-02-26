package com.gradle.ccud.adapters;

import java.net.InetAddress;
import java.util.List;
import java.util.function.Function;

/**
 * Adapter for {@link com.gradle.develocity.agent.gradle.scan.BuildScanDataObfuscationConfiguration} and {@link com.gradle.ccud.adapters.enterprise.proxies.BuildScanDataObfuscationProxy}
 */
public interface BuildScanObfuscationAdapter {

    void username(Function<? super String, ? extends String> obfuscator);

    void hostname(Function<? super String, ? extends String> obfuscator);

    void ipAddresses(Function<? super List<InetAddress>, ? extends List<String>> obfuscator);
}
