package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.CatalystManager;
import com.sack.rpgroll.magic.core.SpellCatalyst;
import com.sack.rpgroll.magic.item.MagicItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

public class CatalystEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int MATERIAL_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int POWER_SLOT = 13;
    private static final int COST_SLOT = 14;
    private static final int RANGE_SLOT = 15;
    private static final int GIVE_SLOT = 22;
    private static final int BACK_SLOT = 40;

    private final CatalystManager catalystManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private SpellCatalyst current;

    public CatalystEditorGUI(Player player, SpellCatalyst catalyst, CatalystManager catalystManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.catalyst_editor.title", "id", catalyst.id()), SIZE);
        this.current = catalyst;
        this.catalystManager = catalystManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(SpellCatalyst updated) {
        current = updated;
        catalystManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.common.name_label", "name", current.displayName())).build());

        setItem(MATERIAL_SLOT, new ItemBuilder(SchoolBrowserGUI.parseMaterial(current.material()))
                .setName(lang.component("gui.catalyst_editor.material_label", "material", current.material())).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description")
                                : current.description()))
                .build());

        setItem(POWER_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(lang.component("gui.catalyst_editor.power_label",
                        "value", String.format(Locale.ROOT, "%.2f", current.powerMultiplier())))
                .setLore(lang.component("gui.common.step_01")).build());

        setItem(COST_SLOT, new ItemBuilder(Material.LAPIS_LAZULI)
                .setName(lang.component("gui.catalyst_editor.mana_cost_label",
                        "value", String.format(Locale.ROOT, "%.2f", current.costMultiplier())))
                .setLore(lang.component("gui.common.step_01")).build());

        setItem(RANGE_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(lang.component("gui.catalyst_editor.range_label",
                        "value", String.format(Locale.ROOT, "%.2f", current.rangeMultiplier())))
                .setLore(lang.component("gui.common.step_01")).build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(lang.component("gui.catalyst_editor.give"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        double sign = click == ClickType.RIGHT ? -0.1 : 0.1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_name"), value -> replace(new SpellCatalyst(
                    current.id(), value, current.material(), current.description(), current.powerMultiplier(),
                    current.costMultiplier(), current.rangeMultiplier())));
            return;
        }

        if (slot == MATERIAL_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.catalyst_editor.prompt_material"), value -> replace(new SpellCatalyst(current.id(),
                    current.displayName(), value, current.description(), current.powerMultiplier(),
                    current.costMultiplier(), current.rangeMultiplier())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_description"), value -> replace(new SpellCatalyst(
                    current.id(), current.displayName(), current.material(), value, current.powerMultiplier(),
                    current.costMultiplier(), current.rangeMultiplier())));
            return;
        }

        if (slot == POWER_SLOT) {
            replace(new SpellCatalyst(current.id(), current.displayName(), current.material(),
                    current.description(), Math.max(0.1, current.powerMultiplier() + sign), current.costMultiplier(),
                    current.rangeMultiplier()));
            return;
        }

        if (slot == COST_SLOT) {
            replace(new SpellCatalyst(current.id(), current.displayName(), current.material(),
                    current.description(), current.powerMultiplier(), Math.max(0.1, current.costMultiplier() + sign),
                    current.rangeMultiplier()));
            return;
        }

        if (slot == RANGE_SLOT) {
            replace(new SpellCatalyst(current.id(), current.displayName(), current.material(),
                    current.description(), current.powerMultiplier(), current.costMultiplier(),
                    Math.max(0.1, current.rangeMultiplier() + sign)));
            return;
        }

        if (slot == GIVE_SLOT) {
            player.getInventory().addItem(MagicItemFactory.createCatalyst(current, lang));
            lang.send(player, "gui.catalyst_editor.given", "name", current.displayName());
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
