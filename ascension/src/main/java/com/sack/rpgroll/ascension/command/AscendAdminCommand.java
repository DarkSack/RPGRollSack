package com.sack.rpgroll.ascension.command;

import com.sack.rpgroll.ascension.AscensionPlugin;
import com.sack.rpgroll.ascension.core.AffinityManager;
import com.sack.rpgroll.ascension.deferred.AchievementManager;
import com.sack.rpgroll.ascension.deferred.FactionManager;
import com.sack.rpgroll.ascension.deferred.JobEvolutionManager;
import com.sack.rpgroll.ascension.deferred.SecretUnlockManager;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.ascension.engine.AscensionEngine;
import com.sack.rpgroll.ascension.gui.AchievementBrowserGUI;
import com.sack.rpgroll.ascension.gui.AffinityBrowserGUI;
import com.sack.rpgroll.ascension.gui.ChatPromptManager;
import com.sack.rpgroll.ascension.gui.ClassSpecializationBrowserGUI;
import com.sack.rpgroll.ascension.gui.FactionBrowserGUI;
import com.sack.rpgroll.ascension.gui.JobEvolutionBrowserGUI;
import com.sack.rpgroll.ascension.gui.LegacyBrowserGUI;
import com.sack.rpgroll.ascension.gui.PrestigeBrowserGUI;
import com.sack.rpgroll.ascension.gui.RaceEvolutionBrowserGUI;
import com.sack.rpgroll.ascension.gui.SecretUnlockBrowserGUI;
import com.sack.rpgroll.ascension.gui.TitleBrowserGUI;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /ascendadmin achievement grant &lt;jugador&gt; &lt;id&gt;
 * /ascendadmin title grant &lt;jugador&gt; &lt;id&gt;
 * /ascendadmin reputation add &lt;jugador&gt; &lt;facción&gt; &lt;cantidad&gt;
 * /ascendadmin browser &lt;evolution|specialization|prestige|affinity|jobevolution|secret|faction|achievement|title|legacy&gt;
 * /ascendadmin reload
 */
