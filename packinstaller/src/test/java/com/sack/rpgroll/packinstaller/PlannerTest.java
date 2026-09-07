package com.sack.rpgroll.packinstaller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Lo que se prueba acá es el criterio, no la copia.
 *
 * <p>Decidir mal es el fallo caro: copiar donde no toca, o no copiar y no
 * decirlo. Que `Files.write` funcione no hace falta comprobarlo.
 */
class PlannerTest {

    @TempDir
    Path temp;

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private Path pack(String... rutas) throws IOException {
        Path raiz = Files.createDirectories(temp.resolve("pack"));

        for (String ruta : rutas) {
            Path archivo = raiz.resolve(ruta);
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, "contenido: " + ruta);
        }

        return raiz;
    }

    /** Una carpeta plugins creíble: con un .jar, como cualquier servidor real. */
    private Path plugins(String... pluginsInstalados) throws IOException {
        Path raiz = Files.createDirectories(temp.resolve("servidor/plugins"));
        Files.writeString(raiz.resolve("AlgunPlugin.jar"), "no es un jar de verdad");

        for (String plugin : pluginsInstalados) {
            Files.createDirectories(raiz.resolve(plugin));
        }

        return raiz;
    }

    private Plan planificar(Path origenPack, Path carpetaPlugins) throws IOException {
        try (PackSource fuente = PackSource.desde(origenPack)) {
            return new Planner(fuente, carpetaPlugins).planificar();
        }
    }

    private long conAccion(Plan plan, Plan.Accion accion) {
        return plan.entradas().stream().filter(e -> e.accion() == accion).count();
    }

    // ------------------------------------------------------------------
    // El comportamiento central: omitir lo que no está instalado
    // ------------------------------------------------------------------

    @Test
    void omite_el_contenido_de_un_plugin_que_no_esta_instalado() throws IOException {
        Path origen = pack(
                "RPGRoll-Mobs/mobs/esqueleto.yml",
                "RPGRoll-Magic/spells/bola.yml");

        // Solo Mobs está instalado.
        Plan plan = planificar(origen, plugins("RPGRoll-Mobs"));

        assertEquals(1, conAccion(plan, Plan.Accion.NUEVO));
        assertEquals(1, conAccion(plan, Plan.Accion.SIN_PLUGIN));
        assertEquals(List.of("RPGRoll-Magic"), plan.pluginsOmitidos());
        assertEquals(List.of("RPGRoll-Mobs"), plan.pluginsAfectados());
    }

    @Test
    void omitir_no_es_un_error() throws IOException {
        Path origen = pack("RPGRoll-Magic/spells/bola.yml");
        Plan plan = planificar(origen, plugins("RPGRoll-Mobs"));

        // Que falte un addon es lo normal, no un problema: nadie compra los 24.
        assertFalse(plan.tieneErrores());
    }

    @Test
    void conserva_la_estructura_de_subcarpetas() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/bosses/lich.yml");
        Path destino = plugins("RPGRoll-Mobs");

        Plan plan = planificar(origen, destino);

        assertEquals(destino.resolve("RPGRoll-Mobs/mobs/bosses/lich.yml"),
                plan.aInstalar().get(0).destino());
    }

    // ------------------------------------------------------------------
    // Conflictos
    // ------------------------------------------------------------------

    @Test
    void un_archivo_identico_no_se_vuelve_a_copiar() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");
        Path destino = plugins("RPGRoll-Mobs");

        Path yaExiste = destino.resolve("RPGRoll-Mobs/mobs/esqueleto.yml");
        Files.createDirectories(yaExiste.getParent());
        Files.writeString(yaExiste, "contenido: RPGRoll-Mobs/mobs/esqueleto.yml");

        Plan plan = planificar(origen, destino);

        assertEquals(1, conAccion(plan, Plan.Accion.IDENTICO));
        assertTrue(plan.aInstalar().isEmpty(), "reinstalar lo mismo no debe escribir nada");
    }

    @Test
    void un_archivo_distinto_se_marca_como_reemplazo() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");
        Path destino = plugins("RPGRoll-Mobs");

        Path yaExiste = destino.resolve("RPGRoll-Mobs/mobs/esqueleto.yml");
        Files.createDirectories(yaExiste.getParent());
        Files.writeString(yaExiste, "lo que el usuario habia editado a mano");

        Plan plan = planificar(origen, destino);

        assertEquals(1, conAccion(plan, Plan.Accion.REEMPLAZA));
    }

    // ------------------------------------------------------------------
    // Validaciones del destino
    // ------------------------------------------------------------------

    @Test
    void rechaza_una_carpeta_que_no_parece_de_plugins() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");
        Path cualquiera = Files.createDirectories(temp.resolve("escritorio"));

        Plan plan = planificar(origen, cualquiera);

        assertTrue(plan.tieneErrores(),
                "volcar el pack en una carpeta cualquiera deja un desorden que hay que limpiar a mano");
    }

    @Test
    void acepta_una_carpeta_de_plugins_sin_jars_pero_con_carpetas_de_plugin() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");

        Path raiz = Files.createDirectories(temp.resolve("otro/plugins"));
        Files.createDirectories(raiz.resolve("RPGRoll-Mobs"));

        assertFalse(planificar(origen, raiz).tieneErrores());
    }

    @Test
    void rechaza_una_carpeta_inexistente() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");

        assertTrue(planificar(origen, temp.resolve("no/existe")).tieneErrores());
    }

    // ------------------------------------------------------------------
    // Validaciones del pack
    // ------------------------------------------------------------------

    @Test
    void avisa_si_una_carpeta_solo_difiere_en_mayusculas() throws IOException {
        Path origen = pack("rpgroll-mobs/mobs/esqueleto.yml");
        Plan plan = planificar(origen, plugins("RPGRoll-Mobs"));

        // En Windows pasaria desapercibido; en el Linux del servidor, no.
        assertTrue(plan.avisos().stream()
                        .anyMatch(a -> a.nivel() == Plan.Aviso.Nivel.ADVERTENCIA
                                && a.mensaje().contains("RPGRoll-Mobs")),
                "un error de mayusculas tiene que avisarse, no omitirse en silencio");
    }

    @Test
    void avisa_de_una_carpeta_que_no_es_ningun_plugin_conocido() throws IOException {
        Path origen = pack("RPGRoll-Mob/mobs/esqueleto.yml");
        Plan plan = planificar(origen, plugins("RPGRoll-Mobs"));

        assertTrue(plan.avisos().stream()
                .anyMatch(a -> a.nivel() == Plan.Aviso.Nivel.ADVERTENCIA
                        && a.mensaje().contains("RPGRoll-Mob")));
    }

    @Test
    void un_nombre_desconocido_avisa_pero_no_impide_instalar() throws IOException {
        Path origen = pack(
                "RPGRoll-AddonDelFuturo/algo.yml",
                "RPGRoll-Mobs/mobs/esqueleto.yml");

        Plan plan = planificar(origen, plugins("RPGRoll-Mobs"));

        // Un instalador viejo no puede romperse porque salga un addon nuevo.
        assertFalse(plan.tieneErrores());
        assertEquals(1, plan.aInstalar().size());
    }

    @Test
    void la_documentacion_de_la_raiz_no_se_instala() throws IOException {
        Path origen = pack("LEEME.md", "RPGRoll-Mobs/mobs/esqueleto.yml");
        Plan plan = planificar(origen, plugins("RPGRoll-Mobs"));

        assertEquals(1, conAccion(plan, Plan.Accion.DOCUMENTACION));
        assertEquals(1, plan.aInstalar().size(), "nadie quiere un LEEME.md dentro de plugins/");
    }

    @Test
    void un_pack_vacio_es_un_error() throws IOException {
        Path vacio = Files.createDirectories(temp.resolve("pack"));

        assertTrue(planificar(vacio, plugins("RPGRoll-Mobs")).tieneErrores());
    }

    // ------------------------------------------------------------------
    // Zip
    // ------------------------------------------------------------------

    @Test
    void lee_un_pack_comprimido_igual_que_una_carpeta() throws IOException {
        Path zip = temp.resolve("pack.zip");

        try (ZipOutputStream salida = new ZipOutputStream(Files.newOutputStream(zip))) {
            salida.putNextEntry(new ZipEntry("RPGRoll-Mobs/mobs/esqueleto.yml"));
            salida.write("tipo: esqueleto".getBytes());
            salida.closeEntry();
        }

        Plan plan = planificar(zip, plugins("RPGRoll-Mobs"));

        assertEquals(1, plan.aInstalar().size());
    }

    @Test
    void descarta_rutas_que_escapan_del_zip() throws IOException {
        Path zip = temp.resolve("malicioso.zip");

        try (ZipOutputStream salida = new ZipOutputStream(Files.newOutputStream(zip))) {
            salida.putNextEntry(new ZipEntry("../../../fuera.yml"));
            salida.write("no deberia salir de aqui".getBytes());
            salida.closeEntry();

            salida.putNextEntry(new ZipEntry("RPGRoll-Mobs/mobs/esqueleto.yml"));
            salida.write("tipo: esqueleto".getBytes());
            salida.closeEntry();
        }

        Plan plan = planificar(zip, plugins("RPGRoll-Mobs"));

        // La entrada peligrosa ni siquiera llega al plan: lo que no esta en la
        // lista no se puede copiar por error mas adelante.
        assertEquals(1, plan.entradas().size());
        assertEquals("RPGRoll-Mobs/mobs/esqueleto.yml",
                plan.entradas().get(0).archivo().rutaRelativa());
    }

    @Test
    void las_rutas_peligrosas_se_reconocen_una_por_una() {
        assertFalse(PackSource.rutaSegura("../fuera.yml"));
        assertFalse(PackSource.rutaSegura("a/../../fuera.yml"));
        assertFalse(PackSource.rutaSegura("/etc/passwd"));
        assertFalse(PackSource.rutaSegura("C:/Windows/algo.dll"));
        assertFalse(PackSource.rutaSegura(""));

        assertTrue(PackSource.rutaSegura("RPGRoll-Mobs/mobs/esqueleto.yml"));
        assertTrue(PackSource.rutaSegura("RPGRoll-Items/packs/mi..pack/x.yml"));
    }

    // ------------------------------------------------------------------

    @Test
    void el_plan_no_escribe_nada() throws IOException {
        Path origen = pack("RPGRoll-Mobs/mobs/esqueleto.yml");
        Path destino = plugins("RPGRoll-Mobs");

        planificar(origen, destino);

        assertFalse(Files.exists(destino.resolve("RPGRoll-Mobs/mobs/esqueleto.yml")),
                "revisar tiene que ser inofensivo o no sirve de nada revisar");
    }

    @Test
    void el_nombre_del_pack_sale_del_zip_sin_la_extension() throws IOException {
        Path zip = temp.resolve("reino-no-muerto.zip");

        try (ZipOutputStream salida = new ZipOutputStream(Files.newOutputStream(zip))) {
            salida.putNextEntry(new ZipEntry("RPGRoll-Mobs/x.yml"));
            salida.write("a: 1".getBytes());
            salida.closeEntry();
        }

        try (PackSource fuente = PackSource.desde(zip)) {
            assertEquals("reino-no-muerto", fuente.nombre());
        }
    }

    @Test
    void rechaza_un_archivo_que_no_es_zip_ni_carpeta() throws IOException {
        Path suelto = temp.resolve("cosa.yml");
        Files.writeString(suelto, "a: 1");

        assertTrue(org.junit.jupiter.api.Assertions
                .assertThrows(IOException.class, () -> PackSource.desde(suelto))
                .getMessage().contains(".zip"));
    }

    @Test
    void corregirMayusculas_solo_responde_ante_una_diferencia_real() {
        assertEquals("RPGRoll-Mobs", RpgRollPlugins.corregirMayusculas("rpgroll-mobs"));
        assertEquals("RPGRoll-FX", RpgRollPlugins.corregirMayusculas("RPGRoll-fx"));

        // Ya esta bien escrito: no hay nada que corregir.
        assertNotNull(RpgRollPlugins.corregirMayusculas("rpgroll-MOBS"));
        assertEquals(null, RpgRollPlugins.corregirMayusculas("RPGRoll-Mobs"));
        assertEquals(null, RpgRollPlugins.corregirMayusculas("OtraCosa"));
    }
}
