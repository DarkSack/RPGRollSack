package com.sack.rpgroll.tab.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ScoreboardLineTextTest {

    @Test
    void aLongLineWithAColorPerLetterKeepsEveryLetterAndItsColor() {
        // El nombre de un gremio con un color por letra pasa de 64 caracteres:
        // partirlo cortaba un código por la mitad y se perdía el final.
        String line = "&6Gremio: &f&#54DAF4P&#549CD5a&#545EB6p&#1E27FFo&#222DEBi&#2632D7s &7(nivel 3)";
        Team team = mock(Team.class);

        ScoreboardEngine.applyLineText(team, line);

        ArgumentCaptor<Component> prefix = ArgumentCaptor.forClass(Component.class);
        ArgumentCaptor<Component> suffix = ArgumentCaptor.forClass(Component.class);
        verify(team).prefix(prefix.capture());
        verify(team).suffix(suffix.capture());

        String visible = plain(prefix.getValue()) + plain(suffix.getValue());
        assertEquals("Gremio: Papois (nivel 3)", visible);
        assertEquals(TextColor.fromHexString("#2632D7"), colorOf(suffix.getValue(), "s "));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static TextColor colorOf(Component root, String text) {
        if (root instanceof net.kyori.adventure.text.TextComponent t && t.content().equals(text)) {
            return t.color();
        }
        for (Component child : root.children()) {
            TextColor found = colorOf(child, text);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

}
