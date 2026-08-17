package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.PrestigeLevel;
import com.sack.rpgroll.ascension.core.PrestigeManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class PrestigeEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int LEVEL_SLOT = 10;
    private static final int BONUS_SLOT = 12;
    private static final int SKILLS_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final PrestigeManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private PrestigeLevel current;

    public PrestigeEditorGUI(Player player, PrestigeLevel level, PrestigeManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack, LangManager lang) {
        super(player, lang.component("gui.prestige.editor_title", "id", level.id()), SIZE);
        this.current = level;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    private void replace(PrestigeLevel updated) {
        current = updated;
        manager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("gui.prestige.level_label", "level", current.requiredLevel()))
                .setLore(lang.component("gui.prestige.click_plusminus10"))
                .build());

        setItem(BONUS_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.prestige.bonus_label", "percent", current.expBonusPercent()))
                .setLore(lang.component("gui.common.click_plusminus1"))
                .build());

        setItem(SKILLS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("gui.prestige.skills_label", "count", current.grantedSkills().size()))
                .setLore(Component.text(String.join(", ", current.grantedSkills()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -10 : 10;
            replace(new PrestigeLevel(current.id(), Math.max(1, current.requiredLevel() + delta),
                    current.expBonusPercent(), current.grantedSkills()));
            return;
        }

        if (slot == BONUS_SLOT) {
            double delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new PrestigeLevel(current.id(), current.requiredLevel(),
                    Math.max(0, current.expBonusPercent() + delta), current.grantedSkills()));
            return;
        }

        if (slot == SKILLS_SLOT) {
            chatPromptManager.prompt(player, "gui.prestige.prompt_skills",
                    value -> replace(new PrestigeLevel(current.id(), current.requiredLevel(),
                            current.expBonusPercent(), List.of(value.trim().split(",")))));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
