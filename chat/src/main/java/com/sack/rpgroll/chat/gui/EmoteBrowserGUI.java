package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.emote.EmoteDefinition;
import com.sack.rpgroll.chat.emote.EmoteManager;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class EmoteBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 45;
    private static final int CONTENT_SLOTS = 36;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final EmoteManager emoteManager;
    private final ChatPromptManager chatPromptManager;
    private List<EmoteDefinition> emotes;

    public EmoteBrowserGUI(Player player, EmoteManager emoteManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("emote.browser_title"), SIZE, CONTENT_SLOTS);
        this.emoteManager = emoteManager;
        this.chatPromptManager = chatPromptManager;
        this.emotes = List.copyOf(emoteManager.getAll());
    }

    @Override
    protected int totalItemCount() {
        return emotes.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        EmoteDefinition emote = emotes.get(absoluteIndex);
        LangManager lang = chatPromptManager.lang();

        setItem(contentSlot, new ItemBuilder(Material.ARMOR_STAND)
                .setName(Component.text(emote.id(), NamedTextColor.YELLOW))
                .setLore(lang.component("emote.browser_lore_radius", "value",
                                emote.radius() <= 0 ? lang.raw("emote.radius_world") : emote.radius())
                        .colorIfAbsent(NamedTextColor.GRAY),
                        lang.component("gui.click_edit").colorIfAbsent(NamedTextColor.YELLOW))
                .build());
    }

    @Override
    protected void renderExtras() {

        LangManager lang = chatPromptManager.lang();

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("emote.create_new").colorIfAbsent(NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.close")));
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
        new EmoteEditorGUI(player, emotes.get(absoluteIndex), emoteManager, chatPromptManager, this::reopen).open();
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        switch (event.getSlot()) {
            case NEW_SLOT -> promptNew();
            case BACK_SLOT -> close();
            default -> {
            }
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("emote.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (emoteManager.exists(id)) {
                chatPromptManager.lang().send(player, "emote.already_exists");
                reopen();
                return;
            }

            EmoteDefinition emote = new EmoteDefinition(id, "{player} hace una acción.", "", 0);
            emoteManager.save(emote);
            reopen();
        });
    }

    private void reopen() {
        this.emotes = List.copyOf(emoteManager.getAll());
        open();
    }

}
