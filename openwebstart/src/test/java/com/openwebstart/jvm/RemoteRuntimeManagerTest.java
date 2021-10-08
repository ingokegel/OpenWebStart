package com.openwebstart.jvm;

import com.openwebstart.jvm.json.JsonHandler;
import com.openwebstart.jvm.json.RemoteRuntimeList;
import com.openwebstart.jvm.os.OperationSystem;
import com.openwebstart.jvm.runtimes.RemoteJavaRuntime;
import com.openwebstart.jvm.runtimes.Vendor;
import net.adoptopenjdk.icedteaweb.jnlp.version.VersionId;
import net.adoptopenjdk.icedteaweb.jnlp.version.VersionString;
import net.sourceforge.jnlp.config.DeploymentConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spark.Spark;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.openwebstart.jvm.os.OperationSystem.ARM32;
import static com.openwebstart.jvm.os.OperationSystem.LINUX64;
import static com.openwebstart.jvm.os.OperationSystem.MAC64;
import static com.openwebstart.jvm.os.OperationSystem.WIN64;
import static com.openwebstart.jvm.runtimes.Vendor.ANY_VENDOR;
import static com.openwebstart.jvm.runtimes.Vendor.ECLIPSE;
import static com.openwebstart.jvm.runtimes.Vendor.ORACLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RemoteRuntimeManagerTest {

    private static final VersionId VERSION_1_8_224 = VersionId.fromString("1.8.224");
    private static final VersionId VERSION_1_8_225 = VersionId.fromString("1.8.225");
    private static final VersionId VERSION_11_0_1 = VersionId.fromString("11.0.1");
    private static final VersionId VERSION_11_0_2 = VersionId.fromString("11.0.2");

    private static int getFreePort() {
        final int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            freePort = socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return freePort;
    }

    @BeforeEach
    public void init(@TempDir Path cacheFolder) throws Exception {
        final List<RemoteJavaRuntime> runtimes = new CopyOnWriteArrayList<>();
        final String theOneAndOnlyJdkZip = "http://localhost:8090/jvms/jdk.zip";

        for (OperationSystem os : Arrays.asList(MAC64, WIN64, LINUX64)) {
            runtimes.add(new RemoteJavaRuntime("1.8.145", os, "Temurin", theOneAndOnlyJdkZip));
            runtimes.add(new RemoteJavaRuntime("1.8.220", os, "Temurin", theOneAndOnlyJdkZip));
            runtimes.add(new RemoteJavaRuntime("1.8.224", os, "Temurin", theOneAndOnlyJdkZip));

            runtimes.add(new RemoteJavaRuntime("1.8.146", os, "oracle", theOneAndOnlyJdkZip));
            runtimes.add(new RemoteJavaRuntime("1.8.221", os, "oracle", theOneAndOnlyJdkZip));
            runtimes.add(new RemoteJavaRuntime("1.8.225", os, "oracle", theOneAndOnlyJdkZip));

            runtimes.add(new RemoteJavaRuntime("11.0.1", os, "Temurin", theOneAndOnlyJdkZip));

            runtimes.add(new RemoteJavaRuntime("11.0.2", os, "oracle", theOneAndOnlyJdkZip));
        }

        final int port = getFreePort();
        Spark.port(port);
        Spark.get("/jvms", ((request, response) -> {
            try {
                final RemoteRuntimeList list = new RemoteRuntimeList(runtimes, 5_000);
                return JsonHandler.getInstance().toJson(list);
            } catch (final Exception e) {
                e.printStackTrace();
                throw e;
            }
        }));
        Spark.init();
        Spark.awaitInitialization();

        RuntimeManagerConfig.setCachePath(cacheFolder);
        RuntimeManagerConfig.setDefaultRemoteEndpoint(new URL("http://localhost:" + port + "/jvms"));
        RuntimeManagerConfig.setNonDefaultServerAllowed(true);
        RuntimeManagerConfig.setDefaultVendor(null);
        RuntimeManagerConfig.setSupportedVersionRange(null);

        LocalRuntimeManager.getInstance().loadRuntimes(new DeploymentConfiguration());
    }

    @AfterEach
    public void reset() {
        Spark.stop();
        Spark.awaitStop();

        RuntimeManagerConfig.setNonDefaultServerAllowed(true);
        RuntimeManagerConfig.setDefaultVendor(null);
        RuntimeManagerConfig.setSupportedVersionRange(null);
    }

    @Test
    public void testRemoteRuntime_1() {
        //given
        final VersionString versionString = VersionString.fromString("1.8*");
        final URL specificServerEndpoint = null;

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ANY_VENDOR, MAC64).orElse(null);

        //than
        Assertions.assertNotNull(runtime);
        assertEquals(VERSION_1_8_225, runtime.getVersion());
        assertEquals(ORACLE, runtime.getVendor());
        assertEquals(MAC64, runtime.getOperationSystem());
    }

    @Test
    public void testRemoteRuntime_2() {
        //given
        final VersionString versionString = VersionString.fromString("1.8*");
        final URL specificServerEndpoint = null;

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ANY_VENDOR, WIN64).orElse(null);

        //than
        Assertions.assertNotNull(runtime);
        assertEquals(VERSION_1_8_225, runtime.getVersion());
        assertEquals(ORACLE, runtime.getVendor());
        assertEquals(WIN64, runtime.getOperationSystem());
    }

    @Test
    public void testRemoteRuntime_3() {
        //given
        final VersionString versionString = VersionString.fromString("1.8*");
        final URL specificServerEndpoint = null;

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ECLIPSE, MAC64).orElse(null);

        //than
        Assertions.assertNotNull(runtime);
        assertEquals(VERSION_1_8_224, runtime.getVersion());
        assertEquals(ECLIPSE, runtime.getVendor());
        assertEquals(MAC64, runtime.getOperationSystem());
    }

    @Test
    public void testRemoteRuntime_4() {
        //given
        final VersionString versionString = VersionString.fromString("1.8+");
        final URL specificServerEndpoint = null;

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ANY_VENDOR, MAC64).orElse(null);

        //than
        Assertions.assertNotNull(runtime);
        assertEquals(VERSION_11_0_2, runtime.getVersion());
        assertEquals(ORACLE, runtime.getVendor());
        assertEquals(MAC64, runtime.getOperationSystem());
    }

    @Test
    public void testRemoteRuntime_5() {
        //given
        final VersionString versionString = VersionString.fromString("1.8+");
        final URL specificServerEndpoint = null;

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ECLIPSE, MAC64).orElse(null);

        //than
        Assertions.assertNotNull(runtime);
        assertEquals(VERSION_11_0_1, runtime.getVersion());
        assertEquals(ECLIPSE, runtime.getVendor());
        assertEquals(MAC64, runtime.getOperationSystem());
    }

    @Test
    public void testRemoteRuntime_6() {
        //given
        final VersionString versionString = VersionString.fromString("20+");
        final URL specificServerEndpoint = null;

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ANY_VENDOR, MAC64).orElse(null);

        //than
        Assertions.assertNull(runtime);
    }

    @Test
    public void testRemoteRuntime_7() {
        //given
        final VersionString versionString = VersionString.fromString("1.8+");
        final URL specificServerEndpoint = null;
        final Vendor vendor = Vendor.fromString("not_found");

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, vendor, MAC64).orElse(null);

        //than
        Assertions.assertNull(runtime);
    }

    @Test
    public void testRemoteRuntime_8() {
        //given
        final VersionString versionString = VersionString.fromString("1.8+");
        final URL specificServerEndpoint = null;

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ANY_VENDOR, ARM32).orElse(null);

        //than
        Assertions.assertNull(runtime);
    }

    @Test
    public void testRemoteRuntime_10() throws Exception {
        //given
        final VersionString versionString = VersionString.fromString("1.8*");
        final URL specificServerEndpoint = new URL("http://do.not.exists/error");

        //when
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ORACLE, MAC64).orElse(null);

        // then
        Assertions.assertNull(runtime);
    }

    @Test
    public void testRemoteRuntime_11() throws Exception {
        //given
        final VersionString versionString = VersionString.fromString("1.8*");
        final URL specificServerEndpoint = new URL("http://do.not.exists/error");

        //when
        RuntimeManagerConfig.setNonDefaultServerAllowed(false);
        final RemoteJavaRuntime runtime = RemoteRuntimeManager.getInstance().getBestRuntime(versionString, specificServerEndpoint, ANY_VENDOR, MAC64).orElse(null);

        //than
        Assertions.assertNotNull(runtime);
        assertEquals(VERSION_1_8_225, runtime.getVersion());
        assertEquals(ORACLE, runtime.getVendor());
        assertEquals(MAC64, runtime.getOperationSystem());
    }

    @Test
    public void testParseRemoteRuntimeJson() throws IOException, URISyntaxException {
        // given
        URL url = getClass().getResource("jvms.json");
        Path resPath = Paths.get(url.toURI());
        String json = new String(Files.readAllBytes(resPath), "UTF8");

        // when
        RemoteRuntimeList remoteRuntimeList = RemoteRuntimeManager.getInstance().parseRemoteRuntimeJson(json);

        // then
        assertFalse(remoteRuntimeList.getRuntimes().isEmpty());
        assertEquals(36, remoteRuntimeList.getRuntimes().size());
    }
}
