package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.reskin.EntityReskin;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobModel;
import com.sack.rpgroll.mobs.core.MobSkin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Editor de la lista de skins sorteables de un mob (peso + material +
 * custom-model-data cada una) — calcado de
 * {@code items/gui/editor/SkinsEditorGUI.java}, con la salvedad de que acá
 * el custom-model-data sí se pide por chat (es central para el reskin).
 */
public class MobSkinsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final LangManager lang;
    private final Runnable onBack;
    private final List<MobSkin> skins;

    public MobSkinsEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.model.skins.title", "id", session.original.id()),
                SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
        this.onBack = onBack;
        this.skins = new ArrayList<>(session.model.skins());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < skins.size() && i < 36; i++) {

            MobSkin skin = skins.get(i);
            EntityReskin reskin = skin.reskin();
            Material material = reskin.material() != null ? parseMaterial(reskin.material()) : Material.ARMOR_STAND;

            setItem(i, new ItemBuilder(material)
                    .setName(Component.text(skin.id(), NamedTextColor.AQUA))
                    .setLore(lang.component("gui.model.skins.entry_cmd", "value", reskin.customModelData()),
                            lang.component("gui.model.skins.entry_weight", "value", skin.weight()),
                            lang.component("gui.common.shift_remove_hint"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.model.skins.add"))
                .setLore(lang.component("gui.model.skins.prompt_add"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    private Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.ARMOR_STAND;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < skins.size() && slot < 36) {
            if (event.isShiftClick()) {
                skins.remove(slot);
                build();
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == BACK_SLOT) {
            session.model = new MobModel(session.model.baseEntityType(), session.model.scale(), session.model.glow(),
                    session.model.invisible(), session.model.equipment(), session.model.modelEngineId(), skins);
            onBack.run();
        }
    }

    private void promptAdd() {
        session.chatPromptManager.prompt(player, lang.raw("gui.model.skins.prompt_add"), value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length < 2) {
                lang.send(player, "gui.common.invalid_format");
                return;
            }

            String id = parts[0];
            String material = parts[1];
            int customModelData = 0;
            double weight = 1.0;

            try {
                if (parts.length >= 3) {
                    customModelData = Integer.parseInt(parts[2]);
                }
                if (parts.length >= 4) {
                    weight = Double.parseDouble(parts[3]);
                }
            } catch (NumberFormatException e) {
                lang.send(player, "gui.common.invalid_format");
                return;
            }

            skins.add(new MobSkin(id, new EntityReskin(material.toUpperCase(Locale.ROOT), customModelData, 1.0, 0.0),
                    weight));
            build();
        });
    }

}
