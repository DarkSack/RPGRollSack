package com.sack.rpgroll.mobs.command;

import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.engine.ActiveMobState;
import com.sack.rpgroll.mobs.engine.MobEngine;
import com.sack.rpgroll.mobs.gui.ChatPromptManager;
import com.sack.rpgroll.mobs.gui.MobBrowserGUI;
import com.sack.rpgroll.mobs.gui.editor.MobEditorHubGUI;
import com.sack.rpgroll.mobs.gui.editor.MobEditorSession;
import com.sack.rpgroll.mobs.core.MobCategory;
import com.sack.rpgroll.mobs.registry.MobStatRegistry;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /mobadmin spawn &lt;id&gt; [jugador]
 * /mobadmin list [categoría]
 * /mobadmin info &lt;id&gt;
 * /mobadmin reload
 * /mobadmin killall [id]
 * /mobadmin browser        — abre el navegador de mobs (GUI)
 * /mobadmin editor &lt;id&gt;  — abre el editor directo de un mob (GUI)
 */
public class MobAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("spawn", "list", "info", "reload", "killall", "create",
            "browser", "editor");

    private static final String PERMISSION = "rpgrollmobs.admin.*";

    private final MobManager mobManager;
    private final MobEngine engine;
    private final MobStatRegistry statRegistry;
    private final ChatPromptManager chatPromptManager;
    private final Plugin plugin;

    public MobAdminCommand(MobManager mobManager, MobEngine engine, MobStatRegistry statRegistry,
            ChatPromptManager chatPromptManager, Plugin plugin) {
        this.mobManager = mobManager;
        this.engine = engine;
        this.statRegistry = statRegistry;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "spawn" -> handleSpawn(sender, args);
            case "list" -> handleList(sender, args);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "killall" -> handleKillAll(sender, args);
            case "create" -> handleCreate(sender, args);
            case "browser" -> handleBrowser(sender);
            case "editor" -> handleEditor(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uso: /mobadmin <spawn|list|info|reload|killall|create|browser|editor>",
                NamedTextColor.YELLOW));
    }

    private void handleCreate(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede crear un mob.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /mobadmin create <id> [tipo-entidad-base]", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);

        if (mobManager.exists(id)) {
            sender.sendMessage(Component.text("Ya existe un mob con ese id.", NamedTextColor.RED));
            return;
        }

        String baseEntityType = args.length >= 3 ? args[2].toUpperCase(Locale.ROOT) : "ZOMBIE";

        com.sack.rpgroll.mobs.core.MobDefinition definition = new com.sack.rpgroll.mobs.core.MobDefinition(id, null,
                id, null, 1, null, null, null, com.sack.rpgroll.mobs.core.MobModel.defaults(baseEntityType), null,
                null, null, null, null, null, null, null, null, null, null, null);

        MobEditorSession session = new MobEditorSession(definition, mobManager, statRegistry, chatPromptManager,
                plugin);

        sender.sendMessage(Component.text("✔ Mob '" + id + "' creado — configuralo en el editor.",
                NamedTextColor.GREEN));

        new MobEditorHubGUI(player, session).open();
    }

    private void handleSpawn(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /mobadmin spawn <id> [jugador]", NamedTextColor.YELLOW));
            return;
        }

        MobDefinition definition = mobManager.get(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("No existe el mob '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2])
                : (sender instanceof Player player ? player : null);

        if (target == null) {
            sender.sendMessage(Component.text("Especificá un jugador de referencia (consola no tiene ubicación).",
                    NamedTextColor.RED));
            return;
        }

        engine.spawnMob(definition, target.getLocation())
                .ifPresentOrElse(
                        entity -> sender.sendMessage(Component.text("Mob '", NamedTextColor.GREEN)
                                .append(com.sack.rpgroll.util.ComponentUtils.parse(definition.displayName()))
                                .append(Component.text("' invocado.", NamedTextColor.GREEN))),
                        () -> sender.sendMessage(Component.text(
                                "No se pudo invocar (base-entity-type inválido).", NamedTextColor.RED)));
    }

    private void handleList(CommandSender sender, String[] args) {

        String categoryFilter = args.length >= 2 ? args[1].toUpperCase(Locale.ROOT) : null;

        List<MobDefinition> defs = mobManager.getAll().stream()
                .filter(def -> categoryFilter == null || def.category().name().equals(categoryFilter))
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList();

        sender.sendMessage(Component.text("=== Mobs (" + defs.size() + ") ===", NamedTextColor.GOLD));

        for (MobDefinition def : defs) {
            sender.sendMessage(Component.text(" - " + def.id() + " (" + def.category() + ") Lv." + def.level(),
                    NamedTextColor.GRAY));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /mobadmin info <id>", NamedTextColor.YELLOW));
            return;
        }

        MobDefinition def = mobManager.get(args[1]).orElse(null);
        if (def == null) {
            sender.sendMessage(Component.text("No existe el mob '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== ", NamedTextColor.GOLD)
                .append(com.sack.rpgroll.util.ComponentUtils.parse(def.displayName()))
                .append(Component.text(" (" + def.id() + ") ===", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("Categoría: " + def.category() + " | Nivel: " + def.level()
                + " | Rareza: " + def.rarityId(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Vida: " + def.stat("health") + " | Daño: " + def.stat("damage")
                + " | Defensa: " + def.stat("defense"), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Fases: " + def.phases().size() + " | Skills: " + def.skills().size()
                + " | Loot: " + def.loot().size(), NamedTextColor.GRAY));
    }

    private void handleReload(CommandSender sender) {
        mobManager.reload();
        sender.sendMessage(Component.text(
                "Mobs recargados: " + mobManager.count() + " definición(es).", NamedTextColor.GREEN));
    }

    private void handleKillAll(CommandSender sender, String[] args) {

        String idFilter = args.length >= 2 ? args[1] : null;
        int killed = 0;

        for (UUID entityId : List.copyOf(engine.getActiveMobs().keySet())) {

            Entity entity = Bukkit.getEntity(entityId);
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            ActiveMobState state = engine.getState(entityId).orElse(null);
            if (state == null) {
                continue;
            }

            if (idFilter != null && !state.definitionId().equalsIgnoreCase(idFilter)) {
                continue;
            }

            living.setHealth(0);
            killed++;
        }

        sender.sendMessage(Component.text(killed + " mob(s) eliminado(s).", NamedTextColor.GREEN));
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede abrir el navegador.", NamedTextColor.RED));
            return;
        }

        new MobBrowserGUI(player, mobManager, statRegistry, chatPromptManager, plugin).open();
    }

    private void handleEditor(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede abrir el editor.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /mobadmin editor <id>", NamedTextColor.YELLOW));
            return;
        }

        MobDefinition definition = mobManager.get(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("No existe el mob '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        MobEditorSession session = new MobEditorSession(definition, mobManager, statRegistry, chatPromptManager,
                plugin);
        new MobEditorHubGUI(player, session).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            return switch (sub) {
                case "spawn", "info", "editor", "killall" -> TabCompleteUtil.filter(args[1], mobIds());
                case "list" -> TabCompleteUtil.filter(args[1],
                        java.util.Arrays.stream(MobCategory.values()).map(Enum::name).toList());
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (sub) {
                case "spawn" -> TabCompleteUtil.onlinePlayerNames(args[2]);
                case "create" -> TabCompleteUtil.spawnableEntityTypes(args[2]);
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> mobIds() {
        return mobManager.getAll().stream().map(MobDefinition::id).toList();
    }

}
