package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.skill.Skill;
import com.sack.rpgroll.workers.core.skill.SkillManager;
import com.sack.rpgroll.util.ComponentUtils;

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
    private final ProfessionManager professionManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Skill> skills;

    public SkillBrowserGUI(Player player, SkillManager skillManager, ProfessionManager professionManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.skill.browser.title"), NamedTextColor.GOLD), SIZE);
        this.skillManager = skillManager;
        this.professionManager = professionManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
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

            setItem(i, new ItemBuilder(Material.BOOK)
                    .setName(ComponentUtils.parse(skill.displayName()))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.profession.browser.id_label", "id",
                            skill.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.skill.browser.profession_label", "profession",
                                    skill.professionId()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.skill.browser.new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < skills.size() && slot < 36) {
            new SkillEditorGUI(player, skills.get(slot), skillManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.skill.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (skillManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.skill.browser.duplicate_id"),
                        NamedTextColor.RED));
                reopen();
                return;
            }

            if (professionManager.getAll().isEmpty()) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.skill.browser.need_profession"),
                        NamedTextColor.RED));
                reopen();
                return;
            }

            String professionId = professionManager.getAll().iterator().next().id();
            skillManager.save(new Skill(id, id, "", professionId, 10, id, 1.0));
            reopen();
        });
    }

    private void reopen() {
        this.skills = List.copyOf(skillManager.getAll());
        open();
    }

}
