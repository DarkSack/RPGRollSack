package com.sack.rpgroll.worldforge.command;

import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.core.Messages;
import com.sack.rpgroll.worldforge.core.WorldForgePlugin;
import com.sack.rpgroll.worldforge.dimension.Dimensions;
import com.sack.rpgroll.worldforge.generation.GenerationProvider;
import com.sack.rpgroll.worldforge.profile.Profile;
import com.sack.rpgroll.worldforge.profile.ProfileService;
import com.sack.rpgroll.worldforge.protection.Protections;
import com.sack.rpgroll.worldforge.regen.JobState;
import com.sack.rpgroll.worldforge.regen.Jobs;
import com.sack.rpgroll.worldforge.regen.Regeneration;
import com.sack.rpgroll.worldforge.terrain.TerrainSpec;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * {@code /worldforge}. Parses, checks the permission of each subcommand (console included)
 * and calls the services; it holds no logic of its own, so the GUI gets the same behaviour.
 */
public final class WorldForgeCommand implements TabExecutor {

    private record Sub(String permission, String usage, int minArgs, Handler handler) {
    }

    @FunctionalInterface
    private interface Handler {
        void run(CommandSender sender, String[] args);
    }

    private final WorldForgePlugin plugin;
    private final Messages msg;
    private final Map<String, Sub> subs = new LinkedHashMap<>();

    public WorldForgeCommand(WorldForgePlugin plugin) {
        this.plugin = plugin;
        this.msg = plugin.messages();

        add("help", "worldforge.worlds.view", "/wf help", 0, this::help);
        add("status", "worldforge.worlds.view", "/wf status", 0, this::status);
        add("gui", "worldforge.worlds.view", "/wf gui", 0, this::gui);
        add("diagnostics", "worldforge.diagnostics", "/wf diagnostics", 0, this::diagnostics);
        add("reload-config", "worldforge.reload", "/wf reload-config", 0, this::reload);

        add("world list", "worldforge.worlds.view", "/wf world list", 0, this::worldList);
        add("world inspect", "worldforge.worlds.view", "/wf world inspect <mundo>", 1, this::worldInspect);
        add("world create", "worldforge.worlds.create", "/wf world create <nombre> <preset>", 2, this::worldCreate);
        add("world load", "worldforge.worlds.manage", "/wf world load <mundo>", 1, this::worldLoad);
        add("world unload", "worldforge.worlds.manage", "/wf world unload <mundo>", 1, this::worldUnload);
        add("world import", "worldforge.worlds.create", "/wf world import <nombre> <preset>", 2, this::worldImport);
        add("world tp", "worldforge.worlds.teleport", "/wf world tp <mundo> [jugador]", 1, this::worldTp);

        add("preset list", "worldforge.presets.view", "/wf preset list", 0, this::presetList);
        add("preset info", "worldforge.presets.view", "/wf preset info <id>", 1, this::presetInfo);
        add("preset validate", "worldforge.presets.view", "/wf preset validate <id>", 1, this::presetValidate);
        add("preset create", "worldforge.presets.edit", "/wf preset create <id> [base]", 1, this::presetCreate);
        add("preset duplicate", "worldforge.presets.edit", "/wf preset duplicate <origen> <nuevo>", 2, this::presetDuplicate);
        add("preset seed", "worldforge.presets.edit", "/wf preset seed <borrador> <número|ninguna>", 2, this::presetSeed);
        add("preset publish", "worldforge.presets.edit", "/wf preset publish <borrador>", 1, this::presetPublish);
        add("preset export", "worldforge.presets.export", "/wf preset export <id>", 1, this::presetExport);
        add("preset import", "worldforge.presets.import", "/wf preset import <archivo.yml>", 1, this::presetImport);

        add("generation validate", "worldforge.generation.validate", "/wf generation validate <preset>", 1, this::presetValidate);
        add("generation preview", "worldforge.generation.validate", "/wf generation preview <mundo> <preset>", 2, this::generationPreview);
        add("generation apply", "worldforge.generation.apply", "/wf generation apply <mundo> <preset>", 2, this::generationApply);

        add("regen plan", "worldforge.regeneration.plan", "/wf regen plan <mundo> <radius|area|all> [x z radio | x1 z1 x2 z2]", 2, this::regenPlan);
        add("regen confirm", "worldforge.regeneration.execute", "/wf regen confirm <trabajo> <código>", 2, this::regenConfirm);
        add("regen cancel", "worldforge.regeneration.cancel", "/wf regen cancel <trabajo>", 1, this::regenCancel);
        add("regen restore", "worldforge.regeneration.plan", "/wf regen restore <trabajo>", 1, this::regenRestore);
        add("regen accept", "worldforge.regeneration.execute", "/wf regen accept <trabajo>", 1, this::regenAccept);
        add("regen list", "worldforge.regeneration.view", "/wf regen list", 0, this::regenList);
        add("regen info", "worldforge.regeneration.view", "/wf regen info <trabajo>", 1, this::regenInfo);

        add("dimension list", "worldforge.dimensions.view", "/wf dimension list", 0, this::dimensionList);
        add("dimension create", "worldforge.dimensions.create", "/wf dimension create <nombre> <tipo> <generador>", 3, this::dimensionCreate);
        add("dimension remove", "worldforge.dimensions.create", "/wf dimension remove <dimensión>", 1, this::dimensionRemove);
        add("terrain list", "worldforge.dimensions.view", "/wf terrain list", 0, this::terrainList);
        add("terrain main", "worldforge.terrain.main", "/wf terrain main <terreno|vanilla>", 1, this::terrainMain);

        add("structure list", "worldforge.templates.view", "/wf structure list", 0, this::structureList);
        add("structure info", "worldforge.templates.view", "/wf structure info <plantilla>", 1, this::structureInfo);
        add("structure save", "worldforge.templates.save", "/wf structure save <nombre> <x1> <y1> <z1> <x2> <y2> <z2> [mundo]", 7, this::structureSave);

        add("protection list", "worldforge.protection.view", "/wf protection list", 0, this::protectionList);
        add("protection add", "worldforge.protection.manage", "/wf protection add <nombre> <mundo> <x1> <z1> <x2> <z2>", 6, this::protectionAdd);
        add("protection remove", "worldforge.protection.manage", "/wf protection remove <zona>", 1, this::protectionRemove);
    }

