package com.sack.rpgroll.tab.command;

import com.sack.rpgroll.tab.animation.AnimationManager;
import com.sack.rpgroll.tab.belowname.BelowNameManager;
import com.sack.rpgroll.tab.bossbar.BossBarManager;
import com.sack.rpgroll.tab.context.ContextManager;
import com.sack.rpgroll.tab.core.RefreshCoordinator;
import com.sack.rpgroll.tab.nametag.NametagManager;
import com.sack.rpgroll.tab.profile.PlayerStateManager;
import com.sack.rpgroll.tab.profile.ProfileManager;
import com.sack.rpgroll.tab.profile.TABProfile;
import com.sack.rpgroll.tab.scoreboard.ScoreboardManager;
import com.sack.rpgroll.tab.sorting.SortingManager;
import com.sack.rpgroll.tab.tablist.TablistManager;
import com.sack.rpgroll.tab.teams.TeamsManager;
import com.sack.rpgroll.util.TabCompleteUtil;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /tabadmin reload
 * /tabadmin list
 * /tabadmin profile &lt;jugador&gt; &lt;perfil&gt;
 */
public class TabAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "list", "profile");
    private static final String PERMISSION = "rpgrolltab.admin.*";

    private final ProfileManager profileManager;
    private final ContextManager contextManager;
    private final TablistManager tablistManager;
    private final ScoreboardManager scoreboardManager;
    private final NametagManager nametagManager;
    private final BelowNameManager belowNameManager;
    private final BossBarManager bossBarManager;
    private final SortingManager sortingManager;
    private final TeamsManager teamsManager;
    private final AnimationManager animationManager;
    private final PlayerStateManager playerStateManager;
    private final RefreshCoordinator refreshCoordinator;
    private final LangManager lang;

    public TabAdminCommand(ProfileManager profileManager, ContextManager contextManager,
            TablistManager tablistManager, ScoreboardManager scoreboardManager, NametagManager nametagManager,
            BelowNameManager belowNameManager, BossBarManager bossBarManager, SortingManager sortingManager,
            TeamsManager teamsManager, AnimationManager animationManager, PlayerStateManager playerStateManager,
            RefreshCoordinator refreshCoordinator, LangManager lang) {
        this.profileManager = profileManager;
        this.contextManager = contextManager;
        this.tablistManager = tablistManager;
        this.scoreboardManager = scoreboardManager;
        this.nametagManager = nametagManager;
        this.belowNameManager = belowNameManager;
        this.bossBarManager = bossBarManager;
        this.sortingManager = sortingManager;
        this.teamsManager = teamsManager;
        this.animationManager = animationManager;
        this.playerStateManager = playerStateManager;
        this.refreshCoordinator = refreshCoordinator;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "command.no-permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "profile" -> handleProfile(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "command.usage");
    }

    private void handleReload(CommandSender sender) {

        animationManager.reload();
        contextManager.reload();
        tablistManager.reload();
        sortingManager.reload();
        teamsManager.reload();
        nametagManager.reload();
        belowNameManager.reload();
        scoreboardManager.reload();
        bossBarManager.reload();
        profileManager.reload();

        refreshCoordinator.refreshAll();

        lang.send(sender, "command.reload.success",
                "profiles", profileManager.count(), "contexts", contextManager.count());
    }

    private void handleList(CommandSender sender) {

        if (profileManager.count() == 0) {
            lang.send(sender, "command.list.empty");
            return;
        }

        lang.send(sender, "command.list.header");

        for (TABProfile profile : profileManager.getAll()) {
            lang.send(sender, "command.list.entry", "id", profile.id());
        }
    }

    private void handleProfile(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.profile.usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "command.profile.player-not-found", "player", args[1]);
            return;
        }

        String profileId = args[2];

        if (profileManager.get(profileId).isEmpty()) {
            lang.send(sender, "command.profile.profile-not-found", "profile", profileId);
            return;
        }

        playerStateManager.forceProfile(target, profileManager.get(profileId).orElseThrow());
        refreshCoordinator.applyActiveState(target);

        lang.send(sender, "command.profile.success", "player", target.getName(), "profile", profileId);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && "profile".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }

        if (args.length == 3 && "profile".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[2], profileManager.getAll().stream().map(TABProfile::id).toList());
        }

        return List.of();
    }

}
