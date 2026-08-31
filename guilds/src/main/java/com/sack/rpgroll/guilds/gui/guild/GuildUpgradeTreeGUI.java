package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.GuildsAPI;
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
        super(player, lang().component("guild.upgrade.title", "name", guild.name()), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.onBack = onBack;
    }

    private static LangManager lang() {
        return GuildsAPI.getLangManager();
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
                    .setName(ComponentUtils.parseWithDefault(lang().raw("guild.upgrade.branch_level", "branch", branch.displayName(lang()),
                            "level", level, "max", GuildUpgradeBranch.MAX_LEVEL), NamedTextColor.YELLOW))
                    .setLore(
                            Component.text(branch.description(lang()), NamedTextColor.GRAY),
                            maxed ? ComponentUtils.parseWithDefault(lang().raw("guild.upgrade.max_level"), NamedTextColor.GREEN)
                                    : ComponentUtils.parseWithDefault(lang().raw("guild.upgrade.cost", "cost", cost), NamedTextColor.GOLD),
                            maxed ? Component.empty() : ComponentUtils.parseWithDefault(lang().raw("guild.upgrade.click_hint"), NamedTextColor.AQUA))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("common.back")));
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
            lang().send(player, "guild.upgrade.no_permission");
            return;
        }

        GuildUpgradeBranch branch = branches[slot];
        int level = guild.upgradeTree().level(branch);

        if (level >= GuildUpgradeBranch.MAX_LEVEL) {
            return;
        }

        double cost = branch.upgradeCost(level);

        if (!guild.vault().withdraw(cost, VaultTransaction.of(player.getUniqueId(), player.getName(),
                VaultTransactionType.WITHDRAW_MONEY, cost, lang().raw("guild.upgrade.log.upgrade_of",
                        "branch", branch.displayName(lang()))))) {
            lang().send(player, "guild.upgrade.insufficient_funds", "cost", cost);
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
