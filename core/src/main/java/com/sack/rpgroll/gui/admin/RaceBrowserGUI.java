package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RacePhysicalModifiers;
import com.sack.rpgroll.race.RaceManagerImpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RaceBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final RaceManagerImpl raceManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<Race> races;

    public RaceBrowserGUI(Player player, RaceManagerImpl raceManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("race_browser_gui.title"), SIZE);
        this.raceManager = raceManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.races = List.copyOf(raceManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < races.size() && i < 36; i++) {

            Race race = races.get(i);

            setItem(i, new ItemBuilder(Material.PLAYER_HEAD)
                    .setName(Component.text(race.id(), NamedTextColor.YELLOW))
                    .setLore(lang.component("race_browser_gui.attribute_count", "count", race.baseAttributes().size()),
                            lang.component("race_browser_gui.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("race_browser_gui.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("race_browser_gui.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < races.size() && slot < 36) {
            new RaceEditorGUI(player, races.get(slot), raceManager, chatPromptManager, lang, this::reopen).open();
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
        chatPromptManager.prompt(player, lang.raw("race_browser_gui.prompt_new"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (raceManager.exists(id)) {
                lang.send(player, "race_browser_gui.already_exists");
                reopen();
                return;
            }

            Race race = new Race(id, id, "", Map.of(), List.of(), "", List.of(), RacePhysicalModifiers.none());
            raceManager.save(race);
            reopen();
        });
    }

    private void reopen() {
        this.races = List.copyOf(raceManager.getAll());
        open();
    }

}
