package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.Vaccine;
import com.sack.rpgroll.ranching.core.health.VaccineManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class VaccineBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final VaccineManager vaccineManager;
    private final DiseaseManager diseaseManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Vaccine> vaccines;

    public VaccineBrowserGUI(Player player, VaccineManager vaccineManager, DiseaseManager diseaseManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.hub.vaccines"), NamedTextColor.GOLD), SIZE);
        this.vaccineManager = vaccineManager;
        this.diseaseManager = diseaseManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.vaccines = List.copyOf(vaccineManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < vaccines.size() && i < 36; i++) {

            Vaccine vaccine = vaccines.get(i);

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(vaccine.icon(), Material.POTION))
                    .setName(ComponentUtils.parse(vaccine.displayName()))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.browser.id_line", "id", vaccine.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.vaccine.browser.new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < vaccines.size() && slot < 36) {
            new VaccineEditorGUI(player, vaccines.get(slot), vaccineManager, diseaseManager, chatPromptManager,
                    this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.vaccine.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (vaccineManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.vaccine.browser.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            vaccineManager.save(new Vaccine(id, id, "POTION", "", Set.of(), 0.8, 0));
            reopen();
        });
    }

    private void reopen() {
        this.vaccines = List.copyOf(vaccineManager.getAll());
        open();
    }

}
