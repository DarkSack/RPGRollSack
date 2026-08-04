package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobModel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Editor de apariencia: tipo base, escala, brillo, invisibilidad, ModelEngine id y equipo visible. */
public class ModelEditorGUI extends InventoryGUI {

    private static final int SIZE = 36;

    private static final int BASE_TYPE_SLOT = 10;
    private static final int SCALE_SLOT = 11;
    private static final int GLOW_SLOT = 12;
    private static final int INVISIBLE_SLOT = 13;
    private static final int MODEL_ENGINE_SLOT = 14;

    private static final int EQUIPMENT_START_SLOT = 18;
    private static final String[] EQUIPMENT_SLOTS = {"HAND", "OFFHAND", "HEAD", "CHEST", "LEGS", "FEET"};

    private static final int BACK_SLOT = 31;

    private final MobEditorSession session;
    private final Runnable onBack;

    public ModelEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Modelo: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        MobModel model = session.model;

        setItem(BASE_TYPE_SLOT, new ItemBuilder(Material.SPAWNER)
                .setName(Component.text("Tipo base: " + model.baseEntityType(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un EntityType vanilla", NamedTextColor.GRAY))
                .build());

        setItem(SCALE_SLOT, new ItemBuilder(Material.SLIME_BALL)
                .setName(Component.text("Escala: " + model.scale(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.1 · Shift-click: +1", NamedTextColor.GRAY),
                        Component.text("Click derecho: -0.1 · Shift-click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(GLOW_SLOT, new ItemBuilder(model.glow() ? Material.GLOWSTONE_DUST : Material.GUNPOWDER)
                .setName(Component.text("Brillo: " + model.glow(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(INVISIBLE_SLOT, new ItemBuilder(model.invisible() ? Material.GLASS : Material.STONE)
                .setName(Component.text("Invisible: " + model.invisible(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(MODEL_ENGINE_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(Component.text("ModelEngine id: "
                        + (model.modelEngineId() != null ? model.modelEngineId() : "(ninguno)"),
                        NamedTextColor.YELLOW))
                .setLore(
                        Component.text("Punto de extensión — requiere el plugin ModelEngine/BetterModel",
                                NamedTextColor.DARK_GRAY),
                        Component.text("Click: escribir · Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {

            String key = EQUIPMENT_SLOTS[i];
            String reference = model.equipment().get(key);

            setItem(EQUIPMENT_START_SLOT + i, new ItemBuilder(reference != null ? Material.IRON_CHESTPLATE
                    : Material.BARRIER)
                    .setName(Component.text(key + ": " + (reference != null ? reference : "(vacío)"),
                            NamedTextColor.YELLOW))
                    .setLore(Component.text("Click: escribir referencia (item de Items o Material)",
                            NamedTextColor.GRAY),
                            Component.text("Shift-click: quitar", NamedTextColor.GRAY))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == BASE_TYPE_SLOT) {
            session.chatPromptManager.prompt(player, "Escribí el EntityType vanilla (ej. ZOMBIE, WITHER_SKELETON):",
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
            session.chatPromptManager.prompt(player, "Escribí el id de modelo (ModelEngine/BetterModel):", value -> {
                session.model = withModelEngineId(session.model, value.trim());
                build();
            });
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

            session.chatPromptManager.prompt(player, "Escribí la referencia de ítem para " + key + ":", value -> {
                Map<String, String> equipment = new LinkedHashMap<>(session.model.equipment());
                equipment.put(key, value.trim());
                session.model = withEquipment(session.model, equipment);
                build();
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private MobModel withBaseType(MobModel model, String baseType) {
        return new MobModel(baseType, model.scale(), model.glow(), model.invisible(), model.equipment(),
                model.modelEngineId());
    }

    private MobModel withScale(MobModel model, double scale) {
        return new MobModel(model.baseEntityType(), scale, model.glow(), model.invisible(), model.equipment(),
                model.modelEngineId());
    }

    private MobModel withGlow(MobModel model, boolean glow) {
        return new MobModel(model.baseEntityType(), model.scale(), glow, model.invisible(), model.equipment(),
                model.modelEngineId());
    }

    private MobModel withInvisible(MobModel model, boolean invisible) {
        return new MobModel(model.baseEntityType(), model.scale(), model.glow(), invisible, model.equipment(),
                model.modelEngineId());
    }

    private MobModel withModelEngineId(MobModel model, String modelEngineId) {
        return new MobModel(model.baseEntityType(), model.scale(), model.glow(), model.invisible(),
                model.equipment(), modelEngineId);
    }

    private MobModel withEquipment(MobModel model, Map<String, String> equipment) {
        return new MobModel(model.baseEntityType(), model.scale(), model.glow(), model.invisible(), equipment,
                model.modelEngineId());
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
