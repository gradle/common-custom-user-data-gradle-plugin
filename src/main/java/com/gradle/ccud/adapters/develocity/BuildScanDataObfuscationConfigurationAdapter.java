package com.gradle.ccud.adapters.develocity;

import com.gradle.ccud.adapters.BuildScanObfuscationAdapter;
import com.gradle.develocity.agent.gradle.scan.BuildScanDataObfuscationConfiguration;

import java.net.InetAddress;
import java.util.List;
import java.util.function.Function;

final class BuildScanDataObfuscationConfigurationAdapter implements BuildScanObfuscationAdapter {

    private final BuildScanDataObfuscationConfiguration obfuscation;

    BuildScanDataObfuscationConfigurationAdapter(BuildScanDataObfuscationConfiguration obfuscation) {
        this.obfuscation = obfuscation;
    }

    @Override
    public void username(Function<? super String, ? extends String> obfuscator) {
        obfuscation.username(obfuscator);
    }

    @Override
    public void hostname(Function<? super String, ? extends String> obfuscator) {
        obfuscation.hostname(obfuscator);
    }

    @Override
    public void ipAddresses(Function<? super List<InetAddress>, ? extends List<String>> obfuscator) {
        obfuscation.ipAddresses(obfuscator);
    }
}
