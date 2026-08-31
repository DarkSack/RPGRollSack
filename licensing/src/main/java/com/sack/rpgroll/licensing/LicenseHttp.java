package com.sack.rpgroll.licensing;

import java.io.IOException;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;

/**
 * Clientes HTTP compartidos por los proveedores de licencia, con una
 * segunda oportunidad cuando el almacén de certificados del JDK está roto.
 * <p>
 * <b>Por qué existe.</b> Algunos servidores corren sobre un JDK cuyo
 * {@code cacerts} está vacío, desactualizado o interceptado por un antivirus
 * corporativo. En esa máquina TODA conexión HTTPS de Java falla con
 * {@code PKIX path building failed}, aunque el certificado del servicio sea
 * perfectamente válido — se reprodujo exactamente así durante las pruebas.
 * El comprador quedaba bloqueado por un problema de su JVM, no de su compra.
 * <p>
 * Ante un fallo de confianza se reintenta una sola vez usando el almacén de
 * certificados del sistema operativo (el mismo en el que confía su
 * navegador). Eso NO es desactivar la verificación: se cambia un conjunto de
 * anclas de confianza por otro, y un certificado inválido sigue siendo
 * rechazado. Desactivar la validación sería además un agujero de negocio —
 * cualquiera podría interceptar la conexión y responder que su licencia es
 * válida.
 */
final class LicenseHttp {

    static final Duration TIMEOUT = Duration.ofSeconds(8);

    private LicenseHttp() {
    }

    static HttpClient defaultClient() {
        return HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    /**
     * ¿El fallo fue por no poder validar la cadena de certificados? Se
     * recorre la cadena de causas porque el {@code CertificateException}
     * real viene envuelto varios niveles adentro del {@code SSLHandshakeException}.
     */
    static boolean isTrustFailure(IOException error) {

        // Java prohíbe que una excepción sea causa de sí misma, pero NO impide
        // un ciclo más largo (a → b → a), así que recorrer la cadena sin límite
        // puede colgarse. Ninguna cadena legítima llega a esta profundidad.
        Throwable cause = error;

        for (int depth = 0; cause != null && depth < 32; depth++, cause = cause.getCause()) {

            if (cause instanceof CertificateException || cause instanceof SSLHandshakeException) {
                return true;
            }
        }

        return false;
    }

    /**
     * Cliente que confía en el almacén de certificados del sistema operativo.
     * <p>
     * Solo existe en Windows ({@code Windows-ROOT}) y macOS
     * ({@code KeychainStore}). En Linux el almacén del JDK ya ES el del
     * sistema en la mayoría de las distribuciones, así que no hay un segundo
     * lugar donde buscar y se devuelve vacío.
     */
    static Optional<HttpClient> platformTrustClient() {

        String storeType = platformStoreType();

        if (storeType == null) {
            return Optional.empty();
        }

        try {
            KeyStore store = KeyStore.getInstance(storeType);
            store.load(null, null);

            TrustManagerFactory factory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(store);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, factory.getTrustManagers(), null);

            return Optional.of(HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .sslContext(context)
                    .build());

        } catch (Exception e) {
            // Si el almacén del sistema tampoco se puede abrir, no queda
            // alternativa: se informa el fallo original, no este.
            return Optional.empty();
        }
    }

    private static String platformStoreType() {

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            return "Windows-ROOT";
        }

        if (os.contains("mac") || os.contains("darwin")) {
            return "KeychainStore";
        }

        return null;
    }

}
