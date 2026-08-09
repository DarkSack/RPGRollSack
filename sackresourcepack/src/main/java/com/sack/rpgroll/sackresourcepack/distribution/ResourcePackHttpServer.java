package com.sack.rpgroll.sackresourcepack.distribution;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.Executors;

/**
 * Host HTTP embebido para el ZIP — usa {@code com.sun.net.httpserver}, ya
 * incluido en el JDK, sin ninguna dependencia extra. Pensado como la
 * opción "cero configuración"; si el server tiene un dominio propio con
 * HTTPS real, usar {@code public-url} en vez de este host es preferible
 * (los clientes de Minecraft son más flexibles con HTTPS).
 */
public class ResourcePackHttpServer {

    /** Prefijo fijo del segundo contexto — sirve archivos sueltos del árbol fusionado (ej. para el preview de skull). */
    private static final String ASSETS_CONTEXT = "/assets/";

    private final Plugin plugin;
    private final String bindAddress;
    private final int port;
    private final String path;
    private final java.util.function.Supplier<File> zipFileSupplier;
    private final java.util.function.Supplier<File> mergedDirectorySupplier;

    private HttpServer server;

    public ResourcePackHttpServer(Plugin plugin, String bindAddress, int port, String path,
            java.util.function.Supplier<File> zipFileSupplier) {
        this(plugin, bindAddress, port, path, zipFileSupplier, null);
    }

    public ResourcePackHttpServer(Plugin plugin, String bindAddress, int port, String path,
            java.util.function.Supplier<File> zipFileSupplier, java.util.function.Supplier<File> mergedDirectorySupplier) {
        this.plugin = plugin;
        this.bindAddress = bindAddress;
        this.port = port;
        this.path = path.startsWith("/") ? path : "/" + path;
        this.zipFileSupplier = zipFileSupplier;
        this.mergedDirectorySupplier = mergedDirectorySupplier;
    }

    public void start() {

        try {
            server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            server.createContext(path, this::handle);

            if (mergedDirectorySupplier != null) {
                server.createContext(ASSETS_CONTEXT, this::handleAsset);
            }

            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            plugin.getLogger().info("✔ Host HTTP de SackResourcePack escuchando en " + bindAddress + ":" + port
                    + path);
        } catch (IOException e) {
            plugin.getLogger().severe("✘ No se pudo iniciar el host HTTP en " + bindAddress + ":" + port + ": "
                    + e.getMessage());
        }
    }

    public void stop() {

        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    private void handle(HttpExchange exchange) throws IOException {

        File zipFile = zipFileSupplier.get();

        if (zipFile == null || !zipFile.isFile()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] bytes = Files.readAllBytes(zipFile.toPath());

        exchange.getResponseHeaders().add("Content-Type", "application/zip");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public String localUrl() {

        String host = bindAddress.equals("0.0.0.0") ? "localhost" : bindAddress;
        return "http://" + host + ":" + port + path;
    }

    /** Base para pedir un archivo suelto del árbol fusionado (ej. una textura), null si no se habilitó ese contexto. */
    public String assetBaseUrl() {

        if (mergedDirectorySupplier == null) {
            return null;
        }

        String host = bindAddress.equals("0.0.0.0") ? "localhost" : bindAddress;
        return "http://" + host + ":" + port + ASSETS_CONTEXT;
    }

    private void handleAsset(HttpExchange exchange) throws IOException {

        File mergedDirectory = mergedDirectorySupplier.get();

        if (mergedDirectory == null || !mergedDirectory.isDirectory()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        String requestPath = exchange.getRequestURI().getPath().substring(ASSETS_CONTEXT.length());
        File requested = new File(mergedDirectory, requestPath);

        String mergedCanonical = mergedDirectory.getCanonicalPath();
        String requestedCanonical = requested.getCanonicalPath();

        if (!requestedCanonical.startsWith(mergedCanonical + File.separator) || !requested.isFile()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] bytes = Files.readAllBytes(requested.toPath());

        exchange.getResponseHeaders().add("Content-Type", guessContentType(requestPath));
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private String guessContentType(String path) {

        if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".json")) {
            return "application/json";
        } else if (path.endsWith(".ogg")) {
            return "audio/ogg";
        }

        return "application/octet-stream";
    }

}
