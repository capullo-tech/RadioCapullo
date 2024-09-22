package tech.capullo.radio.data;


import android.util.Log;

import com.google.gson.JsonObject;
import com.spotify.connectstate.Connect;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import xyz.gianlu.librespot.common.NameThreadFactory;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.crypto.DiffieHellman;
import xyz.gianlu.librespot.mercury.MercuryClient;

/**
 * @author Powerbling
 */
public class AndroidZeroconfServer implements Closeable {
    private final static int MAX_PORT = 65536;
    private final static int MIN_PORT = 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidZeroconfServer.class);
    private static final byte[] EOL = new byte[]{'\r', '\n'};
    private static final JsonObject DEFAULT_GET_INFO_FIELDS = new JsonObject();
    private static final JsonObject DEFAULT_SUCCESSFUL_ADD_USER = new JsonObject();

    static {
        DEFAULT_GET_INFO_FIELDS.addProperty("status", 101);
        DEFAULT_GET_INFO_FIELDS.addProperty("statusString", "OK");
        DEFAULT_GET_INFO_FIELDS.addProperty("spotifyError", 0);
        DEFAULT_GET_INFO_FIELDS.addProperty("version", "2.7.1");
        DEFAULT_GET_INFO_FIELDS.addProperty("libraryVersion", "?.?.?");
        DEFAULT_GET_INFO_FIELDS.addProperty("accountReq", "PREMIUM");
        DEFAULT_GET_INFO_FIELDS.addProperty("brandDisplayName", "librespot-org");
        DEFAULT_GET_INFO_FIELDS.addProperty("modelDisplayName", "librespot-android");
        DEFAULT_GET_INFO_FIELDS.addProperty("voiceSupport", "NO");
        DEFAULT_GET_INFO_FIELDS.addProperty("availability", "");
        DEFAULT_GET_INFO_FIELDS.addProperty("productID", 0);
        DEFAULT_GET_INFO_FIELDS.addProperty("tokenType", "default");
        DEFAULT_GET_INFO_FIELDS.addProperty("groupStatus", "NONE");
        DEFAULT_GET_INFO_FIELDS.addProperty("resolverVersion", "0");
        DEFAULT_GET_INFO_FIELDS.addProperty("scope", "streaming,client-authorization-universal");

        DEFAULT_SUCCESSFUL_ADD_USER.addProperty("status", 101);
        DEFAULT_SUCCESSFUL_ADD_USER.addProperty("spotifyError", 0);
        DEFAULT_SUCCESSFUL_ADD_USER.addProperty("statusString", "OK");

        Utils.removeCryptographyRestrictions();
    }

    private final HttpRunner runner;
    private final DiffieHellman keys;
    private final List<SessionListener> sessionListeners;
    private final Object connectionLock = new Object();
    private final Inner inner;
    private volatile Session session;
    private String connectingUsername = null;
    //private final Disposable registeredService;
    public int listenPort;

    private AndroidZeroconfServer(@NotNull Inner inner, int listenPort) throws IOException {
        this.inner = inner;
        this.keys = new DiffieHellman(inner.random);
        this.sessionListeners = new ArrayList<>();

        if (listenPort == -1)
            listenPort = inner.random.nextInt((MAX_PORT - MIN_PORT) + 1) + MIN_PORT;
        this.listenPort = listenPort;

        new Thread(this.runner = new HttpRunner(listenPort), "zeroconf-http-server").start();
    }

    @NotNull
    private static Map<String, String> parsePath(@NotNull String path) {
        HttpUrl url = HttpUrl.get("http://host" + path);
        Map<String, String> map = new HashMap<>();
        for (String name : url.queryParameterNames()) map.put(name, url.queryParameter(name));
        return map;
    }

    @Override
    public void close() throws IOException {
        runner.close();
    }

    public void closeSession() throws IOException {
        if (session == null) return;

        for (SessionListener l : sessionListeners) {
            l.sessionClosing(session);
        }
        session.close();
        session = null;
    }

    private boolean hasValidSession() {
        try {
            boolean valid = session != null && session.isValid();
            if (!valid) session = null;
            return valid;
        } catch (IllegalStateException ex) {
            session = null;
            return false;
        }
    }

    private void handleGetInfo(OutputStream out, String httpVersion) throws IOException {
        JsonObject info = DEFAULT_GET_INFO_FIELDS.deepCopy();
        info.addProperty("deviceID", inner.deviceId);
        info.addProperty("remoteName", inner.deviceName);
        info.addProperty("publicKey", Utils.toBase64(keys.publicKeyArray()));
        info.addProperty("deviceType", inner.deviceType.name().toUpperCase());

        synchronized (connectionLock) {
            info.addProperty("activeUser", connectingUsername != null ? connectingUsername : (hasValidSession() ? session.username() : ""));
        }

        out.write(httpVersion.getBytes());
        out.write(" 200 OK".getBytes());
        out.write(EOL);
        out.flush();

        out.write("Content-Type: application/json".getBytes());
        out.write(EOL);
        out.flush();

        out.write(EOL);
        out.write(info.toString().getBytes());
        out.flush();
    }

    private void handleAddUser(OutputStream out, Map<String, String> params, String httpVersion) throws GeneralSecurityException, IOException {
        String username = params.get("userName");
        if (username == null || username.isEmpty()) {
            LOGGER.error("Missing userName!");
            return;
        }

        String blobStr = params.get("blob");
        if (blobStr == null || blobStr.isEmpty()) {
            LOGGER.error("Missing blob!");
            return;
        }

        String clientKeyStr = params.get("clientKey");
        if (clientKeyStr == null || clientKeyStr.isEmpty()) {
            LOGGER.error("Missing clientKey!");
            return;
        }

        synchronized (connectionLock) {
            if (username.equals(connectingUsername)) {
                Log.d("CAPULLO", username + " is already trying to connect.");
                LOGGER.info("{} is already trying to connect.", username);

                out.write(httpVersion.getBytes());
                out.write(" 403 Forbidden".getBytes()); // I don't think this is the Spotify way
                out.write(EOL);
                out.write(EOL);
                out.flush();
                return;
            }
        }

        byte[] sharedKey = Utils.toByteArray(keys.computeSharedKey(Utils.fromBase64(clientKeyStr)));
        byte[] blobBytes = Utils.fromBase64(blobStr);
        byte[] iv = Arrays.copyOfRange(blobBytes, 0, 16);
        byte[] encrypted = Arrays.copyOfRange(blobBytes, 16, blobBytes.length - 20);
        byte[] checksum = Arrays.copyOfRange(blobBytes, blobBytes.length - 20, blobBytes.length);

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(sharedKey);
        byte[] baseKey = Arrays.copyOfRange(sha1.digest(), 0, 16);

        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(baseKey, "HmacSHA1"));
        hmac.update("checksum".getBytes());
        byte[] checksumKey = hmac.doFinal();

        hmac.init(new SecretKeySpec(baseKey, "HmacSHA1"));
        hmac.update("encryption".getBytes());
        byte[] encryptionKey = hmac.doFinal();

        hmac.init(new SecretKeySpec(checksumKey, "HmacSHA1"));
        hmac.update(encrypted);
        byte[] mac = hmac.doFinal();

        if (!Arrays.equals(mac, checksum)) {
            LOGGER.error("Mac and checksum don't match!");

            out.write(httpVersion.getBytes());
            out.write(" 400 Bad Request".getBytes()); // I don't think this is the Spotify way
            out.write(EOL);
            out.write(EOL);
            out.flush();
            return;
        }

        Cipher aes = Cipher.getInstance("AES/CTR/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Arrays.copyOfRange(encryptionKey, 0, 16), "AES"), new IvParameterSpec(iv));
        byte[] decrypted = aes.doFinal(encrypted);

        try {
            closeSession();
        } catch (IOException ex) {
            LOGGER.warn("Failed closing previous session.", ex);
        }

        try {
            synchronized (connectionLock) {
                connectingUsername = username;
            }

            Log.d("CAPULLO", "Accepted new user from " + params.get("deviceName") + ". {deviceId: " + inner.deviceId + "}");
            LOGGER.info("Accepted new user from {}. {deviceId: {}}", params.get("deviceName"), inner.deviceId);

            // Sending response
            String resp = DEFAULT_SUCCESSFUL_ADD_USER.toString();
            out.write(httpVersion.getBytes());
            out.write(" 200 OK".getBytes());
            out.write(EOL);
            out.write("Content-Length: ".getBytes());
            out.write(String.valueOf(resp.length()).getBytes());
            out.write(EOL);
            out.flush();

            out.write(EOL);
            out.write(resp.getBytes());
            out.flush();

            session = new Session.Builder(inner.conf)
                    .setDeviceId(inner.deviceId)
                    .setDeviceName(inner.deviceName)
                    .setDeviceType(inner.deviceType)
                    .setPreferredLocale(inner.preferredLocale)
                    .blob(username, decrypted)
                    .create();

            synchronized (connectionLock) {
                connectingUsername = null;
            }

            for (SessionListener l : sessionListeners) {
                l.sessionChanged(session);
            }
        } catch (Session.SpotifyAuthenticationException | MercuryClient.MercuryException | IOException | GeneralSecurityException ex) {
            Log.d("CAPULLO", "Couldn't establish a new session." + ex);
            LOGGER.error("Couldn't establish a new session.", ex);

            synchronized (connectionLock) {
                connectingUsername = null;
            }

            out.write(httpVersion.getBytes());
            out.write(" 500 Internal Server Error".getBytes()); // I don't think this is the Spotify way
            out.write(EOL);
            out.write(EOL);
            out.flush();
        }
    }

    public void addSessionListener(@NotNull SessionListener listener) {
        sessionListeners.add(listener);
    }

    public void removeSessionListener(@NotNull SessionListener listener) {
        sessionListeners.remove(listener);
    }

    public interface SessionListener {
        /**
         * The session instance is going to be closed after this call.
         *
         * @param session The old {@link Session}
         */
        void sessionClosing(@NotNull Session session);

        /**
         * The session instance changed. {@link #sessionClosing(Session)} has been already called.
         *
         * @param session The new {@link Session}
         */
        void sessionChanged(@NotNull Session session);
    }

    public static class Builder extends Session.AbsBuilder<Builder> {
        private int listenPort = -1;

        public Builder(Session.@NotNull Configuration conf) {
            super(conf);
        }

        public Builder setListenPort(int listenPort) {
            this.listenPort = listenPort;
            return this;
        }

        @NonNls
        public AndroidZeroconfServer create() throws IOException {
            return new AndroidZeroconfServer(new Inner(deviceType, deviceName, deviceId, preferredLocale, conf), listenPort);
        }
    }

    private static class Inner {
        final Random random = new SecureRandom();
        final Connect.DeviceType deviceType;
        final String deviceName;
        final String deviceId;
        final String preferredLocale;
        final Session.Configuration conf;

        Inner(@NotNull Connect.DeviceType deviceType, @NotNull String deviceName, @Nullable String deviceId, @NotNull String preferredLocale, @NotNull Session.Configuration conf) {
            this.deviceType = deviceType;
            this.deviceName = deviceName;
            this.preferredLocale = preferredLocale;
            this.conf = conf;
            this.deviceId = (deviceId == null || deviceId.isEmpty()) ? Utils.randomHexString(random, 40).toLowerCase() : deviceId;
        }
    }

    private class HttpRunner implements Runnable, Closeable {
        private final ServerSocket serverSocket;
        private final ExecutorService executorService = Executors.newCachedThreadPool(new NameThreadFactory((r) -> "zeroconf-client-" + r.hashCode()));
        private volatile boolean shouldStop = false;

        HttpRunner(int port) throws IOException {
            serverSocket = new ServerSocket(port);
            Log.d("CAPULLO", "Zeroconf HTTP server started successfully on port " + port + "!");
            LOGGER.info("Zeroconf HTTP server started successfully on port {}!", port);
        }

        @Override
        public void run() {
            while (!shouldStop) {
                try {
                    Socket socket = serverSocket.accept();
                    executorService.execute(() -> {
                        try {
                            Log.d("CAPULLO", "Handling request!");
                            handle(socket);
                            socket.close();
                        } catch (IOException ex) {
                            LOGGER.error("Failed handling request!", ex);
                        }
                    });
                } catch (IOException ex) {
                    if (!shouldStop) LOGGER.error("Failed handling connection!", ex);
                }
            }
        }

        private void handleRequest(@NotNull OutputStream out, @NotNull String httpVersion, @NotNull String action, @Nullable Map<String, String> params) {
            if (Objects.equals(action, "addUser")) {
                if (params == null) throw new IllegalArgumentException();

                try {
                    Log.d("CAPULLO", "Handling addUser!" + out + " " + params + " " + httpVersion);
                    handleAddUser(out, params, httpVersion);
                } catch (GeneralSecurityException | IOException ex) {
                    LOGGER.error("Failed handling addUser!", ex);
                }
            } else if (Objects.equals(action, "getInfo")) {
                try {
                    Log.d("CAPULLO", "Handling getInfo!" + out + " " + httpVersion);
                    handleGetInfo(out, httpVersion);
                } catch (IOException ex) {
                    LOGGER.error("Failed handling getInfo!", ex);
                }
            } else {
                LOGGER.warn("Unknown action: " + action);
            }
        }

        private void handle(@NotNull Socket socket) throws IOException {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();

            String[] requestLine = Utils.split(Utils.readLine(in), ' ');
            if (requestLine.length != 3) {
                Log.d("CAPULLO", "Unexpected request line: " + Arrays.toString(requestLine));
                LOGGER.warn("Unexpected request line: " + Arrays.toString(requestLine));
                return;
            }

            String method = requestLine[0];
            String path = requestLine[1];
            String httpVersion = requestLine[2];

            Map<String, String> headers = new HashMap<>();
            String header;
            while (!(header = Utils.readLine(in)).isEmpty()) {
                String[] split = Utils.split(header, ':');
                headers.put(split[0], split[1].trim());
            }

            if (!hasValidSession()) {
                Log.d("CAPULLO", "Handling request: " + method + " " + path + " " + httpVersion + ", headers: " + headers);
                LOGGER.trace("Handling request: {} {} {}, headers: {}", method, path, httpVersion, headers);
            }

            Map<String, String> params;
            if (Objects.equals(method, "POST")) {
                String contentType = headers.get("Content-Type");
                if (!Objects.equals(contentType, "application/x-www-form-urlencoded")) {
                    LOGGER.error("Bad Content-Type: " + contentType);
                    return;
                }

                String contentLengthStr = headers.get("Content-Length");
                if (contentLengthStr == null) {
                    LOGGER.error("Missing Content-Length header!");
                    return;
                }

                int contentLength = Integer.parseInt(contentLengthStr);
                byte[] body = new byte[contentLength];
                in.readFully(body);
                String bodyStr = new String(body);

                String[] pairs = Utils.split(bodyStr, '&');
                params = new HashMap<>(pairs.length);
                for (String pair : pairs) {
                    String[] split = Utils.split(pair, '=');
                    params.put(URLDecoder.decode(split[0], "UTF-8"),
                            URLDecoder.decode(split[1], "UTF-8"));
                }
            } else {
                params = parsePath(path);
            }

            String action = params.get("action");
            if (action == null) {
                Log.d("CAPULLO", "Request is missing action.");
                LOGGER.debug("Request is missing action.");
                return;
            }

            handleRequest(out, httpVersion, action, params);
        }

        @Override
        public void close() throws IOException {
            shouldStop = true;
            serverSocket.close();
            executorService.shutdown();
        }
    }
}

