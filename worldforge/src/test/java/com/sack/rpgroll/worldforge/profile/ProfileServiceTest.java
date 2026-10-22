package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.TestCatalog;
import com.sack.rpgroll.worldforge.api.Result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileServiceTest {

    @TempDir
    Path dir;
    ProfileService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new ProfileService(dir, 4096, TestCatalog::validator);
        service.load(id -> {
            try {
                return ProfileCodecTest.resource(id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void built_in_presets_are_copied_once_and_usable() throws IOException {
        assertEquals(ProfileService.BUILT_IN.size(), service.all().size());
        assertTrue(service.all().stream().allMatch(ProfileService.Entry::usable));
        // An admin's edit survives a reload: built-ins are only copied when missing.
        Files.writeString(dir.resolve("presets/flat.yml"), Files.readString(dir.resolve("presets/flat.yml")).replace("Plano", "Mi plano"));
        service.load(id -> "no se usa");
        assertEquals("Mi plano", service.preset("flat").orElseThrow().profile().name());
    }

    @Test
    void a_broken_file_is_reported_and_left_alone() throws IOException {
        Path broken = dir.resolve("presets/roto.yml");
        Files.writeString(broken, "schema: 1\nid: [roto\n");
        service.load(id -> null);
        assertTrue(service.loadProblems().stream().anyMatch(p -> p.field().contains("roto.yml")));
        assertEquals("schema: 1\nid: [roto\n", Files.readString(broken));
    }

    @Test
    void duplicate_makes_a_draft_that_cannot_create_worlds_until_published() {
        Result<ProfileService.Entry> d = service.duplicate("flat", "mi-plano", "Mi plano");
        assertTrue(d.isOk(), d.toString());
        assertTrue(d.value().draft());
        assertFalse(d.value().usable());
        Result<ProfileService.Entry> pub = service.publish("mi-plano", null);
        assertTrue(pub.isOk(), pub.toString());
        assertTrue(service.preset("mi-plano").orElseThrow().usable());
        assertTrue(service.draft("mi-plano").isEmpty());
    }

    @Test
    void saving_over_someone_elses_change_is_refused() {
        ProfileService.Entry d = service.duplicate("flat", "x", "X").value();
        assertTrue(service.saveDraft(d.profile().withSeed(1L), d.hash()).isOk());
        // Second editor still has the old version open.
        Result<ProfileService.Entry> stale = service.saveDraft(d.profile().withSeed(2L), d.hash());
        assertFalse(stale.isOk());
        assertEquals(1L, service.draft("x").orElseThrow().profile().seed());
    }

    @Test
    void publishing_a_draft_with_errors_is_refused() {
        ProfileService.Entry d = service.duplicate("vanilla", "x", "X").value();
        service.saveDraft(d.profile().withProvider("empty").withStructures(true), d.hash());
        Result<ProfileService.Entry> pub = service.publish("x", null);
        assertFalse(pub.isOk());
        assertEquals("structures", pub.errors().getFirst().field());
    }

    @Test
    void export_is_reproducible_and_has_a_manifest() throws IOException {
        Path f = service.export("resource-rich").value();
        String first = Files.readString(f);
        service.export("resource-rich");
        assertEquals(first, Files.readString(f));
        String manifest = Files.readString(dir.resolve("exports/resource-rich.manifest"));
        assertTrue(manifest.contains("sha256: " + ProfileService.sha256(first.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }

    @Test
    void import_goes_to_drafts_and_checks_name_size_and_content() throws IOException {
        Files.writeString(dir.resolve("imports/nuevo.yml"), "schema: 1\nid: nuevo\nprovider: flat\nlayers: [{block: stone, height: 2}]\n");
        Result<ProfileService.Entry> ok = service.importFile("nuevo.yml");
        assertTrue(ok.isOk(), ok.toString());
        assertTrue(service.draft("nuevo").isPresent());
        assertTrue(service.preset("nuevo").isEmpty(), "importar nunca publica");

        assertFalse(service.importFile("../presets/flat.yml").isOk());
        assertFalse(service.importFile("C:\\Windows\\win.ini").isOk());

        Files.writeString(dir.resolve("imports/grande.yml"), "schema: 1\n" + "# relleno\n".repeat(1000));
        assertTrue(service.importFile("grande.yml").errors().getFirst().message().contains("límite"));

        Files.writeString(dir.resolve("imports/malo.yml"), "schema: 1\nprovider: flat\n");
        assertFalse(service.importFile("malo.yml").isOk());
    }

    @Test
    void an_import_with_semantic_errors_is_kept_as_draft_with_warnings() throws IOException {
        Files.writeString(dir.resolve("imports/raro.yml"), "schema: 1\nid: raro\nprovider: vanilla\nbiomes: [mordor]\n");
        Result<ProfileService.Entry> r = service.importFile("raro.yml");
        assertTrue(r.isOk());
        assertTrue(r.problems().stream().anyMatch(p -> p.message().contains("impide publicar")));
        assertFalse(service.publish("raro", null).isOk());
    }
}
