package com.gradle;

import org.junit.Test;

import java.net.URI;

import static com.gradle.Utils.toWebRepoUri;
import static org.junit.Assert.assertEquals;

public class UtilsTest {
    @Test
    public void toWebRepoUri_git() {
        assertEquals(URI.create("https://github.com/acme-inc/my-project"), toWebRepoUri("git://github.com/acme-inc/my-project.git").get());
        assertEquals(URI.create("https://gitlab.com/acme-inc/my-project"), toWebRepoUri("git://gitlab.com/acme-inc/my-project.git").get());
    }

    @Test
    public void toWebRepoUri_https() {
        assertEquals(URI.create("https://github.com/acme-inc/my-project"), toWebRepoUri("https://github.com/acme-inc/my-project").get());
        assertEquals(URI.create("https://gitlab.com/acme-inc/my-project"), toWebRepoUri("https://gitlab.com/acme-inc/my-project").get());
    }
}
