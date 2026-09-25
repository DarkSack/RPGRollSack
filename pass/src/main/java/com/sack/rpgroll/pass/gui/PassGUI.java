package com.sack.rpgroll.pass.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.pass.PassModule;
import com.sack.rpgroll.pass.player.PassPlayer;
import com.sack.rpgroll.pass.reward.Reward;
import com.sack.rpgroll.pass.season.PassService;
import com.sack.rpgroll.pass.season.Season;
import com.sack.rpgroll.pass.season.SeasonLevel;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * El pase: siete niveles por página, con la pista gratis debajo y la premium
 * al fondo. Se abre en la página del nivel actual.
 */
public class PassGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 7;
    private static final int HEADER = 4;
    private static final int LEVEL_ROW = 10;
    private static final int FREE_ROW = 19;
    private static final int PREMIUM_ROW = 28;
    private static final int PREVIOUS = 45;
    private static final int MISSIONS = 47;
    private static final int CLAIM_ALL = 49;
    private static final int DAILY = 51;
    private static final int NEXT = 53;

    private final PassModule module;
    private final LangManager lang;
    private int page = -1;

    public PassGUI(Player player, PassModule module) {
        super(player, module.lang().component("gui.pass.title"), SIZE);
        this.module = module;
        this.lang = module.lang();
    }

    @Override
    public void build() {

        clear();
        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        PassService pass = module.pass();
        Optional<Season> open = pass.openSeason();

        if (open.isEmpty()) {
            setItem(22, GuiItems.item(Material.BARRIER, lang.component("gui.pass.closed"),
                    List.of(lang.component("gui.pass.closed_lore"))));
            return;
        }

        Season season = open.get();
        PassPlayer state = pass.player(player);
        int level = season.levelFor(state.xp());
        boolean premium = pass.isPremium(player);
        List<Integer> levels = new ArrayList<>(season.levels().keySet());
        int pages = Math.max(1, (levels.size() + PER_PAGE - 1) / PER_PAGE);

        if (page < 0) {
            int index = Math.max(0, levels.indexOf(Math.max(1, Math.min(level + 1, season.maxLevel()))));
            page = index / PER_PAGE;
        }
        page = Math.max(0, Math.min(page, pages - 1));

        setItem(HEADER, header(season, state, level, premium));
        setItem(LEVEL_ROW - 1, GuiItems.item(Material.EXPERIENCE_BOTTLE, lang.component("gui.pass.row_level"), List.of()));
        setItem(FREE_ROW - 1, GuiItems.item(Material.CHEST, lang.component("gui.pass.row_free"), List.of()));
        setItem(PREMIUM_ROW - 1, GuiItems.item(Material.ENDER_CHEST, lang.component("gui.pass.row_premium"),
                List.of(lang.component(premium ? "gui.pass.premium_active" : "gui.pass.premium_buy"))));

        for (int column = 0; column < PER_PAGE; column++) {

            int index = page * PER_PAGE + column;
            if (index >= levels.size()) {
                break;
            }

            SeasonLevel entry = season.levels().get(levels.get(index));
            boolean unlocked = level >= entry.level();

            setItem(LEVEL_ROW + column, GuiItems.item(
                    unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, entry.level(),
                    lang.component("gui.pass.level", "level", entry.level()),
                    List.of(lang.component(unlocked ? "gui.pass.level_unlocked" : "gui.pass.level_locked",
                            "xp", entry.level() * season.xpPerLevel()))));

            setItem(FREE_ROW + column, rewardItem(entry, entry.free(), false, state, unlocked, true));
            setItem(PREMIUM_ROW + column, rewardItem(entry, entry.premium(), true, state, unlocked, premium));
        }

        if (page > 0) {
            setItem(PREVIOUS, GuiItems.item(Material.ARROW, lang.component("gui.previous"), List.of()));
        }
        if (page < pages - 1) {
            setItem(NEXT, GuiItems.item(Material.ARROW, lang.component("gui.next"), List.of()));
        }

        setItem(MISSIONS, GuiItems.item(Material.WRITABLE_BOOK, lang.component("gui.pass.missions"),
                List.of(lang.component("gui.pass.missions_lore"))));
        setItem(CLAIM_ALL, GuiItems.item(Material.HOPPER, lang.component("gui.pass.claim_all"),
                List.of(lang.component("gui.pass.claim_all_lore"))));
        setItem(DAILY, GuiItems.item(Material.CLOCK, lang.component("gui.pass.daily"),
                List.of(lang.component(module.daily().canClaim(player) ? "gui.pass.daily_ready" : "gui.pass.daily_done"))));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case PREVIOUS -> {
                page--;
                build();
                return;
            }
            case NEXT -> {
                page++;
                build();
                return;
            }
            case MISSIONS -> {
                new MissionsGUI(player, module).open();
                return;
            }
            case DAILY -> {
                new DailyGUI(player, module).open();
                return;
            }
            case CLAIM_ALL -> {
                int claimed = module.pass().claimAll(player);
                lang.send(player, claimed > 0 ? "pass.claimed_all" : "pass.nothing_to_claim", "count", claimed);
                build();
                return;
            }
            default -> {
            }
        }

        boolean free = slot >= FREE_ROW && slot < FREE_ROW + PER_PAGE;
        boolean premium = slot >= PREMIUM_ROW && slot < PREMIUM_ROW + PER_PAGE;

        if (!free && !premium) {
            return;
        }

        Optional<Season> open = module.pass().openSeason();
        if (open.isEmpty()) {
            return;
        }

        List<Integer> levels = new ArrayList<>(open.get().levels().keySet());
        int index = page * PER_PAGE + (slot - (free ? FREE_ROW : PREMIUM_ROW));
        if (index >= levels.size()) {
            return;
        }

        int level = levels.get(index);
        PassService.ClaimResult result = module.pass().claim(player, level, premium);

        switch (result) {
            case CLAIMED -> {
                lang.send(player, "pass.claimed", "level", level);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            }
            case LOCKED -> lang.send(player, "pass.locked", "level", level);
            case NEEDS_PREMIUM -> lang.send(player, "pass.needs_premium");
            case ALREADY_CLAIMED -> lang.send(player, "pass.already_claimed");
            default -> {
            }
        }

        build();
    }

    private org.bukkit.inventory.ItemStack header(Season season, PassPlayer state, int level, boolean premium) {

        int intoLevel = state.xp() - level * season.xpPerLevel();
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("gui.pass.header_level", "level", level, "max", season.maxLevel()));

        if (level < season.maxLevel()) {
            lore.add(lang.component("gui.pass.header_xp", "bar", GuiItems.bar(intoLevel, season.xpPerLevel(), 20),
                    "xp", intoLevel, "needed", season.xpPerLevel()));
        } else {
            lore.add(lang.component("gui.pass.header_max"));
        }

        lore.add(lang.component(premium ? "gui.pass.header_premium" : "gui.pass.header_free"));
        lore.add(lang.component("gui.pass.header_ends", "date", season.end()));

        return GuiItems.item(Material.NETHER_STAR, com.sack.rpgroll.util.ComponentUtils.parse(season.displayName()),
                lore);
    }

    private org.bukkit.inventory.ItemStack rewardItem(SeasonLevel entry, List<Reward> rewards, boolean premiumTrack,
            PassPlayer state, boolean unlocked, boolean hasTrack) {

        if (rewards.isEmpty()) {
            return ItemBuilder.createFiller();
        }

        List<Component> lore = new ArrayList<>();
        rewards.forEach(reward -> lore.add(module.rewards().describe(reward)));
        lore.add(Component.empty());

        Material material = module.rewards().icon(rewards, Material.CHEST);
        String status;

        if (state.hasClaimed(entry.level(), premiumTrack)) {
            material = premiumTrack ? Material.ORANGE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
            status = "gui.pass.status_claimed";
        } else if (!unlocked) {
            status = "gui.pass.status_locked";
        } else if (!hasTrack) {
            status = "gui.pass.status_premium";
        } else {
            status = "gui.pass.status_ready";
        }

        lore.add(lang.component(status, "level", entry.level()));
        String title = premiumTrack ? "gui.pass.reward_premium" : "gui.pass.reward_free";
        int amount = rewards.get(0).type() == Reward.Type.MATERIAL ? rewards.get(0).amount() : 1;

        return GuiItems.item(material, amount, lang.component(title, "level", entry.level()), lore);
    }

}
