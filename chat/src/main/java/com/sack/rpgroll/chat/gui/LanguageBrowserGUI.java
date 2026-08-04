package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.language.Language;
import com.sack.rpgroll.chat.language.LanguageManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class LanguageBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 45;
    private static final int CONTENT_SLOTS = 36;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final LanguageManager languageManager;
    private final ChatPromptManager chatPromptManager;
    private List<Language> languages;

    public LanguageBrowserGUI(Player player, LanguageManager languageManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Idiomas RPGRoll", NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);
        this.languageManager = languageManager;
        this.chatPromptManager = chatPromptManager;
        this.languages = List.copyOf(languageManager.getAll());
    }

    @Override
    protected int totalItemCount() {
        return languages.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        Language language = languages.get(absoluteIndex);

        setItem(contentSlot, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(Component.text(language.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text(language.id(), NamedTextColor.DARK_GRAY),
                        Component.text("Razas: " + (language.defaultForRaces().isEmpty() ? "(universal)"
                                : String.join(", ", language.defaultForRaces())), NamedTextColor.GRAY),
                        Component.text("Click para editar", NamedTextColor.YELLOW))
                .build());
    }

    @Override
    protected void renderExtras() {

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear idioma nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
        new LanguageEditorGUI(player, languages.get(absoluteIndex), languageManager, chatPromptManager, this::reopen)
                .open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo idioma:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (languageManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un idioma con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            Language language = new Language(id, id, '?', List.of());
            languageManager.save(language);
            reopen();
        });
    }

    private void reopen() {
        this.languages = List.copyOf(languageManager.getAll());
        build();
    }

}
