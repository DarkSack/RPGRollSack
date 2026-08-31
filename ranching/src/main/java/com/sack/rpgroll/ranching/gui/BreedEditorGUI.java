package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.reskin.EntityReskin;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

public class BreedEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int SPECIES_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int PRODUCTION_SLOT = 13;
    private static final int WEIGHT_SLOT = 14;
    private static final int FERTILITY_SLOT = 15;
    private static final int RESISTANCE_SLOT = 16;
    private static final int TEMPERAMENT_SLOT = 19;

    private static final int RESKIN_MATERIAL_SLOT = 20;
    private static final int RESKIN_CMD_SLOT = 21;
    private static final int RESKIN_SCALE_SLOT = 22;

    private static final int BACK_SLOT = 40;

    private final BreedManager breedManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Breed current;

    public BreedEditorGUI(Player player, Breed breed, BreedManager breedManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.breed.editor.title", "id", breed.id()), NamedTextColor.GOLD), SIZE);
        this.current = breed;
        this.breedManager = breedManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Breed updated) {
        current = updated;
        breedManager.save(current);
        build();
    }

    private void replaceReskin(EntityReskin reskin) {
        replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                current.productionMultiplier(), current.weightMultiplier(), current.fertilityMultiplier(),
                current.resistanceMultiplier(), current.temperament(), reskin));
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

        setItem(SPECIES_SLOT, new ItemBuilder(Material.COW_SPAWN_EGG)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.species_line", "species", current.speciesId()), NamedTextColor.AQUA)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.description_title"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? lang.raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(PRODUCTION_SLOT, new ItemBuilder(Material.BUCKET)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.production_line", "value",
                        String.format(Locale.ROOT, "%.2f", current.productionMultiplier())), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step01_hint"), NamedTextColor.GRAY)).build());

        setItem(WEIGHT_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.weight_line", "value",
                        String.format(Locale.ROOT, "%.2f", current.weightMultiplier())), NamedTextColor.WHITE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step01_hint"), NamedTextColor.GRAY)).build());

        setItem(FERTILITY_SLOT, new ItemBuilder(Material.RABBIT_FOOT)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.fertility_line", "value",
                        String.format(Locale.ROOT, "%.2f", current.fertilityMultiplier())), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step01_hint"), NamedTextColor.GRAY)).build());

        setItem(RESISTANCE_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.resistance_line", "value",
                        String.format(Locale.ROOT, "%.2f", current.resistanceMultiplier())), NamedTextColor.GREEN))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step01_hint"), NamedTextColor.GRAY)).build());

        setItem(TEMPERAMENT_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.temperament_line", "temperament", current.temperament()), NamedTextColor.GOLD)).build());

        EntityReskin reskin = current.reskin();

        setItem(RESKIN_MATERIAL_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.reskin_material_line", "value",
                        reskin.material() != null ? reskin.material() : lang.raw("gui.breed.editor.reskin_none")), NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(lang.raw("gui.breed.editor.reskin_hint")))
                .build());

        setItem(RESKIN_CMD_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.reskin_cmd_line", "value", reskin.customModelData()), NamedTextColor.LIGHT_PURPLE))
                .build());

        setItem(RESKIN_SCALE_SLOT, new ItemBuilder(Material.SLIME_BALL)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.breed.editor.reskin_scale_line", "value",
                        String.format(Locale.ROOT, "%.2f", reskin.scale())), NamedTextColor.LIGHT_PURPLE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step01_hint"), NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -0.1 : 0.1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"), value -> replace(new Breed(current.id(),
                    value, current.description(), current.speciesId(), current.productionMultiplier(),
                    current.weightMultiplier(), current.fertilityMultiplier(), current.resistanceMultiplier(),
                    current.temperament(), current.reskin())));
        } else if (slot == SPECIES_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.breed.editor.prompt_species_id"), value -> replace(new Breed(current.id(),
                    current.displayName(), current.description(), value.trim().toLowerCase(Locale.ROOT),
                    current.productionMultiplier(), current.weightMultiplier(), current.fertilityMultiplier(),
                    current.resistanceMultiplier(), current.temperament(), current.reskin())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"), value -> replace(new Breed(current.id(),
                    current.displayName(), value, current.speciesId(), current.productionMultiplier(),
                    current.weightMultiplier(), current.fertilityMultiplier(), current.resistanceMultiplier(),
                    current.temperament(), current.reskin())));
        } else if (slot == PRODUCTION_SLOT) {
            replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                    Math.max(0.1, current.productionMultiplier() + sign), current.weightMultiplier(),
                    current.fertilityMultiplier(), current.resistanceMultiplier(), current.temperament(), current.reskin()));
        } else if (slot == WEIGHT_SLOT) {
            replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                    current.productionMultiplier(), Math.max(0.1, current.weightMultiplier() + sign),
                    current.fertilityMultiplier(), current.resistanceMultiplier(), current.temperament(), current.reskin()));
        } else if (slot == FERTILITY_SLOT) {
            replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                    current.productionMultiplier(), current.weightMultiplier(),
                    Math.max(0.1, current.fertilityMultiplier() + sign), current.resistanceMultiplier(),
                    current.temperament(), current.reskin()));
        } else if (slot == RESISTANCE_SLOT) {
            replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                    current.productionMultiplier(), current.weightMultiplier(), current.fertilityMultiplier(),
                    Math.max(0.1, current.resistanceMultiplier() + sign), current.temperament(), current.reskin()));
        } else if (slot == TEMPERAMENT_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.breed.editor.prompt_temperament"),
                    value -> replace(new Breed(current.id(), current.displayName(), current.description(),
                            current.speciesId(), current.productionMultiplier(), current.weightMultiplier(),
                            current.fertilityMultiplier(), current.resistanceMultiplier(), value, current.reskin())));
        } else if (slot == RESKIN_MATERIAL_SLOT) {
            if (event.isShiftClick()) {
                EntityReskin r = current.reskin();
                replaceReskin(new EntityReskin(null, r.customModelData(), r.scale(), r.yOffset()));
                return;
            }
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.breed.editor.prompt_reskin_material"), value -> {
                EntityReskin r = current.reskin();
                replaceReskin(new EntityReskin(value.trim().toUpperCase(Locale.ROOT), r.customModelData(), r.scale(), r.yOffset()));
            });
        } else if (slot == RESKIN_CMD_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.breed.editor.prompt_reskin_cmd"), value -> {
                try {
                    EntityReskin r = current.reskin();
                    replaceReskin(new EntityReskin(r.material(), Integer.parseInt(value.trim()), r.scale(), r.yOffset()));
                } catch (NumberFormatException ignored) {
                    // valor inválido, se ignora
                }
            });
        } else if (slot == RESKIN_SCALE_SLOT) {
            EntityReskin r = current.reskin();
            replaceReskin(new EntityReskin(r.material(), r.customModelData(), Math.max(0.1, r.scale() + sign), r.yOffset()));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
