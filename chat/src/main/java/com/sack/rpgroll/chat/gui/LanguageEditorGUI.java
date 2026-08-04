package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.language.Language;
import com.sack.rpgroll.chat.language.LanguageManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Editor de un idioma — nombre, carácter de ofuscación, y razas que lo conocen por defecto. */
public class LanguageEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 10;
    private static final int OBFUSCATION_SLOT = 11;
    private static final int RACES_SLOT = 12;
    private static final int BACK_SLOT = 26;

    private final LanguageManager languageManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Language current;

    public LanguageEditorGUI(Player player, Language language, LanguageManager languageManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Idioma: " + language.id(), NamedTextColor.GOLD), SIZE);
        this.current = language;
        this.languageManager = languageManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Language updated) {
        current = updated;
        languageManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(OBFUSCATION_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Carácter de ofuscación: " + current.obfuscationChar(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Se usa para quien NO conoce el idioma", NamedTextColor.GRAY),
                        Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(RACES_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(Component.text("Razas por defecto: " + String.join(", ", current.defaultForRaces()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Vacío = todos lo conocen (idioma universal)", NamedTextColor.GRAY),
                        Component.text("Click para escribir la lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre del idioma:",
                    value -> replace(new Language(current.id(), value.trim(), current.obfuscationChar(),
                            current.defaultForRaces())));
            return;
        }

        if (slot == OBFUSCATION_SLOT) {
            chatPromptManager.prompt(player, "Escribí un solo carácter para la ofuscación (ej. ?):", value -> {
                char c = value.isBlank() ? '?' : value.trim().charAt(0);
                replace(new Language(current.id(), current.displayName(), c, current.defaultForRaces()));
            });
            return;
        }

        if (slot == RACES_SLOT) {
            chatPromptManager.prompt(player, "Escribí las razas separadas por coma (vacío = universal):", value -> {
                List<String> races = value.isBlank() ? List.of()
                        : new ArrayList<>(List.of(value.split("\\s*,\\s*")));
                replace(new Language(current.id(), current.displayName(), current.obfuscationChar(), races));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
