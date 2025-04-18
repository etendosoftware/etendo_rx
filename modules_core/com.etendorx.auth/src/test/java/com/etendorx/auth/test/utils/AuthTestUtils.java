package com.etendorx.auth.test.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.etendorx.auth.controller.AuthControllerDasRequest;

/**
 * Utility class for authentication tests.
 * <p>
 * This class provides methods to retrieve the root project path for testing purposes.
 * </p>
 */
public class AuthTestUtils {

    private static final String CONFIG_URL = "http://localhost:8888";

    static Process configProcess;
    static Process dasProcess;
    static Process authProcess;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private AuthTestUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Starts the configuration server.
     *
     * @throws URISyntaxException If the URI syntax is incorrect.
     * @throws IOException        If an I/O error occurs.
     * @throws InterruptedException If the thread is interrupted.
     */
    public static void startConfigServer() throws URISyntaxException, IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        final String rootProjectPath = getRootProjectPath();
        ProcessBuilder pb = new ProcessBuilder(javaBin, "-jar", "/tmp/com.etendorx.configserver-2.3.0.jar");
        Map<String, String> env = pb.environment();
        env.put("SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS", "file://" + rootProjectPath + "/rxconfig");
        env.put("SPRING_PROFILES_ACTIVE", "native");
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        configProcess = pb.start();
        Thread.sleep(25000);
    }

    /**
     * Starts the DAS server.
     *
     * @throws IOException        If an I/O error occurs.
     * @throws InterruptedException If the thread is interrupted.
     */
    public static void startDASServer() throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        ProcessBuilder pbDAS = new ProcessBuilder(
            javaBin,
            "-Dfile.encoding=UTF-8",
            "-Dloader.path=/tmp/",
            "-jar",
            "/tmp/com.etendorx.das-2.3.0.jar"
        );

        pbDAS.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pbDAS.redirectError(ProcessBuilder.Redirect.INHERIT);
        dasProcess = pbDAS.start();
        Thread.sleep(60000);
    }

    /**
     * Starts the authentication server.
     *
     * @throws IOException        If an I/O error occurs.
     * @throws InterruptedException If the thread is interrupted.
     */
    public static void startAuthServer() throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        ProcessBuilder pbAuth = new ProcessBuilder(javaBin, "-jar", "/tmp/com.etendorx.auth-2.3.0.jar");
        Map<String, String> envAuth = pbAuth.environment();
        envAuth.put("CONFIG_SERVER_URL", CONFIG_URL);
        envAuth.put("SPRING_CONFIG_IMPORT", "configserver:${config.server.url}");
        pbAuth.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pbAuth.redirectError(ProcessBuilder.Redirect.INHERIT);
        authProcess = pbAuth.start();
        Thread.sleep(30000);
    }

    /**
     * Retrieves the root project path.
     *
     * @return The root project path as a string.
     * @throws URISyntaxException If the URI syntax is incorrect.
     */
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
            throw new IllegalStateException("Root project path not found. " +
                "Configure a WORKSPACE environment variable or run the test from the etendo_rx project root directory.");
        }
        return rootProjectPath;
    }

    /**
     * Stops all running services.
     */
    public static void stopRunningServices() {
        if (configProcess != null) {
            configProcess.destroy();
        }
        if (dasProcess != null) {
            dasProcess.destroy();
        }
        if (authProcess != null) {
            authProcess.destroy();
        }
    }
}
