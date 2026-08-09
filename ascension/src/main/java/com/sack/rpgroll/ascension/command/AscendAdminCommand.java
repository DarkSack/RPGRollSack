package com.sack.rpgroll.ascension.command;

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
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

    public AscendAdminCommand(AscensionEngine engine, AchievementManager achievementManager,
            TitleManager titleManager, AffinityManager affinityManager, JobEvolutionManager jobEvolutionManager,
            SecretUnlockManager secretUnlockManager, FactionManager factionManager,
            ChatPromptManager chatPromptManager) {
        this.engine = engine;
        this.achievementManager = achievementManager;
        this.titleManager = titleManager;
        this.affinityManager = affinityManager;
        this.jobEvolutionManager = jobEvolutionManager;
        this.secretUnlockManager = secretUnlockManager;
        this.factionManager = factionManager;
        this.chatPromptManager = chatPromptManager;
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
        sender.sendMessage(Component.text(
                "Uso: /ascendadmin <achievement|title|reputation|browser|reload> [args]", NamedTextColor.RED));
    }

    private void handleBrowser(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar el editor visual.", NamedTextColor.RED));
            return;
        }

        String type = args.length > 1 ? args[1].toLowerCase() : "evolution";

        switch (type) {
            case "evolution" -> new RaceEvolutionBrowserGUI(player, engine.getEvolutionManager(), chatPromptManager)
                    .open();
            case "specialization" -> new ClassSpecializationBrowserGUI(player, engine.getSpecializationManager(),
                    chatPromptManager).open();
            case "prestige" -> new PrestigeBrowserGUI(player, engine.getPrestigeManager(), chatPromptManager).open();
            case "affinity" -> new AffinityBrowserGUI(player, affinityManager, chatPromptManager).open();
            case "jobevolution" -> new JobEvolutionBrowserGUI(player, jobEvolutionManager, chatPromptManager).open();
            case "secret" -> new SecretUnlockBrowserGUI(player, secretUnlockManager, chatPromptManager).open();
            case "faction" -> new FactionBrowserGUI(player, factionManager, chatPromptManager).open();
            case "achievement" -> new AchievementBrowserGUI(player, achievementManager, chatPromptManager).open();
            case "title" -> new TitleBrowserGUI(player, titleManager, chatPromptManager).open();
            case "legacy" -> new LegacyBrowserGUI(player, engine.getLegacyManager(), chatPromptManager).open();
            default -> player.sendMessage(Component.text(
                    "Opción inválida. Usa: evolution, specialization, prestige, affinity, jobevolution, secret, "
                            + "faction, achievement, title o legacy",
                    NamedTextColor.RED));
        }
    }

    private void handleAchievement(CommandSender sender, String[] args) {

        if (args.length < 4 || !args[1].equalsIgnoreCase("grant")) {
            sender.sendMessage(Component.text("Uso: /ascendadmin achievement grant <jugador> <id>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[2], NamedTextColor.RED));
            return;
        }

        if (achievementManager.get(args[3]).isEmpty()) {
            sender.sendMessage(Component.text("No existe un logro con id: " + args[3], NamedTextColor.RED));
            return;
        }

        AscensionPlayerState state = engine.getStateManager().getOrLoad(target);

        if (state.unlockAchievement(args[3])) {
            sender.sendMessage(Component.text("✔ Logro '" + args[3] + "' otorgado a " + target.getName(),
                    NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text(target.getName() + " ya tiene ese logro.", NamedTextColor.RED));
        }
    }

    private void handleTitle(CommandSender sender, String[] args) {

        if (args.length < 4 || !args[1].equalsIgnoreCase("grant")) {
            sender.sendMessage(Component.text("Uso: /ascendadmin title grant <jugador> <id>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[2], NamedTextColor.RED));
            return;
        }

        if (titleManager.get(args[3]).isEmpty()) {
            sender.sendMessage(Component.text("No existe un título con id: " + args[3], NamedTextColor.RED));
            return;
        }

        AscensionPlayerState state = engine.getStateManager().getOrLoad(target);

        if (state.unlockTitle(args[3])) {
            sender.sendMessage(Component.text("✔ Título '" + args[3] + "' otorgado a " + target.getName(),
                    NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text(target.getName() + " ya tiene ese título.", NamedTextColor.RED));
        }
    }

    private void handleReputation(CommandSender sender, String[] args) {

        if (args.length < 5 || !args[1].equalsIgnoreCase("add")) {
            sender.sendMessage(Component.text(
                    "Uso: /ascendadmin reputation add <jugador> <facción> <cantidad>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[2], NamedTextColor.RED));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Cantidad inválida: " + args[4], NamedTextColor.RED));
            return;
        }

        engine.getStateManager().getOrLoad(target).addReputation(args[3], amount);
        sender.sendMessage(Component.text(
                "✔ +" + amount + " de reputación con '" + args[3] + "' para " + target.getName(),
                NamedTextColor.GREEN));
    }

    private void handleReload(CommandSender sender) {

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

        sender.sendMessage(Component.text("✔ RPGRoll-Ascension recargado.", NamedTextColor.GREEN));
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
