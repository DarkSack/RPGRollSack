package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.achievement.GuildAchievementDefinition;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuildAchievementsGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int BACK_SLOT = 26;

    private final Guild guild;
    private final Runnable onBack;

    public GuildAchievementsGUI(Player player, Guild guild, Runnable onBack) {
        super(player, Component.text(GuildsAPI.getLangManager().raw("guild.achievements.title", "name", guild.name()),
                NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.onBack = onBack;
    }

    private static com.sack.rpgroll.common.lang.LangManager lang() {
        return GuildsAPI.getLangManager();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        var definitions = GuildAchievementDefinition.ALL;

        for (int i = 0; i < definitions.size() && i < 18; i++) {

            GuildAchievementDefinition definition = definitions.get(i);
            boolean unlocked = guild.unlockedAchievements().contains(definition.id());

            setItem(i, new ItemBuilder(unlocked ? Material.GOLD_INGOT : Material.GRAY_DYE)
                    .setName(Component.text(definition.displayName(), unlocked ? NamedTextColor.GOLD
                            : NamedTextColor.DARK_GRAY))
                    .setLore(Component.text(definition.description(), NamedTextColor.GRAY),
                            Component.text(lang().raw(unlocked ? "guild.achievements.unlocked"
                                    : "guild.achievements.locked"),
                                    unlocked ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        if (event.getSlot() == BACK_SLOT) {
            onBack.run();
        }
    }

}
