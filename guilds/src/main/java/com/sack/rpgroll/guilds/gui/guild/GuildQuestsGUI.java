package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestDefinition;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestProgress;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Quests activas de la guild (arriba) + quests disponibles para iniciar (abajo). */
public class GuildQuestsGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int BACK_SLOT = 53;

    private final Guild guild;
    private final GuildManager guildManager;
    private final GuildQuestManager questManager;
    private final GuildQuestService questService;
    private final Runnable onBack;

    private final List<GuildQuestProgress> active;
    private final List<GuildQuestDefinition> available;

    public GuildQuestsGUI(Player player, Guild guild, GuildManager guildManager, GuildQuestManager questManager,
            GuildQuestService questService, Runnable onBack) {
        super(player, Component.text("Quests: " + guild.name(), NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.questManager = questManager;
        this.questService = questService;
        this.onBack = onBack;

        this.active = guild.activeQuests().stream().filter(p -> !p.completed()).toList();

        List<String> activeIds = active.stream().map(GuildQuestProgress::questId).toList();
        this.available = new ArrayList<>(questManager.getAll().stream()
                .filter(def -> !activeIds.contains(def.id()))
                .filter(def -> guild.level() >= def.minGuildLevel())
                .toList());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < active.size() && i < 18; i++) {

            GuildQuestProgress progress = active.get(i);
            GuildQuestDefinition definition = questManager.get(progress.questId()).orElse(null);
            String name = definition != null ? definition.displayName() : progress.questId();
            int target = definition != null ? definition.targetAmount() : 1;

            setItem(i, new ItemBuilder(Material.MAP)
                    .setName(Component.text(name, NamedTextColor.AQUA))
                    .setLore(Component.text("Progreso: " + progress.currentAmount() + "/" + target,
                            NamedTextColor.GRAY))
                    .build());
        }

        for (int i = 0; i < available.size() && i < 18; i++) {

            GuildQuestDefinition definition = available.get(i);

            setItem(18 + i, new ItemBuilder(Material.BOOK)
                    .setName(Component.text(definition.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text(definition.description(), NamedTextColor.GRAY),
                            Component.text("Tipo: " + definition.type() + " · Objetivo: " + definition.targetAmount(),
                                    NamedTextColor.GRAY),
                            Component.text("Recompensa: " + definition.rewardMoney() + " oro, "
                                    + definition.rewardXp() + " XP de guild", NamedTextColor.GRAY),
                            Component.text("Click para iniciar", NamedTextColor.GREEN))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot >= 18 && slot < 18 + available.size() && slot < 36) {

            if (!guild.roleOf(player.getUniqueId()).canManageSettings()) {
                player.sendMessage(Component.text("No tenés permiso para iniciar quests de guild.",
                        NamedTextColor.RED));
                return;
            }

            GuildQuestDefinition definition = available.get(slot - 18);
            var result = questService.start(guild, definition.id());
            player.sendMessage(Component.text("Resultado: " + result, NamedTextColor.GRAY));
            guildManager.save(guild);
            reopen();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        new GuildQuestsGUI(player, guild, guildManager, questManager, questService, onBack).open();
    }

}
