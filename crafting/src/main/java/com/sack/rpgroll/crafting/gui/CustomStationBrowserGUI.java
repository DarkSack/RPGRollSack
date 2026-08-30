package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CustomStationBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final CustomStationManager stationManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<CustomStation> stations;

    public CustomStationBrowserGUI(Player player, CustomStationManager stationManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.station.browser_title"), NamedTextColor.GOLD), SIZE);
        this.stationManager = stationManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.stations = List.copyOf(stationManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < stations.size() && i < 36; i++) {

            CustomStation station = stations.get(i);

            setItem(i, new ItemBuilder(parseMaterial(station.icon()))
                    .setName(ComponentUtils.parse(station.displayName()))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.common.id_lore", "id", station.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.station.block_lore", "block", station.triggerBlockMaterial()), NamedTextColor.AQUA),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.station.create_new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < stations.size() && slot < 36) {
            new CustomStationEditorGUI(player, stations.get(slot), stationManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.station.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (stationManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.station.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            stationManager.save(new CustomStation(id, id, "SMITHING_TABLE", "SMITHING_TABLE", 27,
                    List.of(0, 1, 2, 3), 4, 8, false, id, Set.of(), List.of(), 1, List.of(), 0, 0, id, false));
            reopen();
        });
    }

    static Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.SMITHING_TABLE;
        }
    }

    private void reopen() {
        this.stations = List.copyOf(stationManager.getAll());
        open();
    }

}
