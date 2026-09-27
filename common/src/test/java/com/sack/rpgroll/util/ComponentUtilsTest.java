package com.sack.rpgroll.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentUtilsTest {

    @Test
    void parseReturnsEmptyComponentForNull() {
        assertEquals(Component.empty(), ComponentUtils.parse(null));
    }

    @Test
    void parseReturnsEmptyComponentForBlankString() {
        assertEquals(Component.empty(), ComponentUtils.parse("   "));
    }

    @Test
    void parseReturnsEmptyComponentForEmptyString() {
        assertEquals(Component.empty(), ComponentUtils.parse(""));
    }

    @Test
    void parseHandlesLegacyAmpersandColorCodes() {
        Component result = ComponentUtils.parse("&6Gold text");

        assertEquals(NamedTextColor.GOLD, result.color());
        assertEquals("Gold text", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void parseHandlesLegacyHexColorCodes() {
        Component result = ComponentUtils.parse("&#54daf4Hex text");

        assertEquals(TextColor.fromHexString("#54daf4"), result.color());
    }

    @Test
    void parseHandlesBungeeCordRepeatedHexFormat() {
        String bungeeHex = "&x&5&4&d&a&f&4Hex text";
        Component result = ComponentUtils.parse(bungeeHex);

        assertEquals(TextColor.fromHexString("#54daf4"), result.color());
        assertEquals("Hex text", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void parseHandlesMiniMessageWhenAngleBracketsPresent() {
        Component result = ComponentUtils.parse("<red>Danger");

        assertEquals(NamedTextColor.RED, result.color());
        assertEquals("Danger", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void parseTreatsPlainTextWithoutSpecialCharsAsLegacy() {
        Component result = ComponentUtils.parse("just plain text");

        assertEquals("just plain text", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void parseIsNotNullForArbitraryInput() {
        assertInstanceOf(Component.class, ComponentUtils.parse("<gradient:#54daf4:#545eb6>Gradient</gradient>"));
    }

    @Test
    void aLegacyTemplateWithAMiniMessageNameShowsNoRawTags() {

        // "&7Recibiste {item}" con un ítem de nombre en degradado.
        Component result = ComponentUtils.parse("&7Recibiste <gradient:#a18cd1:#fbc2eb>Grimorio</gradient>");

        assertEquals("Recibiste Grimorio", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void aMiniMessageTemplateWithALegacyNameShowsNoRawCodes() {

        Component result = ComponentUtils.parse("<gold>Ganaste &6&lEspada &7de hierro");

        assertEquals("Ganaste Espada de hierro", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void placeholdersInAngleBracketsStayLegacyText() {

        Component result = ComponentUtils.parse("&cUso: /guild create <nombre>");

        assertEquals("Uso: /guild create <nombre>", PlainTextComponentSerializer.plainText().serialize(result));
        assertEquals(NamedTextColor.RED, result.children().isEmpty() ? result.color() : result.children().get(0).color());
    }

    @Test
    void legacyCodesBecomeTheirTags() {
        assertEquals("<reset><gray>a<bold>b<reset><#54daf4>c<reset><#54daf4>d",
                ComponentUtils.legacyToMiniMessage("&7a&lb&#54daf4c&x&5&4&d&a&f&4d"));
    }

    @Test
    void parseTextContainingOnlyOpenBracketFallsBackToLegacy() {
        // Sin '>' de cierre no cuenta como MiniMessage según la heurística de parse().
        Component result = ComponentUtils.parse("&aless < than");
        assertTrue(PlainTextComponentSerializer.plainText().serialize(result).contains("less < than"));
    }
}
