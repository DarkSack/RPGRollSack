package com.sack.rpgroll.mobs.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;

    public MobAdminCommand(MobManager mobManager, MobEngine engine, MobStatRegistry statRegistry,
            ChatPromptManager chatPromptManager, Plugin plugin) {
        this.mobManager = mobManager;
        this.engine = engine;
        this.statRegistry = statRegistry;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "general.no_permission");
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
        lang.send(sender, "command.usage");
    }

    private void handleCreate(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.create_player_only");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "command.create_usage");
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);

        if (mobManager.exists(id)) {
            lang.send(sender, "command.id_exists");
            return;
        }

        String baseEntityType = args.length >= 3 ? args[2].toUpperCase(Locale.ROOT) : "ZOMBIE";

        com.sack.rpgroll.mobs.core.MobDefinition definition = new com.sack.rpgroll.mobs.core.MobDefinition(id, null,
                id, null, 1, null, null, null, com.sack.rpgroll.mobs.core.MobModel.defaults(baseEntityType), null,
                null, null, null, null, null, null, null, null, null, null, null);

        MobEditorSession session = new MobEditorSession(definition, mobManager, statRegistry, chatPromptManager,
                plugin);

        lang.send(sender, "command.create_success", "id", id);

        new MobEditorHubGUI(player, session).open();
    }

    private void handleSpawn(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "command.spawn_usage");
            return;
        }

        MobDefinition definition = mobManager.get(args[1]).orElse(null);
        if (definition == null) {
            lang.send(sender, "command.not_found", "id", args[1]);
            return;
        }

        Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2])
                : (Senders.asPlayer(sender) instanceof Player player ? player : null);

        if (target == null) {
            lang.send(sender, "command.spawn_need_player");
            return;
        }

        engine.spawnMob(definition, target.getLocation())
                .ifPresentOrElse(
                        entity -> lang.send(sender, "command.spawn_success", "name", definition.displayName()),
                        () -> lang.send(sender, "command.spawn_failed"));
    }

    private void handleList(CommandSender sender, String[] args) {

        String categoryFilter = args.length >= 2 ? args[1].toUpperCase(Locale.ROOT) : null;

        List<MobDefinition> defs = mobManager.getAll().stream()
                .filter(def -> categoryFilter == null || def.category().name().equals(categoryFilter))
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList();

        lang.send(sender, "command.list_header", "count", defs.size());

        for (MobDefinition def : defs) {
            lang.send(sender, "command.list_entry", "id", def.id(), "category", def.category(),
                    "level", def.level());
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "command.info_usage");
            return;
        }

        MobDefinition def = mobManager.get(args[1]).orElse(null);
        if (def == null) {
            lang.send(sender, "command.not_found", "id", args[1]);
            return;
        }

        lang.send(sender, "command.info_header", "name", def.displayName(), "id", def.id());
        lang.send(sender, "command.info_category", "category", def.category(), "level", def.level(),
                "rarity", def.rarityId());
        lang.send(sender, "command.info_stats", "health", def.stat("health"), "damage", def.stat("damage"),
                "defense", def.stat("defense"));
        lang.send(sender, "command.info_counts", "phases", def.phases().size(), "skills", def.skills().size(),
                "loot", def.loot().size());
    }

    private void handleReload(CommandSender sender) {
        mobManager.reload();
        lang.reload(plugin.getConfig().getString("language", "es"));
        lang.send(sender, "command.reload_success", "count", mobManager.count());
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

        lang.send(sender, "command.killall_result", "count", killed);
    }

    private void handleBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.browser_player_only");
            return;
        }

        new MobBrowserGUI(player, mobManager, statRegistry, chatPromptManager, plugin).open();
    }

    private void handleEditor(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.editor_player_only");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "command.editor_usage");
            return;
        }

        MobDefinition definition = mobManager.get(args[1]).orElse(null);
        if (definition == null) {
            lang.send(sender, "command.not_found", "id", args[1]);
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
