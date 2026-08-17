package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobModel;
import com.sack.rpgroll.mobs.core.MobSkin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Editor de apariencia: tipo base, escala, brillo, invisibilidad, ModelEngine id, equipo visible y skins. */
public class ModelEditorGUI extends InventoryGUI {

    private static final int SIZE = 36;

    private static final int BASE_TYPE_SLOT = 10;
    private static final int SCALE_SLOT = 11;
    private static final int GLOW_SLOT = 12;
    private static final int INVISIBLE_SLOT = 13;
    private static final int MODEL_ENGINE_SLOT = 14;

    private static final int SKINS_SLOT = 15;

    private static final int EQUIPMENT_START_SLOT = 18;
    private static final String[] EQUIPMENT_SLOTS = {"HAND", "OFFHAND", "HEAD", "CHEST", "LEGS", "FEET"};

    private static final int BACK_SLOT = 31;

    private final MobEditorSession session;
    private final Runnable onBack;
    private final LangManager lang;

    public ModelEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.model.title", "id", session.original.id()),
                SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        MobModel model = session.model;

        setItem(BASE_TYPE_SLOT, new ItemBuilder(Material.SPAWNER)
                .setName(lang.component("gui.model.base_type_label", "value", model.baseEntityType()))
                .setLore(lang.component("gui.model.base_type_hint"))
                .build());

        setItem(SCALE_SLOT, new ItemBuilder(Material.SLIME_BALL)
                .setName(lang.component("gui.model.scale_label", "value", model.scale()))
                .setLore(lang.component("gui.model.scale_hint1"),
                        lang.component("gui.model.scale_hint2"))
                .build());

        setItem(GLOW_SLOT, new ItemBuilder(model.glow() ? Material.GLOWSTONE_DUST : Material.GUNPOWDER)
                .setName(lang.component("gui.model.glow_label", "value", model.glow()))
                .setLore(lang.component("gui.common.click_toggle"))
                .build());

        setItem(INVISIBLE_SLOT, new ItemBuilder(model.invisible() ? Material.GLASS : Material.STONE)
                .setName(lang.component("gui.model.invisible_label", "value", model.invisible()))
                .setLore(lang.component("gui.common.click_toggle"))
                .build());

        setItem(MODEL_ENGINE_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(lang.component("gui.model.modelengine_label", "value",
                        model.modelEngineId() != null ? model.modelEngineId() : lang.raw("gui.common.none_label")))
                .setLore(
                        lang.component("gui.model.modelengine_note"),
                        lang.component("gui.common.click_write_shift_remove"))
                .build());

        setItem(SKINS_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(lang.component("gui.model.skins_label", "count", model.skins().size()))
                .setLore(lang.component("gui.model.skins_hint"))
                .build());

        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {

            String key = EQUIPMENT_SLOTS[i];
            String reference = model.equipment().get(key);

            setItem(EQUIPMENT_START_SLOT + i, new ItemBuilder(reference != null ? Material.IRON_CHESTPLATE
                    : Material.BARRIER)
                    .setName(lang.component("gui.model.equipment_label", "slot", key, "value",
                            reference != null ? reference : lang.raw("gui.model.empty_label")))
                    .setLore(lang.component("gui.model.equipment_hint"),
                            lang.component("gui.common.shift_remove_hint"))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == BASE_TYPE_SLOT) {
            session.chatPromptManager.prompt(player, "gui.model.prompt_base_type",
                    value -> {
                        session.model = withBaseType(session.model, value.trim().toUpperCase(Locale.ROOT));
                        build();
                    });
            return;
        }

        if (slot == SCALE_SLOT) {
            double newScale = Math.max(0.1, session.model.scale() + delta(event.getClick()) / 10.0);
            session.model = withScale(session.model, newScale);
            build();
            return;
        }

        if (slot == GLOW_SLOT) {
            session.model = withGlow(session.model, !session.model.glow());
            build();
            return;
        }

        if (slot == INVISIBLE_SLOT) {
            session.model = withInvisible(session.model, !session.model.invisible());
            build();
            return;
        }

        if (slot == MODEL_ENGINE_SLOT) {
            if (event.isShiftClick()) {
                session.model = withModelEngineId(session.model, null);
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "gui.model.prompt_modelengine", value -> {
                session.model = withModelEngineId(session.model, value.trim());
                build();
            });
            return;
        }

        if (slot == SKINS_SLOT) {
            new MobSkinsEditorGUI(player, session, this::reopen).open();
            return;
        }

        if (slot >= EQUIPMENT_START_SLOT && slot < EQUIPMENT_START_SLOT + EQUIPMENT_SLOTS.length) {

            String key = EQUIPMENT_SLOTS[slot - EQUIPMENT_START_SLOT];

            if (event.isShiftClick()) {
                Map<String, String> equipment = new LinkedHashMap<>(session.model.equipment());
                equipment.remove(key);
                session.model = withEquipment(session.model, equipment);
                build();
                return;
            }

            session.chatPromptManager.prompt(player, "gui.model.prompt_equipment", value -> {
                Map<String, String> equipment = new LinkedHashMap<>(session.model.equipment());
                equipment.put(key, value.trim());
                session.model = withEquipment(session.model, equipment);
                build();
            }, "slot", key);
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        new ModelEditorGUI(player, session, onBack).open();
    }

    private MobModel withBaseType(MobModel model, String baseType) {
        return new MobModel(baseType, model.scale(), model.glow(), model.invisible(), model.equipment(),
                model.modelEngineId(), model.skins());
    }

    private MobModel withScale(MobModel model, double scale) {
        return new MobModel(model.baseEntityType(), scale, model.glow(), model.invisible(), model.equipment(),
                model.modelEngineId(), model.skins());
    }

    private MobModel withGlow(MobModel model, boolean glow) {
        return new MobModel(model.baseEntityType(), model.scale(), glow, model.invisible(), model.equipment(),
                model.modelEngineId(), model.skins());
    }

    private MobModel withInvisible(MobModel model, boolean invisible) {
        return new MobModel(model.baseEntityType(), model.scale(), model.glow(), invisible, model.equipment(),
                model.modelEngineId(), model.skins());
    }

    private MobModel withModelEngineId(MobModel model, String modelEngineId) {
        return new MobModel(model.baseEntityType(), model.scale(), model.glow(), model.invisible(),
                model.equipment(), modelEngineId, model.skins());
    }

    private MobModel withEquipment(MobModel model, Map<String, String> equipment) {
        return new MobModel(model.baseEntityType(), model.scale(), model.glow(), model.invisible(), equipment,
                model.modelEngineId(), model.skins());
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 1;
            case SHIFT_LEFT -> 10;
            case RIGHT -> -1;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };
    }

}
