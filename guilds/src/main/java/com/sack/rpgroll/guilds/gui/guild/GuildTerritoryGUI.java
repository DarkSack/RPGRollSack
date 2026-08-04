package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.territory.GuildTerritory;

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
        super(player, Component.text("Territorios: " + guild.name(), NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.territories = guild.territories();
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
                    .setName(Component.text(territory.name(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Mundo: " + territory.world(), NamedTextColor.GRAY),
                            Component.text("Protección de bloques: " + (territory.protectBlocks() ? "sí" : "no"),
                                    NamedTextColor.GRAY),
                            Component.text("PvP externo permitido: " + (territory.allowOutsiderPvp() ? "sí" : "no"),
                                    NamedTextColor.GRAY),
                            Component.text("Click: alternar protección · Click derecho: alternar PvP",
                                    NamedTextColor.DARK_GRAY),
                            Component.text("Shift-click: liberar territorio", NamedTextColor.DARK_GRAY))
                    .build());
        }

        int maxTerritories = guild.upgradeTree().maxTerritories();

        setItem(CLAIM_SLOT, new ItemBuilder(territories.size() < maxTerritories ? Material.EMERALD : Material.BARRIER)
                .setName(Component.text("Reclamar territorio acá", NamedTextColor.GREEN))
                .setLore(Component.text("Cupo: " + territories.size() + "/" + maxTerritories
                        + " (mejorá la rama Territorio para más)", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        boolean canManage = guild.roleOf(player.getUniqueId()).canManageSettings();

        if (slot < territories.size() && slot < 36) {

            if (!canManage) {
                player.sendMessage(Component.text("No tenés permiso para gestionar territorios.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("No tenés permiso para reclamar territorio.", NamedTextColor.RED));
                return;
            }

            if (territories.size() >= guild.upgradeTree().maxTerritories()) {
                player.sendMessage(Component.text("Alcanzaste el cupo de territorios — mejorá la rama Territorio.",
                        NamedTextColor.RED));
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
        chatPromptManager.prompt(player, "Escribí: nombre;radio (ej. base;40):", value -> {

            String[] parts = value.split(";", 2);

            if (parts.length < 2) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                reopen();
                return;
            }

            double radius;
            try {
                radius = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("El radio debe ser un número.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("Ese territorio se superpone con uno ya reclamado.",
                        NamedTextColor.RED));
                reopen();
                return;
            }

            guild.addTerritory(territory);
            guildManager.save(guild);
            player.sendMessage(Component.text("✔ Territorio '" + name + "' reclamado.", NamedTextColor.GREEN));
            reopen();
        });
    }

    private void reopen() {
        new GuildTerritoryGUI(player, guild, guildManager, chatPromptManager, onBack).open();
    }

}
