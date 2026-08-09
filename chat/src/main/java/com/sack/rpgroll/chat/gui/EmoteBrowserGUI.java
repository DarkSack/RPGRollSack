package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.emote.EmoteDefinition;
import com.sack.rpgroll.chat.emote.EmoteManager;

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
        super(player, Component.text("Emotes RPGRoll", NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);
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

        setItem(contentSlot, new ItemBuilder(Material.ARMOR_STAND)
                .setName(Component.text(emote.id(), NamedTextColor.YELLOW))
                .setLore(Component.text("Radio: " + (emote.radius() <= 0 ? "todo el mundo" : emote.radius()),
                        NamedTextColor.GRAY),
                        Component.text("Click para editar", NamedTextColor.YELLOW))
                .build());
    }

    @Override
    protected void renderExtras() {

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear emote nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva emote (ej. /emote <id>):", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (emoteManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una emote con ese id.", NamedTextColor.RED));
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
