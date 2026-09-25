package com.sack.rpgroll.pass.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.pass.PassModule;
import com.sack.rpgroll.pass.daily.DailyConfig;
import com.sack.rpgroll.pass.daily.DailyService;
import com.sack.rpgroll.pass.reward.Reward;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** El calendario de recompensas diarias: siete días a la vista y el de hoy para reclamar. */
public class DailyGUI extends InventoryGUI {

    private static final int SIZE = 36;
    private static final int FIRST_DAY = 10;
    private static final int WINDOW = 7;
    private static final int STREAK = 4;
    private static final int BONUS = 22;
    private static final int BACK = 31;

    private final PassModule module;
    private final LangManager lang;
    private int todaySlot = -1;

    public DailyGUI(Player player, PassModule module) {
        super(player, module.lang().component("gui.daily.title"), SIZE);
        this.module = module;
        this.lang = module.lang();
    }

    @Override
    public void build() {

        clear();
        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        DailyService daily = module.daily();
        List<List<Reward>> days = daily.config().days();
        boolean claimable = daily.canClaim(player);
        int streak = claimable ? daily.streakIfClaimedToday(player) : module.pass().player(player).dailyStreak();
        int current = daily.cycleIndex(Math.max(1, streak));
        int windowStart = (current / WINDOW) * WINDOW;

        setItem(STREAK, GuiItems.item(Material.BLAZE_POWDER, lang.component("gui.daily.streak", "streak",
                        module.pass().player(player).dailyStreak()),
                List.of(lang.component(daily.config().resetIfMissed() ? "gui.daily.streak_resets" : "gui.daily.streak_keeps"))));

        todaySlot = -1;
        for (int i = 0; i < WINDOW && windowStart + i < days.size(); i++) {

            int index = windowStart + i;
            List<Component> lore = new ArrayList<>();
            days.get(index).forEach(reward -> lore.add(module.rewards().describe(reward)));
            lore.add(Component.empty());

            Material material;
            String status;

            if (index < current || (index == current && !claimable)) {
                material = Material.LIME_STAINED_GLASS_PANE;
                status = "gui.daily.status_claimed";
            } else if (index == current) {
                material = Material.CHEST;
                status = "gui.daily.status_ready";
                todaySlot = FIRST_DAY + i;
            } else {
                material = module.rewards().icon(days.get(index), Material.BARREL);
                status = "gui.daily.status_upcoming";
            }

            lore.add(lang.component(status));
            setItem(FIRST_DAY + i, GuiItems.item(material, index + 1,
                    lang.component("gui.daily.day", "day", index + 1), lore));
        }

        setItem(BONUS, bonusItem(daily));
        setItem(BACK, GuiItems.item(Material.ARROW, lang.component("gui.back"), List.of()));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == BACK) {
            new PassGUI(player, module).open();
            return;
        }

        if (slot == todaySlot) {
            if (!module.daily().claim(player)) {
                lang.send(player, "daily.already");
            }
            build();
        }
    }

    private org.bukkit.inventory.ItemStack bonusItem(DailyService daily) {

        Optional<DailyConfig.RankBonus> mine = daily.bonusFor(player);
        List<Component> lore = new ArrayList<>();

        for (DailyConfig.RankBonus bonus : daily.config().bonuses()) {
            boolean active = mine.isPresent() && mine.get() == bonus;
            lore.add(lang.component(active ? "gui.daily.bonus_active" : "gui.daily.bonus_rank", "rank", bonus.name()));
            bonus.rewards().forEach(reward -> lore.add(Component.text("   ").append(module.rewards().describe(reward))));
        }

        return GuiItems.item(Material.GOLD_BLOCK, lang.component("gui.daily.bonus_title"), lore);
    }

}
