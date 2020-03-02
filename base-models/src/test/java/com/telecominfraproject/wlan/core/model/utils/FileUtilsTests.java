package com.telecominfraproject.wlan.core.model.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.utils.FileUtils;

public class FileUtilsTests {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtilsTests.class);
    @Rule
    public TestName name = new TestName();

    @Test
    public void testIsRoot() {
        for (Path file : FileSystems.getDefault().getRootDirectories()) {
            String testDir = file.toString() + ".";
            LOG.debug("{} {}", name.getMethodName(), testDir);
            assertTrue(name.getMethodName() + " " + testDir, FileUtils.isRootDirectory(testDir));
        }
    }

    @Test
    public void testNotIsRoot() {
        String testDir = FileSystems.getDefault().getPath(".").toAbsolutePath().toString();
        LOG.debug("{} {}", name.getMethodName(), testDir);
        assertFalse(name.getMethodName() + " " + testDir, FileUtils.isRootDirectory(testDir));
    }

    @Test
    public void testNotIsRootInvalidDir() {
        String testDir = FileSystems.getDefault().getPath(UUID.randomUUID().toString()).toAbsolutePath().toString();
        LOG.debug("{} {}", name.getMethodName(), testDir);
        assertFalse(name.getMethodName() + " " + testDir, FileUtils.isRootDirectory(testDir));
    }
}
