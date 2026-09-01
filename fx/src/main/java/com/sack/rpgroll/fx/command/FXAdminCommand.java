package com.sack.rpgroll.fx.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fx.RPGRollFXPlugin;
import com.sack.rpgroll.fx.core.EffectDefinition;
import com.sack.rpgroll.fx.core.EffectManager;
import com.sack.rpgroll.fx.engine.EffectContext;
import com.sack.rpgroll.fx.engine.EffectEngine;
import com.sack.rpgroll.fx.gui.ChatPromptManager;
import com.sack.rpgroll.fx.gui.EffectBrowserGUI;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * /rpgfx browser
 * /rpgfx reload
 * /rpgfx test <id> [jugador]
 */
public class FXAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "test");

    private final RPGRollFXPlugin plugin;
    private final EffectManager effectManager;
    private final EffectEngine engine;
    private final ChatPromptManager chatPromptManager;
    private final LangManager langManager;

    public FXAdminCommand(RPGRollFXPlugin plugin, EffectManager effectManager, EffectEngine engine,
            ChatPromptManager chatPromptManager, LangManager langManager) {
        this.plugin = plugin;
        this.effectManager = effectManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
        this.langManager = langManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollfx.admin.*")) {
            langManager.send(sender, "general.no_permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "browser" -> handleBrowser(sender);
            case "reload" -> handleReload(sender);
            case "test" -> handleTest(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        langManager.send(sender, "command.usage");
    }

    private void handleBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            langManager.send(sender, "command.browser_players_only");
            return;
        }

        new EffectBrowserGUI(player, effectManager, engine, chatPromptManager, langManager).open();
    }

    private void handleReload(CommandSender sender) {
        effectManager.reload();

        plugin.reloadConfig();
        langManager.reload(plugin.getConfig().getString("language", "es"));

        langManager.send(sender, "command.reload_success", "count", effectManager.count());
    }

    private void handleTest(CommandSender sender, String[] args) {

        if (args.length < 2) {
            langManager.send(sender, "command.test_usage");
            return;
        }

        String effectId = args[1];
        Optional<EffectDefinition> effectOpt = effectManager.get(effectId);

        if (effectOpt.isEmpty()) {
            langManager.send(sender, "command.test_effect_not_found", "id", effectId);
            return;
        }

        Player target;

        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                langManager.send(sender, "command.test_player_not_found", "player", args[2]);
                return;
            }
        } else if (Senders.asPlayer(sender) instanceof Player senderPlayer) {
            target = senderPlayer;
        } else {
            langManager.send(sender, "command.test_specify_player");
            return;
        }

        engine.play(effectOpt.get(), EffectContext.of(target));
        langManager.send(sender, "command.test_playing", "id", effectId, "player", target.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && "test".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[1],
                    effectManager.getAll().stream().map(EffectDefinition::id).toList());
        }

        if (args.length == 3 && "test".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.onlinePlayerNames(args[2]);
        }

        return List.of();
    }

}