    private void add(String name, String permission, String usage, int minArgs, Handler h) {
        subs.put(name, new Sub(permission, usage, minArgs, h));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            args = new String[] {"help"};
        }
        String two = args.length >= 2 ? (args[0] + " " + args[1]).toLowerCase(Locale.ROOT) : null;
        String one = args[0].toLowerCase(Locale.ROOT);
        String name = two != null && subs.containsKey(two) ? two : one;
        Sub sub = subs.get(name);
        if (sub == null) {
            msg.send(sender, "unknown-subcommand", "sub", String.join(" ", Arrays.copyOf(args, Math.min(2, args.length))));
            return true;
        }
        if (!sender.hasPermission(sub.permission())) {
            msg.send(sender, "no-permission", "permission", sub.permission());
            return true;
        }
        String[] rest = Arrays.copyOfRange(args, name.split(" ").length, args.length);
        if (rest.length < sub.minArgs()) {
            msg.send(sender, "usage", "usage", sub.usage());
            return true;
        }
        if (!plugin.ready() && !name.equals("diagnostics") && !name.equals("help")) {
            msg.send(sender, "db-unavailable", "reason", plugin.notReadyReason());
            return true;
        }
        try {
            sub.handler().run(sender, rest);
        } catch (RuntimeException e) {
            msg.send(sender, "failed", "reason", e.toString());
            plugin.getLogger().warning("/wf " + String.join(" ", args) + " falló: " + e);
        }
        return true;
    }

    // ── general ─────────────────────────────────────────

    private void help(CommandSender s, String[] a) {
        msg.sendPlain(s, Component.text("Comandos disponibles para ti:", NamedTextColor.AQUA));
        subs.forEach((name, sub) -> {
            if (s.hasPermission(sub.permission())) {
                s.sendMessage(Component.text("  " + sub.usage(), NamedTextColor.WHITE));
            }
        });
    }

    private void gui(CommandSender s, String[] a) {
        if (!(s instanceof org.bukkit.entity.Player p)) {
            msg.send(s, "failed", "reason", "el menú es para jugadores; desde la consola usa los comandos.");
            return;
        }
        plugin.gui().openMain(p);
    }

    private void status(CommandSender s, String[] a) {
        plugin.worlds().list().whenComplete((views, err) -> {
            if (err != null) {
                msg.send(s, "failed", "reason", err.toString());
                return;
            }
            long managed = views.stream().filter(v -> v.managed()).count();
            msg.sendPlain(s, Component.text(plugin.platform().name() + " · " + views.size() + " mundos (" + managed
                    + " de WorldForge) · " + plugin.profiles().all().stream().filter(ProfileService.Entry::usable).count()
                    + " presets utilizables", NamedTextColor.AQUA));
        });
    }

    private void diagnostics(CommandSender s, String[] a) {
        plugin.diagnostics().report().whenComplete((lines, err) -> {
            if (err != null) {
                msg.send(s, "failed", "reason", err.toString());
                return;
            }
            msg.sendPlain(s, Component.text("Diagnóstico", NamedTextColor.AQUA));
            lines.forEach(l -> s.sendMessage(Component.text("  " + l, l.startsWith("✘") ? NamedTextColor.RED : NamedTextColor.GRAY)));
        });
    }

    private void reload(CommandSender s, String[] a) {
        plugin.platform().async(() -> plugin.reloadEverything()).whenComplete((problems, err) -> {
            if (err != null) {
                msg.send(s, "failed", "reason", err.toString());
                return;
            }
            msg.send(s, "reloaded");
            plugin.gui().invalidate();
            problems.forEach(p -> s.sendMessage(Component.text("  ✘ " + p, NamedTextColor.RED)));
        });
    }

    // ── worlds ──────────────────────────────────────────

    private void worldList(CommandSender s, String[] a) {
        plugin.worlds().list().whenComplete((views, err) -> {
            if (err != null) {
                msg.send(s, "failed", "reason", err.toString());
                return;
            }
            msg.sendPlain(s, Component.text("Mundos", NamedTextColor.AQUA));
            views.forEach(v -> s.sendMessage(Component.text("  " + v.name() + " ", NamedTextColor.WHITE)
                    .append(Component.text("[" + v.key() + "] " + (v.loaded() ? "cargado" : "sin cargar") + " · " + v.status()
                            + (v.profileId() == null ? "" : " · preset " + v.profileId()), NamedTextColor.GRAY))));
        });
    }

    private void worldInspect(CommandSender s, String[] a) {
        report(s, plugin.worlds().inspect(a[0]), lines -> {
            msg.sendPlain(s, Component.text("Mundo " + a[0], NamedTextColor.AQUA));
            lines.forEach(l -> s.sendMessage(Component.text("  " + l, NamedTextColor.GRAY)));
        });
    }

    private void worldCreate(CommandSender s, String[] a) {
        msg.send(s, "working");
        report(s, plugin.worlds().create(a[0], a[1], s.getName()),
                w -> msg.send(s, "world-created", "name", w.getName(), "preset", a[1], "seed", String.valueOf(w.getSeed())));
    }

    private void worldLoad(CommandSender s, String[] a) {
        report(s, plugin.worlds().load(a[0], s.getName()), w -> msg.send(s, "world-loaded", "name", w.getName()));
    }

    private void worldUnload(CommandSender s, String[] a) {
        report(s, plugin.worlds().unload(a[0], s.getName()), v -> msg.send(s, "world-unloaded", "name", a[0]));
    }

    private void worldImport(CommandSender s, String[] a) {
        report(s, plugin.worlds().importWorld(a[0], a[1], s.getName()), w -> msg.sendPlain(s, Component.text("Mundo " + a[0]
                + " importado con el preset " + a[1] + ": lo que ya existe no cambia; los chunks nuevos usan el preset.", NamedTextColor.GREEN)));
    }

    private void worldTp(CommandSender s, String[] a) {
        World w = Bukkit.getWorld(a[0]);
        if (w == null) {
            msg.send(s, "failed", "reason", "no hay ningún mundo cargado llamado «" + a[0] + "».");
            return;
        }
        org.bukkit.entity.Player target = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : s instanceof org.bukkit.entity.Player p ? p : null;
        if (target == null) {
            msg.send(s, "failed", "reason", a.length > 1 ? "«" + a[1] + "» no está conectado." : "desde la consola indica el jugador.");
            return;
        }
        if (target != s && !s.hasPermission("worldforge.worlds.teleport.others")) {
            msg.send(s, "no-permission", "permission", "worldforge.worlds.teleport.others");
            return;
        }
        target.teleportAsync(w.getSpawnLocation()).whenComplete((ok, err) -> {
            if (Boolean.TRUE.equals(ok)) {
                msg.sendPlain(s, Component.text(target.getName() + " → " + w.getName() + ".", NamedTextColor.GREEN));
            } else {
                msg.send(s, "failed", "reason", "no se pudo teletransportar" + (err == null ? "." : ": " + err));
            }
        });
    }

    // ── presets ─────────────────────────────────────────

    private void presetList(CommandSender s, String[] a) {
        msg.sendPlain(s, Component.text("Presets", NamedTextColor.AQUA));
        for (ProfileService.Entry e : plugin.profiles().all()) {
            long errors = e.problems().stream().filter(Problem::isError).count();
            String state = e.draft() ? "borrador" : errors > 0 ? errors + " errores" : "listo";
            s.sendMessage(Component.text("  " + e.profile().id(), e.usable() ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                    .append(Component.text(" — " + e.profile().name() + " · " + e.profile().provider() + " · " + state, NamedTextColor.GRAY)));
        }
    }

    private Optional<ProfileService.Entry> find(String id) {
        return plugin.profiles().preset(id).or(() -> plugin.profiles().draft(id));
    }

    private void presetInfo(CommandSender s, String[] a) {
        Optional<ProfileService.Entry> e = find(a[0]);
        if (e.isEmpty()) {
            msg.send(s, "failed", "reason", "no existe «" + a[0] + "».");
            return;
        }
        Profile p = e.get().profile();
        GenerationProvider provider = plugin.providers().get(p.provider()).orElse(null);
        msg.sendPlain(s, Component.text(p.name() + " (" + p.id() + ")" + (e.get().draft() ? " — borrador" : ""), NamedTextColor.AQUA));
        s.sendMessage(Component.text("  " + p.description(), NamedTextColor.GRAY));
        s.sendMessage(Component.text("  Proveedor " + p.provider() + (provider == null ? " (no existe)" : ": " + provider.description()), NamedTextColor.GRAY));
        s.sendMessage(Component.text("  Semilla " + (p.seed() == null ? "aleatoria" : p.seed()) + " · entorno " + p.environment()
                + " · versión " + e.get().hash(), NamedTextColor.GRAY));
        if (!p.ores().isEmpty()) {
            s.sendMessage(Component.text("  Menas: " + String.join(", ", p.ores().stream().map(o -> o.id() + (o.enabled() ? "" : " (off)")).toList()), NamedTextColor.GRAY));
        }
        msg.problems(s, e.get().problems());
    }

    private void presetValidate(CommandSender s, String[] a) {
        Optional<ProfileService.Entry> e = find(a[0]);
        if (e.isEmpty()) {
            msg.send(s, "failed", "reason", "no existe «" + a[0] + "».");
            return;
        }
        List<Problem> problems = plugin.validator().validate(e.get().profile());
        if (problems.stream().noneMatch(Problem::isError)) {
            msg.send(s, "preset-valid", "id", a[0], "provider", e.get().profile().provider());
        } else {
            msg.send(s, "preset-invalid", "id", a[0]);
        }
        msg.problems(s, problems);
    }

    private void presetCreate(CommandSender s, String[] a) {
        String base = a.length > 1 ? a[1] : "custom";
        async(s, () -> plugin.profiles().duplicate(base, a[0], a[0]),
                e -> msg.send(s, "preset-duplicated", "id", a[0], "source", base));
    }

    private void presetDuplicate(CommandSender s, String[] a) {
        async(s, () -> plugin.profiles().duplicate(a[0], a[1], a[1]),
                e -> msg.send(s, "preset-duplicated", "id", a[1], "source", a[0]));
    }

    private void presetSeed(CommandSender s, String[] a) {
        Optional<ProfileService.Entry> d = plugin.profiles().draft(a[0]);
        if (d.isEmpty()) {
            msg.send(s, "failed", "reason", "la semilla solo se cambia en un borrador (duplica el preset primero).");
            return;
        }
        Long seed;
        if (a[1].equalsIgnoreCase("ninguna") || a[1].equalsIgnoreCase("none")) {
            seed = null;
        } else {
            try {
                seed = Long.parseLong(a[1]);
            } catch (NumberFormatException e) {
                msg.send(s, "failed", "reason", "«" + a[1] + "» no es un número entero.");
                return;
            }
        }
        String expected = d.get().hash();
        async(s, () -> plugin.profiles().saveDraft(d.get().profile().withSeed(seed), expected), e -> msg.send(s, "done"));
    }

    private void presetPublish(CommandSender s, String[] a) {
        String current = plugin.profiles().preset(a[0]).map(ProfileService.Entry::hash).orElse(null);
        async(s, () -> plugin.profiles().publish(a[0], current), e -> {
            msg.send(s, "preset-published", "id", a[0]);
            plugin.records().saveProfileVersion(a[0], e.hash(), s.getName());
            plugin.records().history(s.getName(), "preset.publish", a[0], e.hash());
        });
    }

    private void presetExport(CommandSender s, String[] a) {
        async(s, () -> plugin.profiles().export(a[0]),
                f -> msg.send(s, "preset-exported", "file", plugin.getDataFolder().toPath().relativize(f).toString()));
    }

    private void presetImport(CommandSender s, String[] a) {
        async(s, () -> plugin.profiles().importFile(a[0]), e -> {
            msg.send(s, "preset-imported", "id", e.profile().id());
            plugin.records().history(s.getName(), "preset.import", e.profile().id(), a[0]);
        });
    }

    // ── generation ──────────────────────────────────────

    private void generationPreview(CommandSender s, String[] a) {
        Optional<ProfileService.Entry> e = plugin.profiles().preset(a[1]);
        if (e.isEmpty()) {
            msg.send(s, "failed", "reason", "no hay un preset publicado «" + a[1] + "».");
            return;
        }
        plugin.records().world(a[0]).thenCompose(rec -> plugin.platform().global(() -> {
            List<String> lines = new ArrayList<>();
            World w = Bukkit.getWorld(a[0]);
            Profile p = e.get().profile();
            lines.add("Preset " + p.id() + " · proveedor " + p.provider());
            lines.add("Los chunks ya generados NO cambian; para eso está la regeneración.");
            if (rec.isPresent()) {
                lines.add(plugin.platform().folia()
                        ? "Mundo administrado: en Folia se aplica al reiniciar (no se recarga en caliente)."
                        : "Mundo administrado: se descarga y se vuelve a cargar; los chunks nuevos usarán el preset.");
            } else if (w != null) {
                lines.add("Mundo del servidor: necesita bukkit.yml → worlds: " + a[0] + ": generator: RPGRoll-WorldForge:" + p.id() + " y REINICIAR.");
            } else {
                lines.add("No existe un mundo «" + a[0] + "».");
            }
            if (!e.get().usable()) {
                lines.add("✘ el preset tiene errores: no se puede aplicar.");
            }
            return lines;
        })).whenComplete((lines, err) -> {
            if (err != null) {
                msg.send(s, "failed", "reason", err.toString());
                return;
            }
            msg.sendPlain(s, Component.text("Vista previa", NamedTextColor.AQUA));
            lines.forEach(l -> s.sendMessage(Component.text("  " + l, l.startsWith("✘") ? NamedTextColor.RED : NamedTextColor.GRAY)));
        });
    }

    private void generationApply(CommandSender s, String[] a) {
        report(s, plugin.worlds().applyPreset(a[0], a[1], s.getName()),
                text -> msg.sendPlain(s, Component.text(text, NamedTextColor.GREEN)));
    }

    // ── regeneration ────────────────────────────────────

    private void regenPlan(CommandSender s, String[] a) {
        Regeneration.Scope scope;
        try {
            scope = switch (a[1].toLowerCase(Locale.ROOT)) {
                case "radius" -> a.length == 5 ? Regeneration.Scope.radius(num(a[2]), num(a[3]), range(num(a[4]), 0, 64, "radio")) : null;
                case "area" -> a.length == 6 ? Regeneration.Scope.area(num(a[2]), num(a[3]), num(a[4]), num(a[5])) : null;
                case "all" -> a.length == 2 ? Regeneration.Scope.all() : null;
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            msg.send(s, "failed", "reason", e.getMessage());
            return;
        }
        if (scope == null) {
            msg.send(s, "usage", "usage", "/wf regen plan <mundo> radius <x> <z> <radio> · area <x1> <z1> <x2> <z2> · all");
            return;
        }
        msg.send(s, "working");
        report(s, plugin.regeneration().plan(a[0], scope, s.getName()), p -> showPlan(s, p));
    }

    private void regenRestore(CommandSender s, String[] a) {
        report(s, plugin.regeneration().planRestore(a[0], s.getName()), p -> showPlan(s, p));
    }

    private void showPlan(CommandSender s, Regeneration.Preview p) {
        Jobs.Job j = p.job();
        boolean restore = j.type().equals(Regeneration.RESTORE);
        msg.sendPlain(s, Component.text((restore ? "Plan de restauración " : "Plan de regeneración ") + j.id() + " · mundo " + j.world(), NamedTextColor.AQUA));
        gray(s, j.message());
        if (restore) {
            gray(s, "Archivos a devolver: " + j.total() + " (" + Regeneration.mb(p.backupBytes()) + ")");
            warn(s, "Cada archivo de región (32×32 chunks) vuelve ENTERO al momento del respaldo: también se pierde lo hecho después en esas regiones.");
        } else {
            gray(s, "Chunks a regenerar: " + j.total() + " · protegidos (no se tocan): " + p.protectedChunks()
                    + " · nunca generados (se ignoran): " + p.neverGenerated());
            warn(s, "Se pierde TODO lo construido en esos chunks: se generan de nuevo con el generador actual del mundo.");
            if (p.neverGenerated() > 0 && plugin.platform().folia()) {
                warn(s, Regeneration.FOLIA_UNSAVED);
            }
        }
        gray(s, "Respaldo previo: " + Regeneration.mb(p.backupBytes()) + " en plugins/RPGRoll-WorldForge/backups (WorldForge nunca lo borra).");
        gray(s, p.restart()
                ? "Cuándo: en el PRÓXIMO ARRANQUE, antes de que el servidor abra los mundos."
                : "Cuándo: al confirmar. El mundo se descarga unos segundos; quien esté dentro va a la aparición principal.");
        s.sendMessage(Component.text("  Para confirmar: /wf regen confirm " + j.id() + " " + j.token()
                + "  (caduca en " + plugin.getConfig().getInt("regeneration.confirm-minutes", 15) + " min)", NamedTextColor.GREEN));
    }

    private void regenConfirm(CommandSender s, String[] a) {
        report(s, plugin.regeneration().confirm(a[0], a[1], s.getName(),
                        text -> s.sendMessage(Component.text(text, text.contains("✘") ? NamedTextColor.RED : NamedTextColor.GREEN))),
                text -> msg.sendPlain(s, Component.text(text, NamedTextColor.GREEN)));
    }

    private void regenCancel(CommandSender s, String[] a) {
        report(s, plugin.regeneration().cancel(a[0], s.getName()), text -> msg.sendPlain(s, Component.text(text, NamedTextColor.GREEN)));
    }

    private void regenAccept(CommandSender s, String[] a) {
        report(s, plugin.regeneration().accept(a[0], s.getName()), text -> msg.sendPlain(s, Component.text(text, NamedTextColor.GREEN)));
    }

    private void regenList(CommandSender s, String[] a) {
        plugin.regeneration().jobs().recent(10).whenComplete((list, err) -> {
            if (err != null) {
                msg.send(s, "failed", "reason", err.toString());
                return;
            }
            msg.sendPlain(s, Component.text("Trabajos (últimos 10)", NamedTextColor.AQUA));
            if (list.isEmpty()) {
                gray(s, "ninguno");
            }
            list.forEach(j -> gray(s, j.id() + " " + j.type() + " " + j.world() + " · " + j.state() + " · " + j.done() + "/" + j.total()));
        });
    }

    private void regenInfo(CommandSender s, String[] a) {
        plugin.regeneration().jobs().get(a[0]).whenComplete((found, err) -> {
            if (err != null || found.isEmpty()) {
                msg.send(s, "failed", "reason", err != null ? err.toString() : "no hay un trabajo «" + a[0] + "».");
                return;
            }
            Jobs.Job j = found.get();
            msg.sendPlain(s, Component.text("Trabajo " + j.id() + " (" + j.type() + ") · " + j.state(), NamedTextColor.AQUA));
            gray(s, "Mundo " + j.world() + " · carpeta " + j.dimensionDir());
            gray(s, "Hecho " + j.done() + "/" + j.total() + (j.failed() > 0 ? " · omitidos " + j.failed() : ""));
            gray(s, "Respaldo: " + (j.backupId() == null ? "todavía ninguno" : j.backupId()));
            gray(s, "Creado por " + j.createdBy() + " · " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(j.createdAt())));
            if (j.message() != null) {
                gray(s, j.message());
            }
            if (j.state() == JobState.PLANNED) {
                s.sendMessage(Component.text("  Para confirmar: /wf regen confirm " + j.id() + " " + j.token(), NamedTextColor.GREEN));
            }
        });
    }

    // ── dimensions ──────────────────────────────────────

    private void dimensionList(CommandSender s, String[] a) {
        plugin.dimensions().list().whenComplete((list, err) -> {
            if (err != null) {
                msg.send(s, "failed", "reason", err.toString());
                return;
            }
            msg.sendPlain(s, Component.text("Dimensiones de WorldForge (datapack, se cargan al arrancar)", NamedTextColor.AQUA));
            if (list.isEmpty()) {
                gray(s, "ninguna; crea una con /wf dimension create <nombre> <tipo> <generador>");
            }
            list.forEach(d -> s.sendMessage(Component.text("  " + d.id() + " · " + d.type() + " · " + d.generator() + " · " + d.state()
                    + (d.loaded() ? " · cargada como " + d.worldName() : "") + (d.message() == null ? "" : " — " + d.message()),
                    d.state().equals(Dimensions.ERROR) ? NamedTextColor.RED : NamedTextColor.GRAY)));
            gray(s, "Tipos: " + String.join(", ", Dimensions.TYPES.keySet()) + " · generadores: " + String.join(", ", Dimensions.GENERATORS.keySet())
                    + ", " + Dimensions.TERRAIN + "<terreno> (/wf terrain list)");
        });
    }

    private void dimensionCreate(CommandSender s, String[] a) {
        report(s, plugin.dimensions().create(a[0], a[1], a[2], s.getName()), d -> {
            msg.sendPlain(s, Component.text("Dimensión " + d.id() + " escrita en el datapack (" + Dimensions.TYPES.get(d.type()).description()
                    + "; " + plugin.dimensions().describe(d.generator()) + ").", NamedTextColor.GREEN));
            warn(s, "Minecraft solo lee dimensiones nuevas al ARRANCAR: reinicia y aparecerá como el mundo " + d.worldName()
                    + " (entra con /execute in " + Dimensions.NAMESPACE + ":" + d.id() + " run tp @s 0 100 0).");
        });
    }

    private void dimensionRemove(CommandSender s, String[] a) {
        report(s, plugin.dimensions().remove(a[0], s.getName()), d -> msg.sendPlain(s, Component.text("Dimensión " + d.id() + ": "
                + d.state() + " — " + d.message(), NamedTextColor.YELLOW)));
    }

    // ── terrains ────────────────────────────────────────

    private void terrainList(CommandSender s, String[] a) {
        String main = plugin.dimensions().mainTerrain().orElse("vanilla");
        msg.sendPlain(s, Component.text("Terrenos (terrains/*.yml) · mundo principal: " + main, NamedTextColor.AQUA));
        for (TerrainSpec t : plugin.terrains().list()) {
            s.sendMessage(Component.text("  " + t.id() + " — " + t.description(), NamedTextColor.WHITE));
            gray(s, "    altura ×" + t.heightScale() + (t.heightOffset() == 0 ? "" : " " + (t.heightOffset() > 0 ? "+" : "") + t.heightOffset())
                    + " · picos ×" + t.peaks() + " · continentes ×" + t.continentSize() + " · biomas ×" + t.biomeSize()
                    + " · cimas hasta y≈" + t.estimatedPeak() + (t.estimatedPeak() > t.slideStart(320) ? " (tipo towering)" : ""));
        }
        gray(s, "Úsalos en /wf dimension create <nombre> <tipo> " + Dimensions.TERRAIN + "<terreno>, o en el principal con /wf terrain main.");
    }

    private void terrainMain(CommandSender s, String[] a) {
        report(s, plugin.dimensions().setMain(a[0], s.getName()), notes -> {
            msg.sendPlain(s, Component.text(a[0].equals("vanilla")
                    ? "El mundo principal vuelve al terreno de vanilla en el próximo arranque."
                    : "El mundo principal usará el terreno " + a[0] + " desde el próximo arranque.", NamedTextColor.GREEN));
            warn(s, "Solo cambia en los chunks que aún no existen: donde lo nuevo toque lo ya generado habrá un corte. "
                    + "También lo usan los mundos de proveedor vanilla y las dimensiones con generador overworld.");
            notes.forEach(n -> s.sendMessage(Component.text("  " + n, NamedTextColor.GOLD)));
        });
    }

    // ── templates ───────────────────────────────────────

    private void structureList(CommandSender s, String[] a) {
        async(s, () -> Result.ok(plugin.templates().list()), list -> {
            msg.sendPlain(s, Component.text("Plantillas (structures/)", NamedTextColor.AQUA));
            if (list.isEmpty()) {
                gray(s, "ninguna; guarda una con /wf structure save");
            }
            list.forEach(t -> s.sendMessage(t.problem() == null
                    ? Component.text("  " + t.name() + " · " + t.x() + "×" + t.y() + "×" + t.z() + " · sha " + t.sha256(), NamedTextColor.GRAY)
                    : Component.text("  ✘ " + t.name() + ": " + t.problem(), NamedTextColor.RED)));
        });
    }

    private void structureInfo(CommandSender s, String[] a) {
        async(s, () -> plugin.templates().info(a[0]), t -> {
            msg.sendPlain(s, Component.text("Plantilla " + t.name() + ": " + t.x() + "×" + t.y() + "×" + t.z() + " (ancho×alto×fondo) · sha "
                    + t.sha256(), NamedTextColor.AQUA));
            List<String> users = plugin.profiles().all().stream()
                    .filter(e -> e.profile().templates().stream().anyMatch(r -> r.structure().equals(t.name())))
                    .map(e -> e.profile().id() + (e.draft() ? " (borrador)" : "")).toList();
            gray(s, users.isEmpty() ? "Ningún preset la usa." : "La usan: " + String.join(", ", users));
        });
    }

    private void structureSave(CommandSender s, String[] a) {
        int[] c = new int[6];
        try {
            for (int i = 0; i < 6; i++) {
                c[i] = num(a[1 + i]);
            }
        } catch (IllegalArgumentException e) {
            msg.send(s, "failed", "reason", e.getMessage());
            return;
        }
        World w = a.length > 7 ? Bukkit.getWorld(a[7]) : s instanceof org.bukkit.entity.Player p ? p.getWorld() : null;
        if (w == null) {
            msg.send(s, "failed", "reason", a.length > 7 ? "no hay ningún mundo cargado llamado «" + a[7] + "»." : "desde la consola indica el mundo.");
            return;
        }
        // The blocks are read where they live: the main thread on Paper, the region's thread on Folia.
        report(s, plugin.platform().at(w, Math.min(c[0], c[3]) >> 4, Math.min(c[2], c[5]) >> 4,
                () -> plugin.templates().save(a[0], w, c[0], c[1], c[2], c[3], c[4], c[5])), t -> {
            msg.sendPlain(s, Component.text("Plantilla " + t.name() + " guardada: " + t.x() + "×" + t.y() + "×" + t.z()
                    + " · sha " + t.sha256() + ".", NamedTextColor.GREEN));
            gray(s, "Úsala en un preset (templates: structure: " + t.name() + "); se coloca solo en chunks que se generen después.");
            plugin.records().history(s.getName(), "structure.save", w.getName(), t.name());
        });
    }

    // ── protection ──────────────────────────────────────

    private void protectionList(CommandSender s, String[] a) {
        Protections p = plugin.protections();
        msg.sendPlain(s, Component.text("Zonas protegidas", NamedTextColor.AQUA));
        p.blockedReason().ifPresent(r -> s.sendMessage(Component.text("  ✘ " + r, NamedTextColor.RED)));
        gray(s, "Alrededor de cada aparición: " + p.spawnRadius() + " chunks");
        p.areas().forEach(z -> gray(s, z.id() + " · " + z.world() + " · " + z.minX() + ", " + z.minZ() + " → " + z.maxX() + ", " + z.maxZ()
                + (z.note().isEmpty() ? "" : " · " + z.note())));
    }

    private void protectionAdd(CommandSender s, String[] a) {
        int[] c = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                c[i] = num(a[2 + i]);
            }
        } catch (IllegalArgumentException e) {
            msg.send(s, "failed", "reason", e.getMessage());
            return;
        }
        String note = a.length > 6 ? String.join(" ", Arrays.copyOfRange(a, 6, a.length)) : "";
        async(s, () -> plugin.protections().add(a[0], a[1], c[0], c[1], c[2], c[3], note), z -> {
            msg.sendPlain(s, Component.text("Zona " + z.id() + " protegida en " + z.world() + ": " + z.minX() + ", " + z.minZ()
                    + " → " + z.maxX() + ", " + z.maxZ() + ".", NamedTextColor.GREEN));
            plugin.records().history(s.getName(), "protection.add", z.world(), z.id());
        });
    }

    private void protectionRemove(CommandSender s, String[] a) {
        async(s, () -> plugin.protections().remove(a[0]), z -> {
            msg.sendPlain(s, Component.text("Zona " + z.id() + " ya no está protegida.", NamedTextColor.YELLOW));
            plugin.records().history(s.getName(), "protection.remove", z.world(), z.id());
        });
    }

    // ── helpers ─────────────────────────────────────────

    private static int num(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("«" + raw + "» no es un número entero.");
        }
    }

    private static int range(int v, int min, int max, String what) {
        if (v < min || v > max) {
            throw new IllegalArgumentException("el " + what + " va de " + min + " a " + max + ".");
        }
        return v;
    }

    private static void gray(CommandSender s, String text) {
        s.sendMessage(Component.text("  " + text, NamedTextColor.GRAY));
    }

    private static void warn(CommandSender s, String text) {
        s.sendMessage(Component.text("  ⚠ " + text, NamedTextColor.GOLD));
    }

    private <T> void report(CommandSender s, CompletableFuture<Result<T>> f, java.util.function.Consumer<T> onOk) {
        f.whenComplete((r, err) -> {
            if (err != null) {
                msg.send(s, "failed", "reason", err.getCause() == null ? err.toString() : err.getCause().toString());
                return;
            }
            if (r.isOk()) {
                onOk.accept(r.value());
                msg.problems(s, r.problems());
            } else {
                Problem first = r.errors().getFirst();
                msg.send(s, "failed", "reason", (first.field().isEmpty() ? "" : first.field() + ": ") + first.message());
                msg.problems(s, r.problems().stream().filter(p -> p != first).toList());
            }
        });
    }

    private <T> void async(CommandSender s, java.util.function.Supplier<Result<T>> work, java.util.function.Consumer<T> onOk) {
        report(s, plugin.platform().async(work), onOk);
    }

    // ── tab completion ──────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> names = subs.entrySet().stream().filter(e -> sender.hasPermission(e.getValue().permission()))
                .map(Map.Entry::getKey).toList();
        if (args.length == 1) {
            return filter(names.stream().map(n -> n.split(" ")[0]).distinct(), args[0]);
        }
        if (args.length == 2 && names.stream().anyMatch(n -> n.startsWith(args[0] + " "))) {
            return filter(names.stream().filter(n -> n.startsWith(args[0] + " ")).map(n -> n.split(" ")[1]), args[1]);
        }
        String key = args[0] + " " + args[1];
        if (!subs.containsKey(key) || !sender.hasPermission(subs.get(key).permission())) {
            return List.of();
        }
        int pos = args.length - 3;
        String usage = subs.get(key).usage();
        String[] parts = usage.split(" ");
        String slot = parts.length > 3 + pos ? parts[3 + pos] : "";
        String typed = args[args.length - 1];
        if (slot.contains("mundo")) {
            return filter(Bukkit.getWorlds().stream().map(World::getName), typed);
        }
        if (slot.contains("preset") || slot.contains("id") || slot.contains("origen") || slot.contains("base")) {
            return filter(plugin.profiles().all().stream().filter(e -> !e.draft()).map(e -> e.profile().id()), typed);
        }
        if (slot.contains("borrador")) {
            return filter(plugin.profiles().all().stream().filter(ProfileService.Entry::draft).map(e -> e.profile().id()), typed);
        }
        if (slot.equals("<tipo>")) {
            return filter(Dimensions.TYPES.keySet().stream(), typed);
        }
        if (slot.equals("<generador>")) {
            return filter(Stream.concat(Dimensions.GENERATORS.keySet().stream(),
                    plugin.terrains().list().stream().map(t -> Dimensions.TERRAIN + t.id())), typed);
        }
        if (slot.equals("<terreno|vanilla>")) {
            return filter(Stream.concat(Stream.of("vanilla"), plugin.terrains().list().stream().map(TerrainSpec::id)), typed);
        }
        if (slot.contains("radius")) {
            return filter(Stream.of("radius", "area", "all"), typed);
        }
        if (slot.equals("<plantilla>")) {
            return filter(plugin.templates().names().stream(), typed);
        }
        if (slot.equals("<zona>")) {
            return filter(plugin.protections().areas().stream().map(Protections.Area::id), typed);
        }
        if (slot.contains("archivo")) {
            return filter(plugin.importFiles().stream(), typed);
        }
        return List.of();
    }

    private static List<String> filter(Stream<String> options, String typed) {
        String t = typed.toLowerCase(Locale.ROOT);
        return options.filter(o -> o.toLowerCase(Locale.ROOT).startsWith(t)).sorted().toList();
    }
}
