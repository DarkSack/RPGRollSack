package com.sack.rpgroll.pass.command;

import com.sack.rpgroll.pass.PassModule;
import com.sack.rpgroll.pass.gui.DailyGUI;
import com.sack.rpgroll.pass.gui.MissionsGUI;
import com.sack.rpgroll.pass.gui.PassGUI;
import com.sack.rpgroll.pass.player.PassPlayer;
import com.sack.rpgroll.pass.vote.VoteConfig;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /pase, /diario y /votar: los tres comandos de jugador. */
public final class PlayerCommands {

    private PlayerCommands() {
    }

    /** /pase [misiones|reclamar] */
    public static final class Pass implements CommandExecutor, TabCompleter {

        private final PassModule module;

        public Pass(PassModule module) {
            this.module = module;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

            if (!(sender instanceof Player player)) {
                module.lang().send(sender, "command.players_only");
                return true;
            }

            String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

            switch (sub) {
                case "misiones", "missions" -> new MissionsGUI(player, module).open();
                case "reclamar", "claim" -> {
                    int claimed = module.pass().claimAll(player);
                    module.lang().send(player, claimed > 0 ? "pass.claimed_all" : "pass.nothing_to_claim",
                            "count", claimed);
                }
                default -> new PassGUI(player, module).open();
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return args.length == 1 ? List.of("misiones", "reclamar") : List.of();
        }

    }

    /** /diario */
    public static final class Daily implements CommandExecutor {

        private final PassModule module;

        public Daily(PassModule module) {
            this.module = module;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

            if (sender instanceof Player player) {
                new DailyGUI(player, module).open();
            } else {
                module.lang().send(sender, "command.players_only");
            }
            return true;
        }

    }

    /** /votar: enlaces clicables a cada página y tus votos. */
    public static final class Vote implements CommandExecutor {

        private final PassModule module;

        public Vote(PassModule module) {
            this.module = module;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

            VoteConfig config = module.votes().config();
            module.lang().send(sender, "votes.header");

            if (config.sites().isEmpty()) {
                module.lang().send(sender, "votes.no_sites");
            }

            for (VoteConfig.Site site : config.sites()) {
                sender.sendMessage(module.lang().component("votes.site", "name", site.name())
                        .clickEvent(ClickEvent.openUrl(site.url()))
                        .hoverEvent(HoverEvent.showText(ComponentUtils.parse("&7" + site.url()))));
            }

            if (sender instanceof Player player) {
                PassPlayer state = module.pass().player(player);
                module.lang().send(sender, "votes.stats", "total", state.votesTotal(), "streak", state.voteStreak());
            }
            return true;
        }

    }

}
