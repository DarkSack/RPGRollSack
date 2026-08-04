package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.bank.VaultTransaction;
import com.sack.rpgroll.guilds.guild.bank.VaultTransactionType;
import com.sack.rpgroll.guilds.guild.upgrade.GuildUpgradeBranch;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuildUpgradeTreeGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int BACK_SLOT = 26;

    private final Guild guild;
    private final GuildManager guildManager;
    private final Runnable onBack;

    public GuildUpgradeTreeGUI(Player player, Guild guild, GuildManager guildManager, Runnable onBack) {
        super(player, Component.text("Mejoras: " + guild.name(), NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.onBack = onBack;
    }

    private Material iconFor(GuildUpgradeBranch branch) {
        return switch (branch) {
            case BANK -> Material.CHEST;
            case ECONOMY -> Material.GOLD_INGOT;
            case MEMBERS -> Material.PLAYER_HEAD;
            case BUFFS -> Material.NETHER_STAR;
            case TERRITORY -> Material.GRASS_BLOCK;
            case EVENTS -> Material.CLOCK;
        };
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        GuildUpgradeBranch[] branches = GuildUpgradeBranch.values();

        for (int i = 0; i < branches.length; i++) {

            GuildUpgradeBranch branch = branches[i];
            int level = guild.upgradeTree().level(branch);
            boolean maxed = level >= GuildUpgradeBranch.MAX_LEVEL;
            double cost = branch.upgradeCost(level);

            setItem(i, new ItemBuilder(iconFor(branch))
                    .setName(Component.text(branch.displayName() + " — Nivel " + level + "/"
                            + GuildUpgradeBranch.MAX_LEVEL, NamedTextColor.YELLOW))
                    .setLore(
                            Component.text(branch.description(), NamedTextColor.GRAY),
                            maxed ? Component.text("Nivel máximo alcanzado", NamedTextColor.GREEN)
                                    : Component.text("Costo: " + cost, NamedTextColor.GOLD),
                            maxed ? Component.empty() : Component.text("Click para mejorar", NamedTextColor.AQUA))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            onBack.run();
            return;
        }

        GuildUpgradeBranch[] branches = GuildUpgradeBranch.values();

        if (slot < 0 || slot >= branches.length) {
            return;
        }

        if (!guild.roleOf(player.getUniqueId()).canManageSettings()) {
            player.sendMessage(Component.text("No tenés permiso para mejorar la guild.", NamedTextColor.RED));
            return;
        }

        GuildUpgradeBranch branch = branches[slot];
        int level = guild.upgradeTree().level(branch);

        if (level >= GuildUpgradeBranch.MAX_LEVEL) {
            return;
        }

        double cost = branch.upgradeCost(level);

        if (!guild.vault().withdraw(cost, VaultTransaction.of(player.getUniqueId(), player.getName(),
                VaultTransactionType.WITHDRAW_MONEY, cost, "Mejora de " + branch.displayName()))) {
            player.sendMessage(Component.text("El vault de la guild no tiene fondos suficientes ("
                    + cost + ").", NamedTextColor.RED));
            return;
        }

        guild.upgradeTree().upgrade(branch);

        if (branch == GuildUpgradeBranch.BANK) {
            guild.vault().resize(guild.upgradeTree().vaultSlots());
        }

        guildManager.save(guild);
        build();
    }

}
