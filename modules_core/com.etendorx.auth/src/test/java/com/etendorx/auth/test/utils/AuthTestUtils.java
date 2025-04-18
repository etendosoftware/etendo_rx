package com.etendorx.auth.test.utils;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import com.etendorx.auth.controller.AuthControllerDasRequest;

public class AuthTestUtils {

    public AuthTestUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getRootProjectPath() throws URISyntaxException {
        Path fullPath = Paths.get(AuthControllerDasRequest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String fullPathStr = fullPath.toAbsolutePath().toString();
        String marker = "etendo_rx";
        int idx = fullPathStr.indexOf(marker);
        String rootProjectPath = "";
        if (idx != -1) {
            rootProjectPath = fullPathStr.substring(0, idx + marker.length());
        }
        rootProjectPath = StringUtils.isBlank(rootProjectPath) ? System.getenv("WORKSPACE") : rootProjectPath;
        if (StringUtils.isBlank(rootProjectPath)) {
            throw new RuntimeException("Root project path not found. " +
                "Configure a WORKSPACE environment variable or run the test from the etendo_rx project root directory.");
        }
        return rootProjectPath;
    }
}
