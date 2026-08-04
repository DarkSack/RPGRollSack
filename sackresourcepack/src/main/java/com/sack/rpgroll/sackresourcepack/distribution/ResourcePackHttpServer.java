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

    private final Plugin plugin;
    private final String bindAddress;
    private final int port;
    private final String path;
    private final java.util.function.Supplier<File> zipFileSupplier;

    private HttpServer server;

    public ResourcePackHttpServer(Plugin plugin, String bindAddress, int port, String path,
            java.util.function.Supplier<File> zipFileSupplier) {
        this.plugin = plugin;
        this.bindAddress = bindAddress;
        this.port = port;
        this.path = path.startsWith("/") ? path : "/" + path;
        this.zipFileSupplier = zipFileSupplier;
    }

    public void start() {

        try {
            server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            server.createContext(path, this::handle);
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

}
