package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.Medicine;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.MedicineType;
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

public class MedicineEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 9;
    private static final int ICON_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 11;
    private static final int TYPE_SLOT = 12;
    private static final int DISEASES_SLOT = 13;
    private static final int CURE_CHANCE_SLOT = 14;
    private static final int RECOVERY_BOOST_SLOT = 15;
    private static final int HEALTH_SLOT = 16;
    private static final int HAPPINESS_SLOT = 19;
    private static final int GIVE_SLOT = 20;
    private static final int BACK_SLOT = 40;

    private final MedicineManager medicineManager;
    private final DiseaseManager diseaseManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Medicine current;

    public MedicineEditorGUI(Player player, Medicine medicine, MedicineManager medicineManager,
            DiseaseManager diseaseManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.medicine.editor.title", "id", medicine.id()), NamedTextColor.GOLD), SIZE);
        this.current = medicine;
        this.medicineManager = medicineManager;
        this.diseaseManager = diseaseManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Medicine updated) {
        current = updated;
        medicineManager.save(current);
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
                .setName(Component.text(lang.raw("gui.editor.name_line", "name", current.displayName()), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon(), Material.POTION))
                .setName(Component.text(lang.raw("gui.editor.icon_line", "icon", current.icon()), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text(lang.raw("gui.editor.description_title"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? lang.raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.BREWING_STAND)
                .setName(Component.text(lang.raw("item.medicine.type", "type", current.type()), NamedTextColor.AQUA))
                .setLore(Component.text(lang.raw("gui.editor.rotate_hint"), NamedTextColor.GRAY)).build());

        setItem(DISEASES_SLOT, new ItemBuilder(Material.ROTTEN_FLESH)
                .setName(Component.text(lang.raw("item.medicine.treats", "diseases", String.join(", ", current.curesDiseaseIds())), NamedTextColor.GREEN))
                .setLore(ItemBuilder.toLoreLines(lang.raw("gui.editor.known_diseases", "diseases",
                        String.join(", ", diseaseManager.getAll().stream().map(d -> d.id()).toList()))
                        + "\n" + lang.raw("gui.editor.prompt_disease_ids"))).build());

        setItem(CURE_CHANCE_SLOT, new ItemBuilder(Material.GOLDEN_CARROT)
                .setName(Component.text(lang.raw("gui.medicine.editor.cure_chance_line", "value", String.format(Locale.ROOT, "%.0f", current.cureChance() * 100)),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(lang.raw("gui.editor.step10pct_hint"), NamedTextColor.GRAY)).build());

        setItem(RECOVERY_BOOST_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text(lang.raw("gui.medicine.editor.recovery_boost_line", "ticks", current.recoveryBoostTicks()), NamedTextColor.WHITE))
                .setLore(Component.text(lang.raw("gui.medicine.editor.recovery_boost_hint"), NamedTextColor.GRAY)).build());

        setItem(HEALTH_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(Component.text(lang.raw("gui.editor.health_bonus_line", "value", current.healthBonus()), NamedTextColor.RED))
                .setLore(Component.text(lang.raw("gui.editor.step2_hint"), NamedTextColor.GRAY)).build());

        setItem(HAPPINESS_SLOT, new ItemBuilder(Material.CAKE)
                .setName(Component.text(lang.raw("gui.editor.happiness_bonus_line", "value", current.happinessBonus()), NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(lang.raw("gui.editor.step2_hint"), NamedTextColor.GRAY)).build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text(lang.raw("gui.editor.give_one_female"), NamedTextColor.GREEN)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -2 : 2;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"), value -> replace(new Medicine(current.id(),
                    value, current.icon(), current.description(), current.type(), current.curesDiseaseIds(),
                    current.cureChance(), current.recoveryBoostTicks(), current.healthBonus(), current.happinessBonus())));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_icon"), value -> replace(new Medicine(
                    current.id(), current.displayName(), value, current.description(), current.type(),
                    current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(), current.healthBonus(),
                    current.happinessBonus())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"), value -> replace(new Medicine(
                    current.id(), current.displayName(), current.icon(), value, current.type(),
                    current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(), current.healthBonus(),
                    current.happinessBonus())));
        } else if (slot == TYPE_SLOT) {
            MedicineType[] values = MedicineType.values();
            MedicineType next = values[(current.type().ordinal() + 1) % values.length];
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(), next,
                    current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(), current.healthBonus(),
                    current.happinessBonus()));
        } else if (slot == DISEASES_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_disease_ids"), value -> {

                Set<String> diseases = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        diseases.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                        current.type(), diseases, current.cureChance(), current.recoveryBoostTicks(),
                        current.healthBonus(), current.happinessBonus()));
            });
        } else if (slot == CURE_CHANCE_SLOT) {
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.type(), current.curesDiseaseIds(), Math.max(0, Math.min(1, current.cureChance() + sign * 0.05)),
                    current.recoveryBoostTicks(), current.healthBonus(), current.happinessBonus()));
        } else if (slot == RECOVERY_BOOST_SLOT) {
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.type(), current.curesDiseaseIds(), current.cureChance(),
                    Math.max(0, current.recoveryBoostTicks() + (long) sign * 300), current.healthBonus(),
                    current.happinessBonus()));
        } else if (slot == HEALTH_SLOT) {
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.type(), current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(),
                    Math.max(0, current.healthBonus() + sign), current.happinessBonus()));
        } else if (slot == HAPPINESS_SLOT) {
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.type(), current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(),
                    current.healthBonus(), Math.max(0, current.happinessBonus() + sign)));
        } else if (slot == GIVE_SLOT) {
            player.getInventory().addItem(RanchingItemFactory.createMedicine(chatPromptManager.lang(), current));
            player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.medicine.editor.gave_self", "name", current.displayName()), NamedTextColor.GREEN));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
