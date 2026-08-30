package com.sack.rpgroll.guilds.command;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.guilds.GuildServices;
import com.sack.rpgroll.guilds.gui.guild.GuildBrowserGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildHubGUI;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildCreationRequirements;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.GuildRole;
import com.sack.rpgroll.guilds.guild.chat.GuildChatChannel;
import com.sack.rpgroll.guilds.guild.ranking.GuildRankingManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/** /guild create|disband|invite|accept|decline|leave|kick|info|vault|territory|upgrade|diplomacy|quest|achievements|calendar|ranking|chat|customize */
public class GuildCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("create", "disband", "accept", "decline", "leave",
            "info", "gui", "vault", "territory", "upgrade", "diplomacy", "quest", "achievements", "calendar",
            "customize", "members", "browser", "ranking", "chat");

    private final GuildManager guildManager;
    private final GuildServices services;
    private final GuildCreationRequirements requirements;

    public GuildCommand(GuildManager guildManager, GuildServices services, GuildCreationRequirements requirements) {
        this.guildManager = guildManager;
        this.services = services;
        this.requirements = requirements;
    }

    private LangManager lang() {
        return services.langManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang().send(sender, "common.players_only");
            return true;
        }

        if (args.length < 1) {
            new GuildBrowserGUI(player, services).open();
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(player, args);
            case "disband" -> handleDisband(player);
            case "accept" -> handleAccept(player, args);
            case "decline" -> {
                guildManager.decline(player.getUniqueId());
                lang().send(player, "guild.decline.success");
            }
            case "leave" -> handleLeave(player);
            case "info", "gui", "vault", "territory", "upgrade", "diplomacy", "quest", "achievements", "calendar",
                    "customize", "members" ->
                openHub(player);
            case "browser" -> new GuildBrowserGUI(player, services).open();
            case "ranking" -> handleRanking(player, args);
            case "chat" -> handleChat(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        lang().send(player, "guild.usage");
    }

    private void openHub(Player player) {

        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        if (guild == null) {
            lang().send(player, "guild.not_in_guild_browser");
            return;
        }

        new GuildHubGUI(player, guild, services).open();
    }

    private void handleCreate(Player player, String[] args) {

        if (args.length < 2) {
            lang().send(player, "guild.create.usage");
            return;
        }

        if (guildManager.findByMember(player.getUniqueId()).isPresent()) {
            lang().send(player, "guild.already_in_guild");
            return;
        }

        String name = String.join(" ", java.util.Arrays.asList(args).subList(1, args.length)).trim();
        String id = name.toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9_]", "");

        if (id.isBlank() || guildManager.exists(id)) {
            lang().send(player, "guild.create.invalid_name");
            return;
        }

        if (!checkRequirements(player)) {
            return;
        }

        Guild guild = guildManager.create(id, name, player.getUniqueId());
        lang().send(player, "guild.create.success", "name", guild.name());
    }

    private boolean checkRequirements(Player player) {

        if (requirements.minLevel() > 0 && RPGRollAPI.isReady()) {
            int level = RPGRollAPI.get().getPlayer(player.getUniqueId()).map(rpgPlayer -> rpgPlayer.getLevel())
                    .orElse(0);
            if (level < requirements.minLevel()) {
                lang().send(player, "guild.create.requires_level", "level", requirements.minLevel());
                return false;
            }
        }

        if (requirements.requiredPermission() != null && !requirements.requiredPermission().isBlank()
                && !player.hasPermission(requirements.requiredPermission())) {
            lang().send(player, "guild.create.requires_permission");
            return false;
        }

        if (requirements.requiredItem() != null && requirements.requiredItemAmount() > 0) {
            ItemStack required = new ItemStack(requirements.requiredItem(), requirements.requiredItemAmount());
            if (!player.getInventory().containsAtLeast(required, requirements.requiredItemAmount())) {
                lang().send(player, "guild.create.requires_item", "amount", requirements.requiredItemAmount(),
                        "item", requirements.requiredItem());
                return false;
            }
        }

        if (requirements.requiredQuestId() != null && !requirements.requiredQuestId().isBlank()) {
            lang().send(player, "guild.create.quest_requirement_not_implemented", "quest",
                    requirements.requiredQuestId());
        }

        if (requirements.moneyCost() > 0) {

            if (!RPGRollAPI.isReady() || !RPGRollAPI.get().getEconomyProvider().isAvailable()) {
                return true;
            }

            var economy = RPGRollAPI.get().getEconomyProvider().getEconomy().orElseThrow();

            if (economy.getBalance(player) < requirements.moneyCost()) {
                lang().send(player, "guild.create.requires_money", "amount", requirements.moneyCost());
                return false;
            }

            economy.withdrawPlayer(player, requirements.moneyCost());
        }

        if (requirements.requiredItem() != null && requirements.requiredItemAmount() > 0) {
            player.getInventory().removeItem(new ItemStack(requirements.requiredItem(),
                    requirements.requiredItemAmount()));
        }

        return true;
    }

    private void handleDisband(Player player) {

        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        if (guild == null) {
            lang().send(player, "guild.not_in_guild");
            return;
        }

        if (guild.roleOf(player.getUniqueId()) != GuildRole.LEADER) {
            lang().send(player, "guild.disband.only_leader");
            return;
        }

        guildManager.disband(guild.id());
        lang().send(player, "guild.disband.success");
    }

    private void handleAccept(Player player, String[] args) {

        if (args.length < 2) {
            lang().send(player, "guild.accept.usage");
            return;
        }

        var result = guildManager.accept(player.getUniqueId(), args[1]);

        switch (result) {
            case OK -> lang().send(player, "guild.accept.success");
            case NO_INVITE -> lang().send(player, "guild.accept.no_invite");
            case EXPIRED -> lang().send(player, "guild.accept.expired");
            case GUILD_GONE -> lang().send(player, "guild.accept.guild_gone");
            case ALREADY_IN_GUILD -> lang().send(player, "guild.already_in_guild");
        }
    }

    private void handleLeave(Player player) {

        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        if (guild == null) {
            lang().send(player, "guild.not_in_guild");
            return;
        }

        if (guild.roleOf(player.getUniqueId()) == GuildRole.LEADER && guild.memberCount() > 1) {
            lang().send(player, "guild.leave.is_leader");
            return;
        }

        if (guild.memberCount() <= 1) {
            guildManager.disband(guild.id());
            lang().send(player, "guild.disband.success");
        } else {
            guild.removeMember(player.getUniqueId());
            guildManager.save(guild);
            lang().send(player, "guild.leave.success");
        }
    }

    private void handleRanking(Player player, String[] args) {

        GuildRankingManager.Category category = GuildRankingManager.Category.LEVEL;

        if (args.length >= 2) {
            try {
                category = GuildRankingManager.Category.valueOf(args[1].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<Guild> top = services.rankingManager().top(category, 10);

        lang().send(player, "guild.ranking.header", "category", category);

        for (int i = 0; i < top.size(); i++) {
            Guild guild = top.get(i);
            lang().send(player, "guild.ranking.entry", "position", i + 1, "name", guild.name(),
                    "value", category.valueOf(guild));
        }
    }

    private void handleChat(Player player, String[] args) {

        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        if (guild == null) {
            lang().send(player, "guild.not_in_guild");
            return;
        }

        GuildChatChannel channel = GuildChatChannel.GUILD;

        if (args.length >= 2) {
            try {
                channel = GuildChatChannel.valueOf(args[1].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (channel == GuildChatChannel.OFFICERS && !guild.roleOf(player.getUniqueId()).canManageSettings()) {
            lang().send(player, "guild.chat.officers_only");
            return;
        }

        services.guildChatListener().setChannel(player, channel);
        lang().send(player, "guild.chat.channel_changed", "channel", channel);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "accept" -> TabCompleteUtil.filter(args[1],
                        guildManager.getAll().stream().map(Guild::id).toList());
                case "ranking" -> TabCompleteUtil.filter(args[1], java.util.Arrays.stream(
                        GuildRankingManager.Category.values()).map(Enum::name).toList());
                case "chat" -> TabCompleteUtil.filter(args[1],
                        java.util.Arrays.stream(GuildChatChannel.values()).map(Enum::name).toList());
                default -> List.of();
            };
        }

        return List.of();
    }

}
