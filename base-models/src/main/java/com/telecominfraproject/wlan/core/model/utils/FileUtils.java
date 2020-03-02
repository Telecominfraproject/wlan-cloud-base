package com.telecominfraproject.wlan.core.model.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    /**
     * Check if workDirStr is root directory of a file system
     * 
     * @param workDirStr
     * @return
     */
    public static boolean isRootDirectory(String workDirStr) {
        if (workDirStr == null) {
            return false;
        }
        Path workDirPath = Paths.get(workDirStr).toAbsolutePath();
        if (!workDirPath.toFile().isDirectory()) {
            return false;
        }
        // clean up /.
        if (!workDirPath.endsWith(workDirPath.getFileSystem().getSeparator())) {
            workDirPath = workDirPath.getParent();
        }
        return workDirPath.equals(workDirPath.getRoot());
    }

    private FileUtils() {
    }
}
