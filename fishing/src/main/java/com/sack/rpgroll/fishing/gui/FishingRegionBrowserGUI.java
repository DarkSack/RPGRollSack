package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.FishingRegion;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.WaterType;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class FishingRegionBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final FishingRegionManager regionManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<FishingRegion> regions;

    public FishingRegionBrowserGUI(Player player, FishingRegionManager regionManager,
            ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.region.browser_title"), SIZE);
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.regions = List.copyOf(regionManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < regions.size() && i < 36; i++) {

            FishingRegion region = regions.get(i);

            setItem(i, new ItemBuilder(Material.MAP)
                    .setName(Component.text(region.id(), NamedTextColor.LIGHT_PURPLE))
                    .setLore(lang.component("gui.region.lore_world", "world", region.world()),
                            lang.component("gui.region.lore_forced_water", "value", region.forcedWaterType()),
                            lang.component("gui.common.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.region.new_button"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < regions.size() && slot < 36) {
            new FishingRegionEditorGUI(player, regions.get(slot), regionManager, chatPromptManager, this::reopen)
                    .open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, lang.raw("gui.region.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (regionManager.exists(id)) {
                lang.send(player, "gui.region.already_exists");
                reopen();
                return;
            }

            var loc = player.getLocation();

            regionManager.save(new FishingRegion(id, loc.getWorld().getName(),
                    loc.getBlockX() - 10, loc.getBlockY() - 5, loc.getBlockZ() - 10,
                    loc.getBlockX() + 10, loc.getBlockY() + 5, loc.getBlockZ() + 10, WaterType.MAGIC_WATER));

            reopen();
        });
    }

    private void reopen() {
        this.regions = List.copyOf(regionManager.getAll());
        open();
    }

}
