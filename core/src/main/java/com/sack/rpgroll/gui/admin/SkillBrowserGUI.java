package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.gameplay.skill.Skill;
import com.sack.rpgroll.gameplay.skill.SkillManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class SkillBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SkillManager skillManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<Skill> skills;

    public SkillBrowserGUI(Player player, SkillManager skillManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("skill_browser_gui.title"), SIZE);
        this.skillManager = skillManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.skills = List.copyOf(skillManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < skills.size() && i < 36; i++) {

            Skill skill = skills.get(i);

            setItem(i, new ItemBuilder(Material.BLAZE_POWDER)
                    .setName(Component.text(skill.id(), NamedTextColor.YELLOW))
                    .setLore(lang.component("skill_browser_gui.level", "level", skill.requiredLevel()),
                            lang.component("skill_browser_gui.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("skill_browser_gui.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("skill_browser_gui.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < skills.size() && slot < 36) {
            new SkillEditorGUI(player, skills.get(slot), skillManager, chatPromptManager, lang, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, lang.raw("skill_browser_gui.prompt_new"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (skillManager.exists(id)) {
                lang.send(player, "skill_browser_gui.already_exists");
                reopen();
                return;
            }

            Skill skill = new Skill(id, id, "", 1, 0, 0, 1.0);
            skillManager.save(skill);
            reopen();
        });
    }

    private void reopen() {
        this.skills = List.copyOf(skillManager.getAll());
        open();
    }

}