public class AscendAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("achievement", "title", "reputation", "browser",
            "reload");
    private static final List<String> BROWSER_CATEGORIES = List.of("evolution", "specialization", "prestige",
            "affinity", "jobevolution", "secret", "faction", "achievement", "title", "legacy");

    private static final String PERMISSION = "rpgrollascension.admin.*";

    private final AscensionEngine engine;
    private final AchievementManager achievementManager;
    private final TitleManager titleManager;
    private final AffinityManager affinityManager;
    private final JobEvolutionManager jobEvolutionManager;
    private final SecretUnlockManager secretUnlockManager;
    private final FactionManager factionManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final AscensionPlugin plugin;

    public AscendAdminCommand(AscensionEngine engine, AchievementManager achievementManager,
            TitleManager titleManager, AffinityManager affinityManager, JobEvolutionManager jobEvolutionManager,
            SecretUnlockManager secretUnlockManager, FactionManager factionManager,
            ChatPromptManager chatPromptManager, LangManager lang, AscensionPlugin plugin) {
        this.engine = engine;
        this.achievementManager = achievementManager;
        this.titleManager = titleManager;
        this.affinityManager = affinityManager;
        this.jobEvolutionManager = jobEvolutionManager;
        this.secretUnlockManager = secretUnlockManager;
        this.factionManager = factionManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "admin.no_permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "achievement" -> handleAchievement(sender, args);
            case "title" -> handleTitle(sender, args);
            case "reputation" -> handleReputation(sender, args);
            case "browser" -> handleBrowser(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "admin.usage");
    }

    private void handleBrowser(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "admin.browser_player_only");
            return;
        }

        String type = args.length > 1 ? args[1].toLowerCase() : "evolution";

        switch (type) {
            case "evolution" -> new RaceEvolutionBrowserGUI(player, engine.getEvolutionManager(), chatPromptManager,
                    lang).open();
            case "specialization" -> new ClassSpecializationBrowserGUI(player, engine.getSpecializationManager(),
                    chatPromptManager, lang).open();
            case "prestige" -> new PrestigeBrowserGUI(player, engine.getPrestigeManager(), chatPromptManager, lang)
                    .open();
            case "affinity" -> new AffinityBrowserGUI(player, affinityManager, chatPromptManager, lang).open();
            case "jobevolution" -> new JobEvolutionBrowserGUI(player, jobEvolutionManager, chatPromptManager, lang)
                    .open();
            case "secret" -> new SecretUnlockBrowserGUI(player, secretUnlockManager, chatPromptManager, lang).open();
            case "faction" -> new FactionBrowserGUI(player, factionManager, chatPromptManager, lang).open();
            case "achievement" -> new AchievementBrowserGUI(player, achievementManager, chatPromptManager, lang)
                    .open();
            case "title" -> new TitleBrowserGUI(player, titleManager, chatPromptManager, lang).open();
            case "legacy" -> new LegacyBrowserGUI(player, engine.getLegacyManager(), chatPromptManager, lang).open();
            default -> lang.send(player, "admin.browser_invalid_option");
        }
    }

    private void handleAchievement(CommandSender sender, String[] args) {

        if (args.length < 4 || !args[1].equalsIgnoreCase("grant")) {
            lang.send(sender, "admin.achievement_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            lang.send(sender, "admin.achievement_player_not_found", "player", args[2]);
            return;
        }

        if (achievementManager.get(args[3]).isEmpty()) {
            lang.send(sender, "admin.achievement_not_found", "id", args[3]);
            return;
        }

        AscensionPlayerState state = engine.getStateManager().getOrLoad(target);

        if (state.unlockAchievement(args[3])) {
            lang.send(sender, "admin.achievement_granted", "id", args[3], "player", target.getName());
        } else {
            lang.send(sender, "admin.achievement_already_has", "player", target.getName());
        }
    }

    private void handleTitle(CommandSender sender, String[] args) {

        if (args.length < 4 || !args[1].equalsIgnoreCase("grant")) {
            lang.send(sender, "admin.title_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            lang.send(sender, "admin.title_player_not_found", "player", args[2]);
            return;
        }

        if (titleManager.get(args[3]).isEmpty()) {
            lang.send(sender, "admin.title_not_found", "id", args[3]);
            return;
        }

        AscensionPlayerState state = engine.getStateManager().getOrLoad(target);

        if (state.unlockTitle(args[3])) {
            lang.send(sender, "admin.title_granted", "id", args[3], "player", target.getName());
        } else {
            lang.send(sender, "admin.title_already_has", "player", target.getName());
        }
    }

    private void handleReputation(CommandSender sender, String[] args) {

        if (args.length < 5 || !args[1].equalsIgnoreCase("add")) {
            lang.send(sender, "admin.reputation_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            lang.send(sender, "admin.reputation_player_not_found", "player", args[2]);
            return;
        }

        if (factionManager.get(args[3]).isEmpty()) {
            lang.send(sender, "admin.reputation_faction_not_found", "id", args[3]);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            lang.send(sender, "admin.reputation_invalid_amount", "amount", args[4]);
            return;
        }

        engine.getStateManager().getOrLoad(target).addReputation(args[3], amount);
        lang.send(sender, "admin.reputation_added", "amount", amount, "faction", args[3], "player",
                target.getName());
    }

    private void handleReload(CommandSender sender) {

        plugin.reloadConfig();
        lang.reload(plugin.getConfig().getString("language", "es"));

        engine.getEvolutionManager().reload();
        engine.getSpecializationManager().reload();
        engine.getPrestigeManager().reload();
        engine.getLegacyManager().reload();
        achievementManager.reload();
        titleManager.reload();
        affinityManager.reload();
        jobEvolutionManager.reload();
        secretUnlockManager.reload();
        factionManager.reload();

        lang.send(sender, "admin.reload_success");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            return switch (sub) {
                case "achievement", "title" -> TabCompleteUtil.filter(args[1], List.of("grant"));
                case "reputation" -> TabCompleteUtil.filter(args[1], List.of("add"));
                case "browser" -> TabCompleteUtil.filter(args[1], BROWSER_CATEGORIES);
                default -> List.of();
            };
        }

        if (args.length == 3 && List.of("achievement", "title", "reputation").contains(sub)) {
            return TabCompleteUtil.onlinePlayerNames(args[2]);
        }

        if (args.length == 4) {
            return switch (sub) {
                case "achievement" -> TabCompleteUtil.filter(args[3],
                        achievementManager.getAll().stream().map(a -> a.id()).toList());
                case "title" -> TabCompleteUtil.filter(args[3],
                        titleManager.getAll().stream().map(t -> t.id()).toList());
                case "reputation" -> TabCompleteUtil.filter(args[3],
                        factionManager.getAll().stream().map(f -> f.id()).toList());
                default -> List.of();
            };
        }

        return List.of();
    }

}
