package com.gradle;

import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.util.GradleVersion;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

final class Utils {

    static Optional<String> sysPropertyOrEnvVariable(String sysPropertyName, String envVarName, ProviderFactory providers) {
        Optional<String> sysProperty = sysProperty(sysPropertyName, providers);
        return sysProperty.isPresent() ? sysProperty : envVariable(envVarName, providers);
    }

    static Optional<Boolean> booleanSysPropertyOrEnvVariable(String sysPropertyName, String envVarName, ProviderFactory providers) {
        Optional<Boolean> sysProperty = booleanSysProperty(sysPropertyName, providers);
        return sysProperty.isPresent() ? sysProperty : booleanEnvVariable(envVarName, providers);
    }

    static Optional<Duration> durationSysPropertyOrEnvVariable(String sysPropertyName, String envVarName, ProviderFactory providers) {
        Optional<Duration> sysProperty = durationSysProperty(sysPropertyName, providers);
        return sysProperty.isPresent() ? sysProperty : durationEnvVariable(envVarName, providers);
    }

    static Optional<String> envVariable(String name, ProviderFactory providers) {
        if (isGradle65OrNewer() && !isGradle74OrNewer()) {
            Provider<String> variable = providers.environmentVariable(name).forUseAtConfigurationTime();
            return Optional.ofNullable(variable.getOrNull());
        }
        return Optional.ofNullable(System.getenv(name));
    }

    static Optional<Boolean> booleanEnvVariable(String name, ProviderFactory providers) {
        return envVariable(name, providers).map(Boolean::parseBoolean);
    }

    static Optional<Duration> durationEnvVariable(String name, ProviderFactory providers) {
        return envVariable(name, providers).map(Duration::parse);
    }

    static Optional<String> sysProperty(String name, ProviderFactory providers) {
        if (isGradle65OrNewer() && !isGradle74OrNewer()) {
            Provider<String> property = providers.systemProperty(name).forUseAtConfigurationTime();
            return Optional.ofNullable(property.getOrNull());
        }
        return Optional.ofNullable(System.getProperty(name));
    }

    static Optional<Boolean> booleanSysProperty(String name, ProviderFactory providers) {
        return sysProperty(name, providers).map(Boolean::parseBoolean);
    }

    static Optional<Duration> durationSysProperty(String name, ProviderFactory providers) {
        return sysProperty(name, providers).map(Duration::parse);
    }

    static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    static String stripPrefix(String prefix, String string) {
        return string.startsWith(prefix) ? string.substring(prefix.length()) : string;
    }

    static String appendIfMissing(String str, String suffix) {
        return str.endsWith(suffix) ? str : str + suffix;
    }

    static URI appendPathAndTrailingSlash(URI baseUri, String path) {
        if (isNotEmpty(path)) {
            String normalizedBasePath = appendIfMissing(baseUri.getPath(), "/");
            String normalizedPath = appendIfMissing(stripPrefix("/", path), "/");
            return baseUri.resolve(normalizedBasePath).resolve(normalizedPath);
        }
        return baseUri;
    }

    static String concatenatePaths(String basePath, String path) {
        if (isNotEmpty(basePath)) {
            if (isNotEmpty(path)) {
                String normalizedBasePath = appendIfMissing(basePath, "/");
                String normalizedPath = stripPrefix("/", path);
                return normalizedBasePath + normalizedPath;
            }
            return basePath;
        }
        return path;
    }

    static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static String redactUserInfo(String url) {
        try {
            String userInfo = new URI(url).getUserInfo();
            return userInfo == null
                ? url
                : url.replace(userInfo + '@', "******@");
        } catch (URISyntaxException e) {
            return url;
        }
    }

    static Properties readPropertiesFile(String name, ProviderFactory providers, Directory projectDirectory) {
        try (InputStream input = readFile(name, providers, projectDirectory)) {
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static InputStream readFile(String name, ProviderFactory providers, Directory projectDirectory) throws FileNotFoundException {
        if (isGradle65OrNewer()) {
            RegularFile file = projectDirectory.file(name);
            Provider<byte[]> fileContent = providers.fileContents(file).getAsBytes();
            return new ByteArrayInputStream(fileContent.getOrElse(new byte[0]));
        }
        return new FileInputStream(name);
    }

    static boolean execAndCheckSuccess(String... args) {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec(args);
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException ignored) {
            return false;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    static String execAndGetStdOut(String... args) {
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Reader standard = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            try (Reader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()))) {
                String standardText = readFully(standard);
                String ignore = readFully(error);

                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                return finished && process.exitValue() == 0 ? trimAtEnd(standardText) : null;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            process.destroyForcibly();
        }
    }

    private static String readFully(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int nRead;
        while ((nRead = reader.read(buf)) != -1) {
            sb.append(buf, 0, nRead);
        }
        return sb.toString();
    }

    private static String trimAtEnd(String str) {
        return ('x' + str).trim().substring(1);
    }

    static boolean isGradle4OrNewer() {
        return isGradleNewerThan("4.0");
    }

    public static boolean isGradle43rNewer() {
        return isGradleNewerThan("4.3");
    }

    static boolean isGradle5OrNewer() {
        return isGradleNewerThan("5.0");
    }

    static boolean isGradle6OrNewer() {
        return isGradleNewerThan("6.0");
    }

    static boolean isGradle61OrNewer() {
        return isGradleNewerThan("6.1");
    }

    static boolean isGradle62OrNewer() {
        return isGradleNewerThan("6.2");
    }

    static boolean isGradle65OrNewer() {
        return isGradleNewerThan("6.5");
    }

    static boolean isGradle74OrNewer() {
        return isGradleNewerThan("7.4");
    }

    private static boolean isGradleNewerThan(String version) {
        return GradleVersion.current().compareTo(GradleVersion.version(version)) >= 0;
    }

    private Utils() {
    }

}
