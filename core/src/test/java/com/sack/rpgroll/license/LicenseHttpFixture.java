package com.sack.rpgroll.license;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Servidor HTTP local (el del JDK, sin dependencias) para probar los
 * proveedores de licencia contra respuestas reales en vez de mockear el
 * HttpClient — así se ejercita también el parseo del cuerpo.
 */
final class LicenseHttpFixture implements AutoCloseable {

    private final HttpServer server;
    private volatile String lastRequestBody;

    private LicenseHttpFixture(HttpServer server) {
        this.server = server;
    }

    static LicenseHttpFixture responding(int statusCode, String body) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        LicenseHttpFixture fixture = new LicenseHttpFixture(server);

        server.createContext("/verify", exchange -> {

            fixture.lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, payload.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });

        server.start();
        return fixture;
    }

    String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/verify";
    }

    String lastRequestBody() {
        return lastRequestBody;
    }

    /** Una URL que no escucha nadie, para simular una caída del servicio. */
    static String unreachableEndpoint() {
        return "http://127.0.0.1:1/verify";
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
