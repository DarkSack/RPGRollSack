package com.sack.rpgroll.structures.catalog;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.config.CategorySettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructureLoaderTest {

    @TempDir File dir;
    File structures;
    File schematics;

    private static final Map<WorkbenchTier, WorkbenchSettings.Size> MAX = Map.of(
            WorkbenchTier.CONSTRUCTOR, new WorkbenchSettings.Size(16, 16, 16),
            WorkbenchTier.INGENIERO, new WorkbenchSettings.Size(32, 32, 32),
            WorkbenchTier.ARQUITECTO, new WorkbenchSettings.Size(64, 96, 64));

    @BeforeEach
    void setUp() {
        structures = new File(dir, "structures");
        schematics = new File(dir, "schematics");
        structures.mkdirs();
        schematics.mkdirs();
    }

    private void yml(String id, String body) throws IOException {
        Files.writeString(new File(structures, id + ".yml").toPath(), body);
    }

    private void schem(String name) throws IOException {
        Files.writeString(new File(schematics, name).toPath(), "x");
    }

    /** Caja de w×h×l bloques; el nombre {@code bad.nbt} falla como un archivo ilegible. */
    private static BlockSource box(File f) {
        if (f.getName().equals("bad.nbt")) {
            throw new UncheckedIOException(new IOException("NBT corrupto"));
        }
        int w = f.getName().equals("big.nbt") ? 17 : 4;
        int h = f.getName().equals("tall.nbt") ? 40 : 1;
        int l = f.getName().equals("long.nbt") ? 20 : 1;
        List<SourceBlock> blocks = new ArrayList<>();
        for (int i = 0; i < w * h * l; i++) {
            blocks.add(new SourceBlock(i, 0, 0, null, null));
        }
        int fw = w, fh = h, fl = l;
        return new BlockSource() {
            public int width() { return fw; }
            public int height() { return fh; }
            public int length() { return fl; }
            public List<SourceBlock> blocks() { return blocks; }
        };
    }

    private StructureLoader.LoadResult load() {
        return StructureLoader.load(structures, schematics, CategorySettings.from(new org.bukkit.configuration.file.YamlConfiguration(), w -> fail(w)),
                MAX, id -> !id.equals("minecraft:nope"), StructureLoaderTest::box);
    }

    private static List<String> ids(List<StructureDefinition> l) {
        return l.stream().map(StructureDefinition::id).toList();
    }

    private static final String BASE = "name: {es: \"Casa\"}\ncategory: casas\ntier: 1\nschematic: ok.nbt\n"
            + "materials:\n  - {item: oak_planks, amount: 8}\n";

    @Test
    void validFileLoadsWithDefaults() throws IOException {
        schem("ok.nbt");
        yml("ok", BASE);
        var r = load();
        assertTrue(r.problems().isEmpty(), r.problems().toString());
        var d = r.valid().getFirst();
        assertFalse(d.instant());
        assertEquals(BuildOrder.BOTTOM_UP, d.order());
        assertEquals(16, d.blocksPerCycle());
        assertEquals(20, d.ticksPerCycle());
        assertEquals("rpgrollstructures.build.ok", d.permission());
        assertEquals("minecraft:oak_planks", d.materials().getFirst().key());
        assertEquals(WorkbenchTier.CONSTRUCTOR, d.tier());
        assertEquals(4, d.volume());
    }

    @Test
    void badEntriesAreReportedAndOthersLoad() throws IOException {
        schem("ok.nbt");
        schem("big.nbt");
        yml("ok", BASE);
        yml("mat", BASE.replace("oak_planks", "minecraft:nope"));
        yml("nofile", BASE.replace("ok.nbt", "falta.nbt"));
        yml("big", BASE.replace("ok.nbt", "big.nbt"));
        var r = load();
        assertEquals(List.of("ok"), ids(r.valid()));
        assertTrue(r.problems().contains(new Problem("mat.yml", "materials[0]", "material desconocido: minecraft:nope")));
        assertTrue(r.problems().contains(new Problem("nofile.yml", "schematic", "no existe el archivo falta.nbt")));
        assertTrue(r.problems().contains(new Problem("big.yml", "schematic", "más grande que el nivel 1 (17×1×1)")));
    }

    @Test
    void rotatedFitCountsAndTooBigInHeightFails() throws IOException {
        schem("long.nbt");
        schem("tall.nbt");
        // 4×1×20: no cabe sin girar en 16 de largo, y girado (20×1×4) tampoco; en nivel 2 (32) sí.
        yml("l1", BASE.replace("ok.nbt", "long.nbt"));
        yml("l2", BASE.replace("ok.nbt", "long.nbt").replace("tier: 1", "tier: 2"));
        yml("t2", BASE.replace("ok.nbt", "tall.nbt").replace("tier: 1", "tier: 2"));
        var r = load();
        assertEquals(List.of("l2"), ids(r.valid()));
        assertEquals(2, r.problems().size());
    }

    @Test
    void otherValidationFailures() throws IOException {
        schem("ok.nbt");
        schem("bad.nbt");
        yml("BadId", BASE);
        yml("noname", BASE.replace("name: {es: \"Casa\"}", "name: {en: \"House\"}"));
        yml("cat", BASE.replace("casas", "nada"));
        yml("tier", BASE.replace("tier: 1", "tier: 4"));
        yml("unreadable", BASE.replace("ok.nbt", "bad.nbt"));
        yml("amount", BASE.replace("amount: 8", "amount: 0"));
        yml("order", BASE + "build: {order: zigzag}\n");
        yml("custom", BASE + "build: {order: custom}\n");
        yml("cycle", BASE + "build: {blocks-per-cycle: 0, ticks-per-cycle: -1}\n");
        var r = load();
        assertTrue(r.valid().isEmpty(), ids(r.valid()).toString());
        Map<String, String> byFile = new java.util.HashMap<>();
        r.problems().forEach(p -> byFile.merge(p.file(), p.key(), (a, b) -> a + "," + b));
        assertEquals("id", byFile.get("BadId.yml"));
        assertEquals("name.es", byFile.get("noname.yml"));
        assertEquals("category", byFile.get("cat.yml"));
        assertEquals("tier", byFile.get("tier.yml"));
        assertEquals("schematic", byFile.get("unreadable.yml"));
        assertEquals("materials[0]", byFile.get("amount.yml"));
        assertEquals("build.order", byFile.get("order.yml"));
        assertEquals("build.custom", byFile.get("custom.yml"));
        assertEquals("build.blocks-per-cycle,build.ticks-per-cycle", byFile.get("cycle.yml"));
    }

    @Test
    void anyOfUsesFirstAsKey() throws IOException {
        schem("ok.nbt");
        yml("ok", BASE.replace("{item: oak_planks, amount: 8}", "{any_of: [minecraft:Glass, glass_pane], amount: 8}"));
        var m = load().valid().getFirst().materials().getFirst();
        assertEquals("minecraft:glass", m.key());
        assertEquals(List.of("minecraft:glass", "minecraft:glass_pane"), m.anyOf());
    }

    @Test
    void customOrderAndInstantAreRead() throws IOException {
        schem("ok.nbt");
        yml("ok", BASE + "build: {mode: instant, order: custom, custom: [minecraft:stone]}\n"
                + "placement: {ground: true, replace: [minecraft:short_grass]}\npermission: mi.permiso\n");
        var d = load().valid().getFirst();
        assertTrue(d.instant());
        assertEquals(BuildOrder.CUSTOM, d.order());
        assertEquals(1, d.customOrder().size());
        assertTrue(d.placement().needsGround());
        assertEquals(1, d.placement().replace().size());
        assertEquals("mi.permiso", d.permission());
    }

    @Test
    void schematicOutsideTheFolderIsRejected() throws IOException {
        Files.writeString(new File(dir, "ok.nbt").toPath(), "x"); // existe, pero fuera de schematics/
        yml("fuera", BASE.replace("ok.nbt", "../ok.nbt"));
        var r = load();
        assertTrue(r.valid().isEmpty());
        assertTrue(r.problems().contains(new Problem("fuera.yml", "schematic", "fuera de la carpeta schematics")),
                r.problems().toString());
    }

    @Test
    void definitionCarriesTheContentHash() throws IOException {
        schem("ok.nbt");
        yml("a", BASE);
        var first = load().valid().getFirst().sourceHash();
        assertNotNull(first);
        assertEquals(64, first.length(), "SHA-256 en hexadecimal");
        // Guardar otra vez el mismo esquemático (otros bytes: cabecera gzip, metadatos) no cambia el hash.
        Files.writeString(new File(schematics, "ok.nbt").toPath(), "otros bytes, mismos bloques");
        assertEquals(first, load().valid().getFirst().sourceHash(), "mismos bloques, mismo hash");
        schem("big.nbt");
        yml("a", BASE.replace("ok.nbt", "big.nbt").replace("tier: 1", "tier: 2"));
        assertNotEquals(first, load().valid().getFirst().sourceHash(), "otros bloques, otro hash");
    }

    @Test
    void contentHashDependsOnBlockDataNotOnListOrder() {
        org.bukkit.block.data.BlockData a = org.mockito.Mockito.mock(org.bukkit.block.data.BlockData.class);
        org.bukkit.block.data.BlockData b = org.mockito.Mockito.mock(org.bukkit.block.data.BlockData.class);
        org.mockito.Mockito.when(a.getAsString()).thenReturn("minecraft:oak_stairs[facing=north]");
        org.mockito.Mockito.when(b.getAsString()).thenReturn("minecraft:oak_stairs[facing=south]");
        SourceBlock p = new SourceBlock(0, 0, 0, a, null), q = new SourceBlock(1, 0, 0, a, null);
        SourceBlock qSouth = new SourceBlock(1, 0, 0, b, null);
        assertEquals(StructureLoader.contentHash(src(List.of(p, q))), StructureLoader.contentHash(src(List.of(q, p))));
        assertNotEquals(StructureLoader.contentHash(src(List.of(p, q))),
                StructureLoader.contentHash(src(List.of(p, qSouth))));
    }

    private StructureLoader.LoadResult loadWithModules() {
        return StructureLoader.load(structures, schematics, CategorySettings.from(new org.bukkit.configuration.file.YamlConfiguration(), w -> fail(w)),
                MAX, id -> !id.equals("minecraft:nope"), StructureLoaderTest::box,
                type -> type.equals("farm") ? java.util.Optional.of(new com.sack.rpgroll.structures.module.FarmModule())
                        : java.util.Optional.empty());
    }

    @Test
    void unknownModuleTypeIsAProblem() throws IOException {
        schem("ok.nbt");
        yml("ok", BASE);
        yml("raro", BASE + "module: {type: teleporter}\n");
        yml("plano", BASE + "module: farm\n");
        var r = loadWithModules();
        assertEquals(List.of("ok"), ids(r.valid()), "sin module: carga sin módulo; el tipo desconocido no carga");
        assertNull(r.valid().getFirst().module());
        assertTrue(r.problems().contains(new Problem("raro.yml", "module.type", "módulo desconocido: teleporter")),
                r.problems().toString());
        assertTrue(r.problems().stream().anyMatch(p -> p.file().equals("plano.yml") && p.key().equals("module")),
                r.problems().toString());
    }

    @Test
    void moduleValidateLinkageErrorIsAProblem() throws IOException {
        schem("ok.nbt");
        yml("rota", BASE + "module: {type: rota}\n");
        com.sack.rpgroll.structures.api.StructureModule broken = new com.sack.rpgroll.structures.api.StructureModule() {
            @Override public String type() { return "rota"; }
            @Override public List<Problem> validate(org.bukkit.configuration.ConfigurationSection c) {
                throw new NoClassDefFoundError("io/lumine/mythic/api/MythicProvider");
            }
            @Override public void onBuilt(com.sack.rpgroll.structures.api.BuiltStructure b) { }
            @Override public void onRemoved(com.sack.rpgroll.structures.api.BuiltStructure b) { }
        };
        var r = StructureLoader.load(structures, schematics,
                CategorySettings.from(new org.bukkit.configuration.file.YamlConfiguration(), w -> fail(w)), MAX,
                id -> true, StructureLoaderTest::box, t -> java.util.Optional.of(broken));
        assertTrue(r.valid().isEmpty());
        assertTrue(r.problems().stream().anyMatch(p -> p.key().equals("module")), r.problems().toString());
    }

    @Test
    void unknownUpgradeFromIsAProblem() throws IOException {
        schem("ok.nbt");
        yml("casa", BASE);
        yml("casa2", BASE + "upgrade_from: casa\n");
        yml("mala", BASE + "upgrade_from: nada\n");
        yml("propia", BASE + "upgrade_from: propia\n");
        yml("sobre", BASE + "upgrade_from: mala\n"); // apunta a una que no entró
        var r = load();
        assertEquals(List.of("casa", "casa2"), ids(r.valid()));
        assertNull(r.valid().get(0).upgradeFrom());
        assertEquals("casa", r.valid().get(1).upgradeFrom());
        assertEquals(List.of("mala.yml", "propia.yml", "sobre.yml"),
                r.problems().stream().map(Problem::file).sorted().toList());
        assertTrue(r.problems().stream().allMatch(p -> p.key().equals("upgrade_from")), r.problems().toString());
    }

    @Test
    void farmNoteIsLoaded() throws IOException {
        schem("ok.nbt");
        yml("granja", BASE + "module:\n  type: farm\n  note:\n    es: \"Crece con agua y luz\"\n"
                + "    en: \"Grows with water and light\"\n");
        var r = loadWithModules();
        assertTrue(r.problems().isEmpty(), r.problems().toString());
        StructureDefinition d = r.valid().getFirst();
        assertEquals("farm", d.module());
        assertEquals("Crece con agua y luz", d.moduleNote("es"));
        assertEquals("Grows with water and light", d.moduleNote("en"));
        assertEquals("Crece con agua y luz", d.moduleNote("pt_BR"), "sin traducción: el español");
        assertEquals("farm", d.moduleConfig().getString("type"));
    }

    private static BlockSource src(List<SourceBlock> blocks) {
        return new BlockSource() {
            public int width() { return 2; }
            public int height() { return 1; }
            public int length() { return 1; }
            public List<SourceBlock> blocks() { return blocks; }
        };
    }
}
