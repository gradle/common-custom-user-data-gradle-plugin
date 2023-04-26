package com.gradle;

import org.junit.Assert;
import org.junit.Test;

import static com.gradle.Utils.extractRepoUrl;
import static org.junit.Assert.assertEquals;

public class UtilsTest {
    @Test
    public void extractRepoUrl_git() {
        assertEquals("https://github.com/acme-inc/my-project", extractRepoUrl("git://github.com/acme-inc/my-project.git").get());
    }
    @Test
    public void extractRepoUrl_https() {
        assertEquals("https://github.com/acme-inc/my-project", extractRepoUrl("https://github.com/acme-inc/my-project").get());
    }
}
