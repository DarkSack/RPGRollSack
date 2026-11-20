package com.sack.rpgroll.emotes.gui;

import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.player.EmotePlayer;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickDialogTest {

    @TempDir
    Path tmp;

    private static final Component MENU = Component.text("Abrir menú");
    private static final Component STOP = Component.text("Parar");

    private EmoteCatalog catalog;
    private EmotePlayer data;

    @BeforeEach
    void setUp() throws Exception {
        Path folder = tmp.resolve("emotes");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("wave.yml"), "animation: wave\nname: \"Saludo\"\n");
        Files.writeString(folder.resolve("dance.yml"), "animation: dance\nname: \"Baile\"\n");
        catalog = new EmoteCatalog();
        catalog.load(List.of(folder.toFile()), w -> { throw new AssertionError(w); });
        data = new EmotePlayerStore(tmp.resolve("data").toFile(), 7).get(UUID.randomUUID());
    }

    private List<QuickDialog.Button> buttons() {
        return QuickDialog.buttons(data, catalog, e -> Component.text(e.name()), MENU, STOP);
    }

    @Test
    void buttonsForOccupiedFavoritesOnly() {
        data.toggleFavorite("wave");   // hueco 1
        data.toggleFavorite("borrado"); // hueco 2: ya no está en el catálogo, no sale
        data.toggleFavorite("dance");  // hueco 3
        List<String> commands = buttons().stream().map(QuickDialog.Button::command).toList();
        assertEquals(List.of("/emote 1", "/emote 3", "/emotes", "/emote stop"), commands);
    }

    @Test
    void alwaysHasMenuAndStop() {
        assertEquals(List.of(new QuickDialog.Button(MENU, "/emotes"), new QuickDialog.Button(STOP, "/emote stop")),
                buttons());
    }

    @Test
    void labelsUseEmoteName() {
        data.toggleFavorite("dance");
        assertEquals(Component.text("Baile"), buttons().get(0).label());
    }
}
