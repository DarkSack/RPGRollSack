package com.sack.rpgroll.licensing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Locale;

import javax.net.ssl.SSLHandshakeException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseHttpTest {

    private static boolean onWindowsOrMac() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win") || os.contains("mac") || os.contains("darwin");
    }

    // El CertificateException real llega envuelto varios niveles adentro del
    // SSLHandshakeException, así que mirar solo la excepción de arriba no alcanza.
    @Test
    void aWrappedCertificateFailureIsRecognised() {
        IOException wrapped = new IOException("falló la petición",
                new SSLHandshakeException("handshake") {
                    private static final long serialVersionUID = 1L;
                });

        assertTrue(LicenseHttp.isTrustFailure(wrapped));
    }

    @Test
    void aDeeplyNestedCertificateFailureIsRecognised() {
        IOException deep = new IOException("nivel 1",
                new IOException("nivel 2",
                        new IOException("nivel 3", new CertificateException("cadena no válida"))));

        assertTrue(LicenseHttp.isTrustFailure(deep));
    }

    // Un corte de red o un DNS caído NO son fallos de confianza: reintentar
    // contra otro almacén de certificados no arreglaría nada.
    @Test
    void aPlainNetworkFailureIsNotATrustFailure() {
        assertFalse(LicenseHttp.isTrustFailure(new UnknownHostException("sin DNS")));
        assertFalse(LicenseHttp.isTrustFailure(new SocketTimeoutException("timeout")));
        assertFalse(LicenseHttp.isTrustFailure(new IOException("conexión reiniciada")));
    }

    // Java prohíbe la auto-causa directa, pero deja armar un ciclo de dos.
    // Recorrer la cadena sin límite colgaría el arranque del servidor.
    @Test
    void aCyclicCauseChainDoesNotHang() {
        IOException first = new IOException("a");
        IOException second = new IOException("b", first);
        first.initCause(second);

        assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> assertFalse(LicenseHttp.isTrustFailure(first)));
    }

    @Test
    void theDefaultClientIsAlwaysAvailable() {
        assertTrue(LicenseHttp.defaultClient() != null);
    }

    // En Windows y macOS hay un almacén del sistema al que recurrir; en Linux
    // el del JDK ya es ese mismo, así que devolver vacío es lo correcto.
    @Test
    void thePlatformTrustClientMatchesTheOperatingSystem() {
        assertTrue(LicenseHttp.platformTrustClient().isPresent() == onWindowsOrMac());
    }
}
