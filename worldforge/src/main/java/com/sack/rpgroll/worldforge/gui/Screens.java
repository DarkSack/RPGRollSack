package com.sack.rpgroll.worldforge.gui;

import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.core.WorldForgePlugin;
import com.sack.rpgroll.worldforge.profile.Profile;
import com.sack.rpgroll.worldforge.profile.ProfileService;
import com.sack.rpgroll.worldforge.profile.TemplateRule;
import com.sack.rpgroll.worldforge.protection.Protections;
import com.sack.rpgroll.worldforge.regen.JobState;
import com.sack.rpgroll.worldforge.regen.Jobs.Job;
import com.sack.rpgroll.worldforge.regen.Regeneration;
import com.sack.rpgroll.worldforge.worlds.WorldService;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Every WorldForge screen. Each button calls the same service as the matching command, with
 * the same permission, so the GUI cannot do anything the commands would refuse.
 */
final class Screens {

    private static final int BACK = 49;
    private static final int[] RADII = {0, 1, 2, 4, 8};

    private final WorldForgePlugin plugin;
    private final Gui gui;

    Screens(WorldForgePlugin plugin, Gui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    // ── main ────────────────────────────────────────────

    Menu main(Player p) {
        long usable = plugin.profiles().all().stream().filter(ProfileService.Entry::usable).count();
        return gui.menu(p, "WorldForge", 3)
                .item(4, Material.PAPER, "Estado", List.of(plugin.platform().name(), usable + " presets utilizables",
                        plugin.ready() ? "Base de datos lista" : "✘ Base de datos: " + plugin.notReadyReason()), null, null)
                .item(10, Material.GRASS_BLOCK, "Mundos", List.of("Crear, cargar, descargar, cambiar de preset"), "worldforge.worlds.view", this::worlds)
                .item(11, Material.BOOK, "Presets", List.of("Validar, duplicar, semilla, publicar, exportar"), "worldforge.presets.view", this::presets)
                .item(12, Material.IRON_PICKAXE, "Regeneración", List.of("Planes, confirmaciones, respaldos y recuperación"), "worldforge.regeneration.view", this::jobs)
                .item(13, Material.SHIELD, "Zonas protegidas", List.of("Lo que la regeneración nunca toca"), "worldforge.protection.view", this::protections)
                .item(14, Material.COMPASS, "Diagnóstico", List.of("En el chat"), "worldforge.diagnostics", this::diagnostics)
                .item(16, Material.BARRIER, "Cerrar", List.of(), null, gui::close);
    }

    private void diagnostics(Player p) {
        gui.close(p);
        plugin.diagnostics().report().whenComplete((lines, err) -> {
            if (err != null) {
                plugin.messages().send(p, "failed", "reason", err.toString());
                return;
            }
            lines.forEach(l -> p.sendMessage(net.kyori.adventure.text.Component.text("  " + l,
                    l.startsWith("✘") ? NamedTextColor.RED : NamedTextColor.GRAY)));
        });
    }

    // ── worlds ──────────────────────────────────────────

    private void worlds(Player p) {
        if (!ready(p)) {
            return;
        }
        plugin.worlds().list().thenAccept(views -> {
            Menu m = gui.menu(p, "Mundos", 6);
            for (int i = 0; i < Math.min(45, views.size()); i++) {
                WorldService.View v = views.get(i);
                m.item(i, v.loaded() ? Material.GRASS_BLOCK : Material.GRAY_DYE, v.name(), worldLore(v), "worldforge.worlds.view", pl -> world(pl, v));
            }
            back(m, this::mainAgain);
            m.item(50, Material.EMERALD, "Crear mundo", List.of("Eliges un preset y luego el nombre"), "worldforge.worlds.create",
                    pl -> pickPreset(pl, "Crear mundo: preset", id -> askWorldName(pl, id)));
            gui.open(p, m);
        });
    }

    private static List<String> worldLore(WorldService.View v) {
        List<String> l = new ArrayList<>();
        l.add(v.key() + (v.loaded() ? " · cargado" : " · sin cargar"));
        l.add(v.managed() ? "Administrado · " + v.status() + (v.profileId() == null ? "" : " · preset " + v.profileId())
                : v.status());
        return l;
    }

    private void world(Player p, WorldService.View v) {
        Menu m = gui.menu(p, "Mundo " + v.name(), 3).item(4, Material.MAP, v.name(), worldLore(v), null, null);
        if (v.managed() && !v.loaded()) {
            m.item(10, Material.LIME_DYE, "Cargar", List.of(), "worldforge.worlds.manage",
                    pl -> after(pl, plugin.worlds().load(v.name(), pl.getName()), w -> {
                        plugin.messages().send(pl, "world-loaded", "name", v.name());
                        worlds(pl);
                    }));
        }
        if (v.managed() && v.loaded()) {
            m.item(10, Material.RED_DYE, "Descargar", List.of("Guarda y descarga; quien esté dentro va a la aparición principal"),
                    "worldforge.worlds.manage", pl -> after(pl, plugin.worlds().unload(v.name(), pl.getName()), x -> {
                        plugin.messages().send(pl, "world-unloaded", "name", v.name());
                        worlds(pl);
                    }));
        }
        m.item(11, Material.PAPER, "Inspeccionar", List.of("En el chat"), "worldforge.worlds.view", pl -> {
            gui.close(pl);
            after(pl, plugin.worlds().inspect(v.name()), lines -> lines.forEach(l -> gui.tell(pl, l, NamedTextColor.GRAY)));
        });
        if (v.managed()) {
            m.item(12, Material.ANVIL, "Cambiar preset", List.of("Solo afecta a los chunks nuevos"), "worldforge.generation.apply",
                    pl -> pickPreset(pl, "Nuevo preset de " + v.name(), id -> after(pl, plugin.worlds().applyPreset(v.name(), id, pl.getName()),
                            text -> gui.tell(pl, text, NamedTextColor.GREEN))));
        }
        if (v.loaded()) {
            m.item(14, Material.IRON_PICKAXE, "Regenerar alrededor de ti", List.of("Elige el radio; luego verás el plan"),
                    "worldforge.regeneration.plan", pl -> inWorld(pl, v.name(), () -> pickRadius(pl, "Regenerar: radio en chunks",
                            r -> after(pl, plugin.regeneration().plan(v.name(),
                                    Regeneration.Scope.radius(pl.getLocation().getBlockX(), pl.getLocation().getBlockZ(), r), pl.getName()),
                                    pv -> preview(pl, pv)))));
            m.item(15, Material.SHIELD, "Proteger alrededor de ti", List.of("Elige el radio; luego el nombre de la zona"),
                    "worldforge.protection.manage", pl -> inWorld(pl, v.name(), () -> pickRadius(pl, "Proteger: radio en chunks", r -> {
                        int x = pl.getLocation().getBlockX(), z = pl.getLocation().getBlockZ(), d = r * 16 + 8;
                        gui.ask(pl, "Nombre de la zona (minúsculas, números, - o _)", name -> plugin.platform()
                                .async(() -> plugin.protections().add(name, v.name(), x - d, z - d, x + d, z + d, "creada desde el menú"))
                                .thenAccept(res -> report(pl, res, a -> gui.tell(pl, "Zona " + a.id() + " protegida: " + a.minX() + ", "
                                        + a.minZ() + " → " + a.maxX() + ", " + a.maxZ() + ".", NamedTextColor.GREEN))));
                    })));
        }
        m.item(22, Material.ARROW, "Volver", List.of(), null, this::worlds);
        gui.open(p, m);
    }

    private void askWorldName(Player p, String presetId) {
        gui.ask(p, "Nombre del mundo nuevo con «" + presetId + "»", name -> {
            plugin.messages().send(p, "working");
            after(p, plugin.worlds().create(name, presetId, p.getName()), w -> plugin.messages().send(p, "world-created",
                    "name", w.getName(), "preset", presetId, "seed", String.valueOf(w.getSeed())));
        });
    }

    private void inWorld(Player p, String world, Runnable then) {
        if (!p.getWorld().getName().equals(world)) {
            gui.tell(p, "Entra en «" + world + "» y vuelve a abrir el menú: el radio se mide desde donde estás.", NamedTextColor.RED);
            return;
        }
        then.run();
    }

    // ── presets ─────────────────────────────────────────

    private void presets(Player p) {
        Menu m = gui.menu(p, "Presets", 6);
        List<ProfileService.Entry> all = plugin.profiles().all();
        for (int i = 0; i < Math.min(45, all.size()); i++) {
            ProfileService.Entry e = all.get(i);
            m.item(i, e.usable() ? Material.BOOK : e.draft() ? Material.WRITABLE_BOOK : Material.RED_DYE, e.profile().id(),
                    presetLore(e), "worldforge.presets.view", pl -> preset(pl, e));
        }
        back(m, this::mainAgain);
        m.item(50, Material.WRITABLE_BOOK, "Nuevo borrador", List.of("Copia de «custom» con el nombre que elijas"), "worldforge.presets.edit",
                pl -> gui.ask(pl, "Id del borrador nuevo", id -> async(pl, () -> plugin.profiles().duplicate("custom", id, id), en -> preset(pl, en))));
        gui.open(p, m);
    }

    private static List<String> presetLore(ProfileService.Entry e) {
        Profile pr = e.profile();
        List<String> l = new ArrayList<>();
        l.add(pr.name() + " · " + pr.provider() + (e.draft() ? " · borrador" : ""));
        l.add("Semilla " + (pr.seed() == null ? "aleatoria" : pr.seed()) + " · " + pr.environment() + " · versión " + e.hash());
        if (!pr.ores().isEmpty()) {
            l.add(pr.ores().size() + " reglas de menas");
        }
        if (!pr.templates().isEmpty()) {
            l.add(pr.templates().size() + " plantillas: " + String.join(", ", pr.templates().stream().map(TemplateRule::structure).distinct().toList()));
        }
        e.problems().stream().limit(3).forEach(x -> l.add(x.toString()));
        return l;
    }

    private void preset(Player p, ProfileService.Entry e) {
        String id = e.profile().id();
        Menu m = gui.menu(p, (e.draft() ? "Borrador " : "Preset ") + id, 3).item(4, Material.BOOK, id, presetLore(e), null, null)
                .item(10, Material.PAPER, "Validar", List.of("Resultado en el chat"), "worldforge.presets.view", pl -> {
                    List<Problem> problems = plugin.validator().validate(e.profile());
                    gui.tell(pl, problems.stream().noneMatch(Problem::isError) ? id + " es válido." : id + " tiene errores:",
                            problems.stream().noneMatch(Problem::isError) ? NamedTextColor.GREEN : NamedTextColor.RED);
                    plugin.messages().problems(pl, problems);
                })
                .item(11, Material.WRITABLE_BOOK, "Duplicar", List.of("Crea un borrador nuevo"), "worldforge.presets.edit",
                        pl -> gui.ask(pl, "Id del duplicado de «" + id + "»", nid -> async(pl, () -> plugin.profiles().duplicate(id, nid, nid), en -> preset(pl, en))));
        if (e.draft()) {
            m.item(12, Material.WHEAT_SEEDS, "Fijar semilla", List.of("Un número, o «ninguna»"), "worldforge.presets.edit",
                    pl -> gui.ask(pl, "Semilla para «" + id + "»", raw -> {
                        Long seed;
                        try {
                            seed = raw.equalsIgnoreCase("ninguna") ? null : Long.parseLong(raw);
                        } catch (NumberFormatException ex) {
                            gui.tell(pl, "«" + raw + "» no es un número entero.", NamedTextColor.RED);
                            return;
                        }
                        // e.hash() is the version this menu showed: someone else's change in between is refused.
                        async(pl, () -> plugin.profiles().saveDraft(e.profile().withSeed(seed), e.hash()), en -> preset(pl, en));
                    }));
            m.item(13, Material.LIME_DYE, "Publicar", List.of("Lo deja listo para crear mundos"), "worldforge.presets.edit", pl -> {
                Optional<ProfileService.Entry> now = plugin.profiles().draft(id);
                if (now.isEmpty() || !now.get().hash().equals(e.hash())) {
                    gui.tell(pl, "El borrador cambió desde que abriste este menú; vuelve a abrirlo antes de publicar.", NamedTextColor.RED);
                    return;
                }
                String current = plugin.profiles().preset(id).map(ProfileService.Entry::hash).orElse(null);
                async(pl, () -> plugin.profiles().publish(id, current), done -> {
                    plugin.messages().send(pl, "preset-published", "id", id);
                    plugin.records().history(pl.getName(), "preset.publish", id, done.hash());
                    preset(pl, done);
                });
            });
        } else {
            m.item(14, Material.CHEST, "Exportar", List.of("A exports/ con manifiesto SHA-256"), "worldforge.presets.export",
                    pl -> async(pl, () -> plugin.profiles().export(id), f -> plugin.messages().send(pl, "preset-exported", "file",
                            plugin.getDataFolder().toPath().relativize(f).toString())));
        }
        if (e.usable()) {
            m.item(15, Material.EMERALD, "Crear mundo con este", List.of(), "worldforge.worlds.create", pl -> askWorldName(pl, id));
        }
        m.item(22, Material.ARROW, "Volver", List.of(), null, this::presets);
        gui.open(p, m);
    }

    private void pickPreset(Player p, String title, Consumer<String> chosen) {
        Menu m = gui.menu(p, title, 6);
        List<ProfileService.Entry> usable = plugin.profiles().all().stream().filter(ProfileService.Entry::usable).toList();
        for (int i = 0; i < Math.min(45, usable.size()); i++) {
            ProfileService.Entry e = usable.get(i);
            m.item(i, Material.BOOK, e.profile().id(), presetLore(e), null, pl -> chosen.accept(e.profile().id()));
        }
        back(m, this::mainAgain);
        gui.open(p, m);
    }

    private void pickRadius(Player p, String title, IntConsumer chosen) {
        Menu m = gui.menu(p, title, 3);
        for (int i = 0; i < RADII.length; i++) {
            int r = RADII[i], side = 2 * r + 1;
            m.item(11 + i, Material.STONE_BUTTON, "Radio " + r, List.of(side + "×" + side + " chunks"), null, pl -> chosen.accept(r));
        }
        m.item(22, Material.ARROW, "Volver", List.of(), null, this::mainAgain);
        gui.open(p, m);
    }

    // ── regeneration ────────────────────────────────────

    private void jobs(Player p) {
        if (!ready(p)) {
            return;
        }
        plugin.regeneration().jobs().recent(45).thenAccept(list -> {
            Menu m = gui.menu(p, "Trabajos", 6);
            for (int i = 0; i < list.size(); i++) {
                Job j = list.get(i);
                m.item(i, jobIcon(j.state()), j.id(), jobLore(j), "worldforge.regeneration.view", pl -> job(pl, j.id()));
            }
            back(m, this::mainAgain);
            gui.open(p, m);
        });
    }

    private static Material jobIcon(JobState s) {
        return switch (s) {
            case PLANNED -> Material.PAPER;
            case PENDING_RESTART -> Material.CLOCK;
            case COMPLETED, RESTORED -> Material.LIME_DYE;
            case NEEDS_RECOVERY -> Material.RED_DYE;
            case FAILED, CANCELLED -> Material.GRAY_DYE;
            default -> Material.YELLOW_DYE;
        };
    }

    private static List<String> jobLore(Job j) {
        List<String> l = new ArrayList<>();
        l.add((j.state() == JobState.NEEDS_RECOVERY ? "✘ " : "") + j.type() + " · " + j.world() + " · " + j.state());
        l.add("Hecho " + j.done() + "/" + j.total() + (j.backupId() == null ? "" : " · respaldo " + j.backupId()));
        if (j.message() != null) {
            wrap(j.message(), l);
        }
        return l;
    }

    private void job(Player p, String id) {
        plugin.regeneration().jobs().get(id).thenAccept(found -> {
            if (found.isEmpty()) {
                gui.tell(p, "Ya no existe el trabajo " + id + ".", NamedTextColor.RED);
                return;
            }
            Job j = found.get();
            Menu m = gui.menu(p, "Trabajo " + id, 3).item(4, jobIcon(j.state()), id, jobLore(j), null, null);
            if (j.state() == JobState.PLANNED) {
                m.item(10, Material.LIME_WOOL, "Confirmar…", List.of("Pide una segunda confirmación"), "worldforge.regeneration.execute",
                        pl -> confirmScreen(pl, j));
            }
            if (j.state() == JobState.PLANNED || j.state() == JobState.PENDING_RESTART) {
                m.item(12, Material.BARRIER, "Cancelar", List.of("No toca nada"), "worldforge.regeneration.cancel",
                        pl -> after(pl, plugin.regeneration().cancel(id, pl.getName()), text -> {
                            gui.tell(pl, text, NamedTextColor.GREEN);
                            jobs(pl);
                        }));
            }
            if ((j.state() == JobState.COMPLETED || j.state() == JobState.NEEDS_RECOVERY) && j.backupId() != null) {
                m.item(13, Material.CHEST, "Restaurar el respaldo…", List.of("Crea un plan de restauración"), "worldforge.regeneration.plan",
                        pl -> after(pl, plugin.regeneration().planRestore(id, pl.getName()), pv -> preview(pl, pv)));
            }
            if (j.state() == JobState.NEEDS_RECOVERY) {
                m.item(14, Material.WHITE_BANNER, "Aceptar el estado actual…", List.of("El respaldo se conserva"), "worldforge.regeneration.execute",
                        pl -> yesNo(pl, "¿Aceptar " + id + " tal como está?", List.of("Se desbloquea el mundo sin restaurar."),
                                "worldforge.regeneration.execute", () -> after(pl, plugin.regeneration().accept(id, pl.getName()),
                                        text -> gui.tell(pl, text, NamedTextColor.GREEN))));
            }
            m.item(22, Material.ARROW, "Volver", List.of(), null, this::jobs);
            gui.open(p, m);
        });
    }

    private void preview(Player p, Regeneration.Preview pv) {
        Job j = pv.job();
        List<String> lore = new ArrayList<>();
        wrap(j.message(), lore);
        if (j.type().equals(Regeneration.RESTORE)) {
            lore.add(j.total() + " archivos (" + Regeneration.mb(pv.backupBytes()) + ")");
            lore.add("⚠ Cada región (32×32 chunks) vuelve entera al respaldo");
        } else {
            lore.add(j.total() + " chunks · " + pv.protectedChunks() + " protegidos · " + pv.neverGenerated() + " sin generar");
            lore.add("⚠ Se pierde todo lo construido en esos chunks");
        }
        lore.add("Respaldo previo: " + Regeneration.mb(pv.backupBytes()));
        lore.add(pv.restart() ? "Se aplica en el PRÓXIMO ARRANQUE" : "Se aplica ya (el mundo se descarga unos segundos)");
        Menu m = gui.menu(p, "Plan " + j.id(), 3).item(4, Material.MAP, "Plan " + j.id(), lore, null, null)
                .item(11, Material.LIME_WOOL, "Confirmar…", List.of("Pide una segunda confirmación"), "worldforge.regeneration.execute",
                        pl -> confirmScreen(pl, j))
                .item(15, Material.BARRIER, "Descartar el plan", List.of(), "worldforge.regeneration.cancel",
                        pl -> after(pl, plugin.regeneration().cancel(j.id(), pl.getName()), text -> {
                            gui.tell(pl, text, NamedTextColor.GRAY);
                            gui.close(pl);
                        }));
        gui.open(p, m);
    }

    private void confirmScreen(Player p, Job j) {
        yesNo(p, "¿Seguro? " + j.id(), List.of(j.type() + " en " + j.world(), "⚠ No se puede deshacer salvo con el respaldo"),
                "worldforge.regeneration.execute", () -> {
                    gui.close(p);
                    after(p, plugin.regeneration().confirm(j.id(), j.token(), p.getName(),
                                    text -> gui.tell(p, text, text.contains("✘") ? NamedTextColor.RED : NamedTextColor.GREEN)),
                            text -> gui.tell(p, text, NamedTextColor.GREEN));
                });
    }

    private void yesNo(Player p, String title, List<String> lore, String permission, Runnable yes) {
        gui.open(p, gui.menu(p, title, 3)
                .item(13, Material.PAPER, title, lore, null, null)
                .item(11, Material.LIME_WOOL, "Sí", lore, permission, pl -> yes.run())
                .item(15, Material.RED_WOOL, "No", List.of(), null, gui::close));
    }

    // ── protection ──────────────────────────────────────

    private void protections(Player p) {
        Protections pr = plugin.protections();
        Menu m = gui.menu(p, "Zonas protegidas", 6);
        List<Protections.Area> areas = pr.areas();
        for (int i = 0; i < Math.min(45, areas.size()); i++) {
            Protections.Area a = areas.get(i);
            m.item(i, Material.SHIELD, a.id(), List.of(a.world() + " · " + a.minX() + ", " + a.minZ() + " → " + a.maxX() + ", " + a.maxZ(),
                    a.note()), "worldforge.protection.view", pl -> protection(pl, a));
        }
        List<String> info = new ArrayList<>(List.of(pr.spawnRadius() + " chunks alrededor de cada aparición"));
        pr.blockedReason().ifPresent(r -> info.add("✘ " + r));
        m.item(45, Material.BEACON, "Siempre protegido", info, null, null);
        back(m, this::mainAgain);
        gui.open(p, m);
    }

    private void protection(Player p, Protections.Area a) {
        gui.open(p, gui.menu(p, "Zona " + a.id(), 3)
                .item(4, Material.SHIELD, a.id(), List.of(a.world(), a.minX() + ", " + a.minZ() + " → " + a.maxX() + ", " + a.maxZ(), a.note()), null, null)
                .item(11, Material.RED_DYE, "Quitar la protección…", List.of(), "worldforge.protection.manage",
                        pl -> yesNo(pl, "¿Quitar " + a.id() + "?", List.of("La regeneración podrá tocar esa zona"), "worldforge.protection.manage", () -> {
                            if (!plugin.protections().areas().contains(a)) {
                                gui.tell(pl, "La zona cambió desde que abriste el menú; vuelve a abrirlo.", NamedTextColor.RED);
                                return;
                            }
                            plugin.platform().async(() -> plugin.protections().remove(a.id())).thenAccept(r -> report(pl, r, x -> {
                                gui.tell(pl, "Zona " + x.id() + " ya no está protegida.", NamedTextColor.YELLOW);
                                plugin.records().history(pl.getName(), "protection.remove", x.world(), x.id());
                                protections(pl);
                            }));
                        }))
                .item(22, Material.ARROW, "Volver", List.of(), null, this::protections));
    }

    // ── helpers ─────────────────────────────────────────

    private void mainAgain(Player p) {
        gui.open(p, main(p));
    }

    /** Only in 6-row menus; detail screens (3 rows) have their own "Volver" at 22. */
    private static Menu back(Menu m, Consumer<Player> to) {
        return m.item(BACK, Material.ARROW, "Menú principal", List.of(), null, to);
    }

    private boolean ready(Player p) {
        if (!plugin.ready()) {
            plugin.messages().send(p, "db-unavailable", "reason", plugin.notReadyReason());
            return false;
        }
        return true;
    }

    private <T> void async(Player p, java.util.function.Supplier<Result<T>> work, Consumer<T> ok) {
        after(p, plugin.platform().async(work), ok);
    }

    /** Reports a service result to the player, on the player's thread. */
    private <T> void after(Player p, CompletableFuture<Result<T>> f, Consumer<T> ok) {
        f.whenComplete((r, err) -> plugin.platform().entity(p, () -> {
            if (err != null) {
                plugin.messages().send(p, "failed", "reason", String.valueOf(err.getCause() == null ? err : err.getCause()));
                return;
            }
            report(p, r, ok);
        }));
    }

    private <T> void report(Player p, Result<T> r, Consumer<T> ok) {
        if (!r.isOk()) {
            Problem first = r.errors().getFirst();
            plugin.messages().send(p, "failed", "reason", (first.field().isEmpty() ? "" : first.field() + ": ") + first.message());
            return;
        }
        ok.accept(r.value());
        plugin.messages().problems(p, r.problems());
    }

    private static void wrap(String text, List<String> into) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 40 && !line.isEmpty()) {
                into.add(line.toString());
                line.setLength(0);
            }
            line.append(line.isEmpty() ? "" : " ").append(word);
        }
        if (!line.isEmpty()) {
            into.add(line.toString());
        }
    }
}
