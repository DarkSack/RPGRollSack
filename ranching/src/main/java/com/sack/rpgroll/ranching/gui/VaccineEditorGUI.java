package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.Vaccine;
import com.sack.rpgroll.ranching.core.health.VaccineManager;
import com.sack.rpgroll.ranching.item.RanchingItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class VaccineEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int DISEASES_SLOT = 13;
    private static final int RISK_REDUCTION_SLOT = 14;
    private static final int DURATION_SLOT = 15;
    private static final int GIVE_SLOT = 16;
    private static final int BACK_SLOT = 40;

    private final VaccineManager vaccineManager;
    private final DiseaseManager diseaseManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Vaccine current;

    public VaccineEditorGUI(Player player, Vaccine vaccine, VaccineManager vaccineManager,
            DiseaseManager diseaseManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.vaccine.editor.title", "id", vaccine.id()), NamedTextColor.GOLD), SIZE);
        this.current = vaccine;
        this.vaccineManager = vaccineManager;
        this.diseaseManager = diseaseManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Vaccine updated) {
        current = updated;
        vaccineManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        var lang = chatPromptManager.lang();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.editor.name_line", "name", current.displayName())).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon(), Material.POTION))
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.icon_line", "icon", current.icon()), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.description_title"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? lang.raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(DISEASES_SLOT, new ItemBuilder(Material.ROTTEN_FLESH)
                .setName(ComponentUtils.parseWithDefault(lang.raw("item.vaccine.prevents", "diseases", String.join(", ", current.preventsDiseaseIds())), NamedTextColor.GREEN))
                .setLore(ItemBuilder.toLoreLines(lang.raw("gui.editor.known_diseases", "diseases",
                        String.join(", ", diseaseManager.getAll().stream().map(d -> d.id()).toList()))
                        + "\n" + lang.raw("gui.editor.prompt_disease_ids"))).build());

        setItem(RISK_REDUCTION_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.vaccine.editor.risk_reduction_line", "value", String.format(Locale.ROOT, "%.0f", current.riskReduction() * 100)), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step10pct_hint"), NamedTextColor.GRAY)).build());

        setItem(DURATION_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(ComponentUtils.parseWithDefault(current.isPermanent() ? lang.raw("gui.vaccine.editor.duration_permanent")
                        : lang.raw("gui.vaccine.editor.duration_line", "ticks", current.immunityDurationTicks()), NamedTextColor.WHITE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.vaccine.editor.duration_hint"), NamedTextColor.GRAY))
                .build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.give_one_female"), NamedTextColor.GREEN)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"), value -> replace(new Vaccine(current.id(),
                    value, current.icon(), current.description(), current.preventsDiseaseIds(), current.riskReduction(),
                    current.immunityDurationTicks())));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_icon"), value -> replace(new Vaccine(
                    current.id(), current.displayName(), value, current.description(), current.preventsDiseaseIds(),
                    current.riskReduction(), current.immunityDurationTicks())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"), value -> replace(new Vaccine(
                    current.id(), current.displayName(), current.icon(), value, current.preventsDiseaseIds(),
                    current.riskReduction(), current.immunityDurationTicks())));
        } else if (slot == DISEASES_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_disease_ids"), value -> {

                Set<String> diseases = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        diseases.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new Vaccine(current.id(), current.displayName(), current.icon(), current.description(),
                        diseases, current.riskReduction(), current.immunityDurationTicks()));
            });
        } else if (slot == RISK_REDUCTION_SLOT) {
            replace(new Vaccine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.preventsDiseaseIds(), Math.max(0, Math.min(1, current.riskReduction() + sign * 0.1)),
                    current.immunityDurationTicks()));
        } else if (slot == DURATION_SLOT) {
            replace(new Vaccine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.preventsDiseaseIds(), current.riskReduction(),
                    Math.max(0, current.immunityDurationTicks() + sign * 1200L)));
        } else if (slot == GIVE_SLOT) {
            player.getInventory().addItem(RanchingItemFactory.createVaccine(chatPromptManager.lang(), current));
            player.sendMessage(chatPromptManager.lang().component("gui.vaccine.editor.gave_self", "name", current.displayName()));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
