package com.sack.rpgroll.guilds.command;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.guilds.GuildServices;
import com.sack.rpgroll.guilds.gui.guild.GuildBrowserGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildHubGUI;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildCreationRequirements;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.GuildRole;
import com.sack.rpgroll.guilds.guild.chat.GuildChatChannel;
import com.sack.rpgroll.guilds.guild.ranking.GuildRankingManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/** /guild create|disband|invite|accept|decline|leave|kick|info|vault|territory|upgrade|diplomacy|quest|achievements|calendar|ranking|chat|customize */
public class GuildCommand implements CommandExecutor {

    private final GuildManager guildManager;
    private final GuildServices services;
    private final GuildCreationRequirements requirements;

    public GuildCommand(GuildManager guildManager, GuildServices services, GuildCreationRequirements requirements) {
        this.guildManager = guildManager;
        this.services = services;
        this.requirements = requirements;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("Invitación rechazada.", NamedTextColor.GRAY));
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
        player.sendMessage(Component.text(
                "Uso: /guild <create|disband|accept|decline|leave|ranking|chat|gui> [args]", NamedTextColor.YELLOW));
    }

    private void openHub(Player player) {

        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        if (guild == null) {
            player.sendMessage(Component.text("No estás en ninguna guild — usá /guild browser.", NamedTextColor.RED));
            return;
        }

        new GuildHubGUI(player, guild, services).open();
    }

    private void handleCreate(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /guild create <nombre>", NamedTextColor.YELLOW));
            return;
        }

        if (guildManager.findByMember(player.getUniqueId()).isPresent()) {
            player.sendMessage(Component.text("Ya estás en una guild.", NamedTextColor.RED));
            return;
        }

        String name = String.join(" ", java.util.Arrays.asList(args).subList(1, args.length)).trim();
        String id = name.toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9_]", "");

        if (id.isBlank() || guildManager.exists(id)) {
            player.sendMessage(Component.text("Ese nombre no es válido o ya existe una guild con ese id.",
                    NamedTextColor.RED));
            return;
        }

        if (!checkRequirements(player)) {
            return;
        }

        Guild guild = guildManager.create(id, name, player.getUniqueId());
        player.sendMessage(Component.text("✔ Guild '" + guild.name() + "' fundada.", NamedTextColor.GREEN));
    }

    private boolean checkRequirements(Player player) {

        if (requirements.minLevel() > 0 && RPGRollAPI.isReady()) {
            int level = RPGRollAPI.get().getPlayer(player.getUniqueId()).map(rpgPlayer -> rpgPlayer.getLevel())
                    .orElse(0);
            if (level < requirements.minLevel()) {
                player.sendMessage(Component.text("Necesitás nivel " + requirements.minLevel()
                        + " para fundar una guild.", NamedTextColor.RED));
                return false;
            }
        }

        if (requirements.requiredPermission() != null && !requirements.requiredPermission().isBlank()
                && !player.hasPermission(requirements.requiredPermission())) {
            player.sendMessage(Component.text("No tenés el permiso necesario para fundar una guild.",
                    NamedTextColor.RED));
            return false;
        }

        if (requirements.requiredItem() != null && requirements.requiredItemAmount() > 0) {
            ItemStack required = new ItemStack(requirements.requiredItem(), requirements.requiredItemAmount());
            if (!player.getInventory().containsAtLeast(required, requirements.requiredItemAmount())) {
                player.sendMessage(Component.text("Necesitás " + requirements.requiredItemAmount() + "x "
                        + requirements.requiredItem() + " para fundar una guild.", NamedTextColor.RED));
                return false;
            }
        }

        if (requirements.requiredQuestId() != null && !requirements.requiredQuestId().isBlank()) {
            player.sendMessage(Component.text(
                    "Aviso: esta guild requiere haber completado la quest '" + requirements.requiredQuestId()
                            + "', pero esa verificación todavía no está implementada — el requisito no se aplica.",
                    NamedTextColor.GRAY));
        }

        if (requirements.moneyCost() > 0) {

            if (!RPGRollAPI.isReady() || !RPGRollAPI.get().getEconomyProvider().isAvailable()) {
                return true;
            }

            var economy = RPGRollAPI.get().getEconomyProvider().getEconomy().orElseThrow();

            if (economy.getBalance(player) < requirements.moneyCost()) {
                player.sendMessage(Component.text("Necesitás " + requirements.moneyCost()
                        + " de dinero para fundar una guild.", NamedTextColor.RED));
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
            player.sendMessage(Component.text("No estás en ninguna guild.", NamedTextColor.RED));
            return;
        }

        if (guild.roleOf(player.getUniqueId()) != GuildRole.LEADER) {
            player.sendMessage(Component.text("Solo el líder puede disolver la guild.", NamedTextColor.RED));
            return;
        }

        guildManager.disband(guild.id());
        player.sendMessage(Component.text("Guild disuelta.", NamedTextColor.GRAY));
    }

    private void handleAccept(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /guild accept <id-de-guild>", NamedTextColor.YELLOW));
            return;
        }

        var result = guildManager.accept(player.getUniqueId(), args[1]);

        switch (result) {
            case OK -> player.sendMessage(Component.text("✔ Te uniste a la guild.", NamedTextColor.GREEN));
            case NO_INVITE -> player.sendMessage(Component.text("No tenés ninguna invitación pendiente de esa guild.",
                    NamedTextColor.RED));
            case EXPIRED -> player.sendMessage(Component.text("Esa invitación ya expiró.", NamedTextColor.RED));
            case GUILD_GONE -> player.sendMessage(Component.text("Esa guild ya no existe.", NamedTextColor.RED));
            case ALREADY_IN_GUILD -> player.sendMessage(Component.text("Ya estás en una guild.", NamedTextColor.RED));
        }
    }

    private void handleLeave(Player player) {

        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        if (guild == null) {
            player.sendMessage(Component.text("No estás en ninguna guild.", NamedTextColor.RED));
            return;
        }

        if (guild.roleOf(player.getUniqueId()) == GuildRole.LEADER && guild.memberCount() > 1) {
            player.sendMessage(Component.text(
                    "Sos el líder — transferí el liderazgo o usá /guild disband.", NamedTextColor.RED));
            return;
        }

        if (guild.memberCount() <= 1) {
            guildManager.disband(guild.id());
            player.sendMessage(Component.text("Guild disuelta.", NamedTextColor.GRAY));
        } else {
            guild.removeMember(player.getUniqueId());
            guildManager.save(guild);
            player.sendMessage(Component.text("Abandonaste la guild.", NamedTextColor.GRAY));
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

        player.sendMessage(Component.text("=== Ranking de Guilds (" + category + ") ===", NamedTextColor.GOLD));

        for (int i = 0; i < top.size(); i++) {
            Guild guild = top.get(i);
            player.sendMessage(Component.text((i + 1) + ". " + guild.name() + " — "
                    + category.valueOf(guild), NamedTextColor.GRAY));
        }
    }

    private void handleChat(Player player, String[] args) {

        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        if (guild == null) {
            player.sendMessage(Component.text("No estás en ninguna guild.", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Solo los oficiales/líder pueden usar ese canal.", NamedTextColor.RED));
            return;
        }

        services.guildChatListener().setChannel(player, channel);
        player.sendMessage(Component.text("Chat cambiado a canal: " + channel, NamedTextColor.AQUA));
    }

}
