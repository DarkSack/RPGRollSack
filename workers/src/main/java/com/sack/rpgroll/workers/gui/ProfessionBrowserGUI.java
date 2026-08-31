package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.economy.WageType;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfessionBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ProfessionManager professionManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Profession> professions;

    public ProfessionBrowserGUI(Player player, ProfessionManager professionManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.browser.title"), NamedTextColor.GOLD), SIZE);
        this.professionManager = professionManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.professions = List.copyOf(professionManager.getAll());
    }

    static Material parseMaterial(String raw, Material fallback) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < professions.size() && i < 36; i++) {

            Profession profession = professions.get(i);

            setItem(i, new ItemBuilder(parseMaterial(profession.icon(), Material.VILLAGER_SPAWN_EGG))
                    .setName(ComponentUtils.parse(profession.displayName()))
                    .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.browser.id_label", "id",
                            profession.id()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.browser.entity_label", "entity",
                                    profession.entityType()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.browser.ai_rules_label", "count",
                                    profession.aiRules().size()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.browser.new"), NamedTextColor.GREEN))
                .build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < professions.size() && slot < 36) {
            new ProfessionEditorGUI(player, professions.get(slot), professionManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (professionManager.exists(id)) {
                player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.browser.duplicate_id"), NamedTextColor.RED));
                reopen();
                return;
            }

            professionManager.save(new Profession(id, id, "VILLAGER_SPAWN_EGG", "", "VILLAGER", Set.of(), List.of(),
                    null, 0, WageType.PER_TASK, null, com.sack.rpgroll.common.reskin.EntityReskin.NONE));
            reopen();
        });
    }

    private void reopen() {
        this.professions = List.copyOf(professionManager.getAll());
        open();
    }

}
