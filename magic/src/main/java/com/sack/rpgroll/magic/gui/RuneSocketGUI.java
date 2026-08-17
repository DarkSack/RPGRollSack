package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Rune;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Runas adjuntas a UN hechizo aprendido — tope {@link PlayerSpellbook#MAX_RUNES_PER_SPELL}. */
public class RuneSocketGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int RUNES_START = 10;
    private static final int ADD_SLOT = 22;
    private static final int BACK_SLOT = 26;

    private final Spell spell;
    private final PlayerSpellbook spellbook;
    private final RuneManager runeManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;

    public RuneSocketGUI(Player player, Spell spell, PlayerSpellbook spellbook, RuneManager runeManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.rune_socket.title", "spell", spell.displayName()), SIZE);
        this.spell = spell;
        this.spellbook = spellbook;
        this.runeManager = runeManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<String> runeIds = spellbook.runesFor(spell.id());

        for (int i = 0; i < runeIds.size() && i < PlayerSpellbook.MAX_RUNES_PER_SPELL; i++) {

            String runeId = runeIds.get(i);
            String displayName = runeManager.get(runeId).map(rune -> rune.displayName()).orElse(runeId);
            Material icon = runeManager.get(runeId).map(rune -> SchoolBrowserGUI.parseMaterial(rune.icon()))
                    .orElse(Material.EMERALD);

            setItem(RUNES_START + i, new ItemBuilder(icon)
                    .setName(Component.text(displayName, NamedTextColor.GREEN))
                    .setLore(lang.component("gui.common.shift_remove"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.rune_socket.add_label", "count", runeIds.size(),
                        "max", PlayerSpellbook.MAX_RUNES_PER_SPELL))
                .setLore(lang.component("gui.rune_socket.add_lore"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        int runeIndex = slot - RUNES_START;
        List<String> runeIds = spellbook.runesFor(spell.id());

        if (runeIndex >= 0 && runeIndex < runeIds.size() && runeIndex < PlayerSpellbook.MAX_RUNES_PER_SPELL) {
            if (event.isShiftClick()) {
                spellbook.detachRune(spell.id(), runeIds.get(runeIndex));
                build();
            }
            return;
        }

        if (slot == ADD_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.rune_socket.prompt_add"), value -> {

                String runeId = value.trim().toLowerCase(java.util.Locale.ROOT);

                if (runeManager.get(runeId).isEmpty()) {
                    lang.send(player, "gui.rune_socket.unknown_rune");
                    return;
                }

                if (!spellbook.attachRune(spell.id(), runeId)) {
                    lang.send(player, "gui.rune_socket.socket_failed", "max", PlayerSpellbook.MAX_RUNES_PER_SPELL);
                    return;
                }

                lang.send(player, "gui.rune_socket.socketed");
                build();
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
