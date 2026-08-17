package com.sack.rpgroll.crates.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.crates.core.Crate;
import com.sack.rpgroll.crates.core.CrateAction;
import com.sack.rpgroll.crates.core.CrateManager;
import com.sack.rpgroll.crates.core.CrateReward;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Editor de un crate — identidad/llave/holograma + su tabla de recompensas (agregar/quitar). */
public class CrateEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 0;
    private static final int REQUIRE_KEY_SLOT = 1;
    private static final int KEY_MATERIAL_SLOT = 2;
    private static final int HOLOGRAM_SLOT = 3;

    private static final int REWARDS_ROW = 9;
    private static final int ADD_REWARD_SLOT = 44;
    private static final int BACK_SLOT = 53;

    private final CrateManager crateManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Crate current;

    public CrateEditorGUI(Player player, Crate crate, CrateManager crateManager, ChatPromptManager chatPromptManager,
            Runnable onBack, LangManager lang) {
        super(player, lang.component("editor.title", "id", crate.id()), SIZE);
        this.current = crate;
        this.crateManager = crateManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.onBack = onBack;
    }

    private void replace(Crate updated) {
        current = updated;
        crateManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("editor.name_label", "name", current.displayName())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("editor.click_to_rename"))
                .build());

        setItem(REQUIRE_KEY_SLOT, new ItemBuilder(current.requireKey() ? Material.TRIPWIRE_HOOK : Material.BARRIER)
                .setName(lang.component(current.requireKey() ? "editor.require_key_yes" : "editor.require_key_no"))
                .setLore(lang.component("editor.click_to_toggle"))
                .build());

        setItem(KEY_MATERIAL_SLOT, new ItemBuilder(current.keyMaterial())
                .setName(lang.component("editor.key_material_label", "material", current.keyMaterial()))
                .setLore(lang.component("editor.click_to_set_material"))
                .build());

        setItem(HOLOGRAM_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("editor.hologram_label", "count", current.hologramLines().size()))
                .setLore(lang.component("editor.click_to_set_hologram"))
                .build());

        List<CrateReward> rewards = current.rewards();
        for (int i = 0; i < rewards.size() && i < 36; i++) {

            CrateReward reward = rewards.get(i);

            setItem(REWARDS_ROW + i, new ItemBuilder(reward.icon())
                    .setName(ComponentUtils.parse(reward.displayName())
                            .colorIfAbsent(NamedTextColor.WHITE))
                    .setLore(lang.component("editor.reward_weight_lore",
                            "weight", reward.weight(), "actions", reward.actions().size()),
                            lang.component("editor.shift_click_remove"))
                    .build());
        }

        setItem(ADD_REWARD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.add_reward"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "editor.prompt_name",
                    value -> replace(new Crate(current.id(), value, current.guiTitle(), current.requireKey(),
                            current.keyMaterial(), current.keyDisplayName(), current.keyLore(),
                            current.hologramLines(), current.rewards())));
            return;
        }

        if (slot == REQUIRE_KEY_SLOT) {
            replace(new Crate(current.id(), current.displayName(), current.guiTitle(), !current.requireKey(),
                    current.keyMaterial(), current.keyDisplayName(), current.keyLore(), current.hologramLines(),
                    current.rewards()));
            return;
        }

        if (slot == KEY_MATERIAL_SLOT) {
            chatPromptManager.prompt(player, "editor.prompt_key_material", value -> {
                Material material;
                try {
                    material = Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    lang.send(player, "editor.invalid_material");
                    return;
                }
                replace(new Crate(current.id(), current.displayName(), current.guiTitle(), current.requireKey(),
                        material, current.keyDisplayName(), current.keyLore(), current.hologramLines(),
                        current.rewards()));
            });
            return;
        }

        if (slot == HOLOGRAM_SLOT) {
            chatPromptManager.prompt(player, "editor.prompt_hologram", value -> {
                List<String> lines = value.isBlank() ? List.of() : List.of(value.split("\\s*;\\s*"));
                replace(new Crate(current.id(), current.displayName(), current.guiTitle(), current.requireKey(),
                        current.keyMaterial(), current.keyDisplayName(), current.keyLore(), lines,
                        current.rewards()));
            });
            return;
        }

        if (slot >= REWARDS_ROW && slot < REWARDS_ROW + Math.min(current.rewards().size(), 36)) {

            if (event.isShiftClick()) {

                if (current.rewards().size() <= 1) {
                    lang.send(player, "editor.reward_min_required");
                    return;
                }

                List<CrateReward> updated = new ArrayList<>(current.rewards());
                updated.remove(slot - REWARDS_ROW);
                replace(new Crate(current.id(), current.displayName(), current.guiTitle(), current.requireKey(),
                        current.keyMaterial(), current.keyDisplayName(), current.keyLore(), current.hologramLines(),
                        updated));
            }
            return;
        }

        if (slot == ADD_REWARD_SLOT) {
            promptAddReward();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAddReward() {
        chatPromptManager.prompt(player, "editor.prompt_add_reward",
                value -> {

                    String[] parts = value.split(";");

                    if (parts.length < 5) {
                        lang.send(player, "editor.invalid_format");
                        return;
                    }

                    String id = parts[0].trim();
                    String name = parts[1].trim();

                    Material icon;
                    try {
                        icon = Material.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        lang.send(player, "editor.invalid_icon_material");
                        return;
                    }

                    double weight;
                    try {
                        weight = Double.parseDouble(parts[3].trim());
                    } catch (NumberFormatException e) {
                        lang.send(player, "editor.invalid_weight");
                        return;
                    }

                    List<CrateAction> actions = List.of(
                            new CrateAction(CrateAction.CrateActionType.GIVE_ITEM, parts[4].trim()),
                            new CrateAction(CrateAction.CrateActionType.MESSAGE, "&aGanaste " + name + "&a."));

                    List<CrateReward> updated = new ArrayList<>(current.rewards());
                    updated.add(new CrateReward(id, name, icon, List.of(), weight, false, actions));

                    replace(new Crate(current.id(), current.displayName(), current.guiTitle(), current.requireKey(),
                            current.keyMaterial(), current.keyDisplayName(), current.keyLore(),
                            current.hologramLines(), updated));
                });
    }

}
