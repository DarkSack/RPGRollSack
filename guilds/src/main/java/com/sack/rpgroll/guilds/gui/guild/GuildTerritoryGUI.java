package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.territory.GuildTerritory;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * Reclamo simple sin WorldGuard: "reclamar acá" crea un cuboide centrado
 * en la ubicación actual del jugador con el radio pedido — no hay
 * herramienta de selección de dos puntos en esta pasada.
 */
public class GuildTerritoryGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int CLAIM_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final Guild guild;
    private final GuildManager guildManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final List<GuildTerritory> territories;

    public GuildTerritoryGUI(Player player, Guild guild, GuildManager guildManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("guild.territory.title", "name", guild.name()), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.territories = guild.territories();
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < territories.size() && i < 36; i++) {

            GuildTerritory territory = territories.get(i);

            setItem(i, new ItemBuilder(Material.GRASS_BLOCK)
                    .setName(ComponentUtils.parse(territory.name()))
                    .setLore(ComponentUtils.parseWithDefault(lang().raw("guild.territory.lore.world", "world", territory.world()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(lang().raw("guild.territory.lore.block_protection", "state",
                                    territory.protectBlocks() ? lang().raw("common.yes") : lang().raw("common.no")), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(lang().raw("guild.territory.lore.outsider_pvp", "state",
                                    territory.allowOutsiderPvp() ? lang().raw("common.yes") : lang().raw("common.no")), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(lang().raw("guild.territory.lore.toggle_hint"), NamedTextColor.DARK_GRAY),
                            ComponentUtils.parseWithDefault(lang().raw("guild.territory.lore.release_hint"), NamedTextColor.DARK_GRAY))
                    .build());
        }

        int maxTerritories = guild.upgradeTree().maxTerritories();

        setItem(CLAIM_SLOT, new ItemBuilder(territories.size() < maxTerritories ? Material.EMERALD : Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(lang().raw("guild.territory.button.claim"), NamedTextColor.GREEN))
                .setLore(ComponentUtils.parseWithDefault(lang().raw("guild.territory.lore.quota", "count", territories.size(),
                        "max", maxTerritories), NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        boolean canManage = guild.roleOf(player.getUniqueId()).canManageSettings();

        if (slot < territories.size() && slot < 36) {

            if (!canManage) {
                lang().send(player, "guild.territory.no_permission_manage");
                return;
            }

            GuildTerritory territory = territories.get(slot);

            if (event.isShiftClick()) {
                guild.removeTerritory(territory.name());
                guildManager.save(guild);
                reopen();
                return;
            }

            if (event.isRightClick()) {
                territory.setAllowOutsiderPvp(!territory.allowOutsiderPvp());
            } else {
                territory.setProtectBlocks(!territory.protectBlocks());
            }

            guildManager.save(guild);
            build();
            return;
        }

        if (slot == CLAIM_SLOT) {

            if (!canManage) {
                lang().send(player, "guild.territory.no_permission_claim");
                return;
            }

            if (territories.size() >= guild.upgradeTree().maxTerritories()) {
                lang().send(player, "guild.territory.quota_reached");
                return;
            }

            promptClaim();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptClaim() {
        chatPromptManager.prompt(player, "guild.territory.prompt_claim", value -> {

            String[] parts = value.split(";", 2);

            if (parts.length < 2) {
                lang().send(player, "guild.territory.invalid_format");
                reopen();
                return;
            }

            double radius;
            try {
                radius = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                lang().send(player, "guild.territory.radius_not_a_number");
                reopen();
                return;
            }

            Location center = player.getLocation();
            String name = parts[0].trim();

            GuildTerritory territory = new GuildTerritory(name, center.getWorld().getName(),
                    center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                    center.getX() + radius, center.getY() + radius, center.getZ() + radius);

            boolean overlaps = guildManager.getAll().stream()
                    .flatMap(g -> g.territories().stream())
                    .anyMatch(existing -> existing.overlaps(territory.world(), territory.minX(), territory.minY(),
                            territory.minZ(), territory.maxX(), territory.maxY(), territory.maxZ()));

            if (overlaps) {
                lang().send(player, "guild.territory.overlaps");
                reopen();
                return;
            }

            guild.addTerritory(territory);
            guildManager.save(guild);
            lang().send(player, "guild.territory.claimed", "name", name);
            reopen();
        });
    }

    private void reopen() {
        new GuildTerritoryGUI(player, guild, guildManager, chatPromptManager, onBack).open();
    }

}
