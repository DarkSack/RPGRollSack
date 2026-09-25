package com.sack.rpgroll.pass.command;

import com.sack.rpgroll.pass.PassModule;
import com.sack.rpgroll.pass.player.PassPlayer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /passadmin reload | xp &lt;jugador&gt; &lt;cantidad&gt; | vote &lt;jugador&gt; | info &lt;jugador&gt;
 * <p>
 * {@code vote} simula un voto: sirve para probar las recompensas sin pasar
 * por una página de votación.
 */
public class PassAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "xp", "vote", "info");

    private final PassModule module;
    private final Runnable reload;

    public PassAdminCommand(PassModule module, Runnable reload) {
        this.module = module;
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            reload.run();
            module.lang().send(sender, "admin.reloaded", "missions", module.missions().count());
            return true;
        }

        if (!SUBCOMMANDS.contains(sub) || args.length < 2) {
            module.lang().send(sender, "admin.usage");
            return true;
        }

        if (sub.equals("vote")) {
            module.votes().onVote(args[1], "admin");
            module.lang().send(sender, "admin.vote_sent", "player", args[1]);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            module.lang().send(sender, "admin.player_offline", "player", args[1]);
            return true;
        }

        if (sub.equals("xp")) {
            int amount;
            try {
                amount = Integer.parseInt(args.length > 2 ? args[2] : "");
            } catch (NumberFormatException e) {
                module.lang().send(sender, "admin.usage");
                return true;
            }
            module.pass().addXp(target, amount);
            module.lang().send(sender, "admin.xp_given", "player", target.getName(), "amount", amount);
            return true;
        }

        PassPlayer state = module.pass().player(target);
        module.lang().send(sender, "admin.info", "player", target.getName(), "season", state.seasonId(),
                "xp", state.xp(), "level", module.pass().level(target),
                "premium", module.pass().isPremium(target), "streak", state.dailyStreak(),
                "votes", state.votesTotal());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return List.of();
    }

}
