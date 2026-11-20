package com.sack.rpgroll.tab.tablist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

/** Líneas de cabecera y pie agrupadas en bloques que cada jugador puede ocultar. */
class TablistBlocksTest {

    private static TablistDefinition parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return new TablistParser().parse(config);
    }

    @Test
    void plainStringLinesStillWork() throws Exception {
        TablistDefinition t = parse("""
                id: default
                header:
                  lines:
                    - "&6Hola"
                    - ""
                """);
        assertEquals(List.of(new TablistLine("&6Hola", null, null), new TablistLine("", null, null)), t.headerLines());
    }

    @Test
    void mapLinesCarryBlockAndCondition() throws Exception {
        TablistDefinition t = parse("""
                id: default
                footer:
                  lines:
                    - text: "&7Ping {ping}"
                      block: servidor
                    - text: "&7XP"
                      block: progreso
                      condition: "%rpgroll_max_level% == no"
                """);
        assertEquals(new TablistLine("&7Ping {ping}", null, "servidor"), t.footerLines().get(0));
        assertEquals(new TablistLine("&7XP", "%rpgroll_max_level% == no", "progreso"), t.footerLines().get(1));
    }

    @Test
    void blocksAreListedInOrderWithTheirSettings() throws Exception {
        TablistDefinition t = parse("""
                id: default
                header:
                  lines:
                    - text: "a"
                      block: personaje
                footer:
                  lines:
                    - text: "b"
                      block: servidor
                    - text: "c"
                      block: personaje
                blocks:
                  servidor:
                    icon: COMPARATOR
                    default: false
                """);
        assertEquals(List.of("personaje", "servidor"), t.blocks().stream().map(TablistBlock::id).toList());
        TablistBlock servidor = t.blocks().get(1);
        assertEquals("COMPARATOR", servidor.icon());
        assertFalse(servidor.shownByDefault());
        TablistBlock personaje = t.blocks().get(0);
        assertNull(personaje.icon());
        assertTrue(personaje.shownByDefault());
    }

    @Test
    void anAnimationFrameReplacesOnlyTheTitleLine() {
        assertEquals(List.of("frame", "nivel"), TablistLines.withAnimationFrame(List.of("titulo", "nivel"), "frame"));
        assertEquals(List.of("frame"), TablistLines.withAnimationFrame(List.of(), "frame"));
        assertEquals(List.of("titulo"), TablistLines.withAnimationFrame(List.of("titulo"), null));
    }

    @Test
    void hiddenBlocksAndFalseConditionsAreLeftOut() {
        List<TablistLine> lines = List.of(
                new TablistLine("titulo", null, null),
                new TablistLine("nivel", null, "personaje"),
                new TablistLine("xp", "cond-falsa", "progreso"),
                new TablistLine("ping", null, "servidor"));

        List<String> shown = TablistLines.visible(lines, block -> !Set.of("servidor").contains(block),
                condition -> !"cond-falsa".equals(condition));

        assertEquals(List.of("titulo", "nivel"), shown);
    }
}
