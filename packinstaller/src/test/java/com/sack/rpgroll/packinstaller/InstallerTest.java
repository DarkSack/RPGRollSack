package com.sack.rpgroll.packinstaller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Lo que de verdad se escribe en el disco del comprador. */
class InstallerTest {

    @TempDir
    Path temp;

    private Path pack(String... rutas) throws IOException {
        Path raiz = Files.createDirectories(temp.resolve("pack"));

        for (String ruta : rutas) {
            Path archivo = raiz.resolve(ruta);
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, "nuevo: " + ruta);
        }

        return raiz;
    }

    private Path plugins(String... instalados) throws IOException {
        Path raiz = Files.createDirectories(temp.resolve("servidor/plugins"));
        Files.writeString(raiz.resolve("Paper.jar"), "x");

        for (String plugin : instalados) {
            Files.createDirectories(raiz.resolve(plugin));
        }

        return raiz;
    }

    private Installer.Resultado instalar(Path origen, Path destino) throws IOException {
        try (PackSource fuente = PackSource.desde(origen)) {
            Plan plan = new Planner(fuente, destino).planificar();
            return new Installer(fuente).instalar(plan, ruta -> {
            });
        }
    }

    @Test
    void copia_el_contenido_a_la_carpeta_del_plugin() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/bosses/lich.yml");
        Path destino = plugins("RPGRoll-Mobs");

        Installer.Resultado resultado = instalar(origen, destino);

        Path esperado = destino.resolve("RPGRoll-Mobs/mobs/bosses/lich.yml");

        assertTrue(Files.exists(esperado));
        assertEquals("nuevo: RPGRoll-Mobs/mobs/bosses/lich.yml", Files.readString(esperado));
        assertEquals(1, resultado.copiados());
        assertTrue(resultado.todoBien());
    }

    @Test
    void no_crea_carpetas_de_plugins_que_no_estan_instalados() throws IOException {
        Path origen = pack("RPGRoll-Magic/spells/bola.yml");
        Path destino = plugins("RPGRoll-Mobs");

        instalar(origen, destino);

        assertFalse(Files.exists(destino.resolve("RPGRoll-Magic")),
                "crear la carpeta haria pensar que el addon esta instalado");
    }

    @Test
    void respalda_lo_que_reemplaza() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");
        Path destino = plugins("RPGRoll-Mobs");

        Path anterior = destino.resolve("RPGRoll-Mobs/mobs/esqueleto.yml");
        Files.createDirectories(anterior.getParent());
        Files.writeString(anterior, "lo que el usuario habia ajustado");

        Installer.Resultado resultado = instalar(origen, destino);

        assertEquals(1, resultado.reemplazados());
        assertNotNull(resultado.copiaDeSeguridad());

        Path copia = resultado.copiaDeSeguridad()
                .resolve("RPGRoll-Mobs/mobs/esqueleto.yml");

        assertTrue(Files.exists(copia), "reemplazar sin respaldo es perder el trabajo de alguien");
        assertEquals("lo que el usuario habia ajustado", Files.readString(copia));
    }

    @Test
    void el_respaldo_queda_fuera_de_plugins() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");
        Path destino = plugins("RPGRoll-Mobs");

        Path anterior = destino.resolve("RPGRoll-Mobs/mobs/esqueleto.yml");
        Files.createDirectories(anterior.getParent());
        Files.writeString(anterior, "distinto");

        Installer.Resultado resultado = instalar(origen, destino);

        // Dentro de plugins/, el servidor intentaria cargarlo como un plugin.
        assertFalse(resultado.copiaDeSeguridad().startsWith(destino));
    }

    @Test
    void sin_reemplazos_no_hay_carpeta_de_respaldo() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");

        Installer.Resultado resultado = instalar(origen, plugins("RPGRoll-Mobs"));

        assertEquals(null, resultado.copiaDeSeguridad(),
                "una carpeta vacia solo genera dudas sobre si algo se rompio");
    }

    @Test
    void instalar_dos_veces_no_cambia_nada_la_segunda() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");
        Path destino = plugins("RPGRoll-Mobs");

        instalar(origen, destino);
        Installer.Resultado segunda = instalar(origen, destino);

        assertEquals(0, segunda.copiados());
        assertEquals(0, segunda.reemplazados());
        assertEquals(null, segunda.copiaDeSeguridad());
    }

    @Test
    void no_deja_archivos_temporales() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/a.yml", "RPGRoll-Mobs/mobs/b.yml");
        Path destino = plugins("RPGRoll-Mobs");

        instalar(origen, destino);

        try (var flujo = Files.walk(destino)) {
            assertFalse(flujo.anyMatch(ruta -> ruta.getFileName().toString().endsWith(".rpgroll-tmp")));
        }
    }

    @Test
    void reparte_entre_varios_plugins_a_la_vez() throws IOException {
        Path origen = pack(
                "RPGRoll-Mobs/mobs/esqueleto.yml",
                "RPGRoll-Magic/spells/bola.yml",
                "RPGRoll-Traps/traps/pinchos.yml");

        Path destino = plugins("RPGRoll-Mobs", "RPGRoll-Magic", "RPGRoll-Traps");

        Installer.Resultado resultado = instalar(origen, destino);

        assertEquals(3, resultado.copiados());
        assertTrue(Files.exists(destino.resolve("RPGRoll-Mobs/mobs/esqueleto.yml")));
        assertTrue(Files.exists(destino.resolve("RPGRoll-Magic/spells/bola.yml")));
        assertTrue(Files.exists(destino.resolve("RPGRoll-Traps/traps/pinchos.yml")));
    }
}
