package com.sack.rpgroll.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComponentUtilsColorTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    // El bug real: un mensaje de uso lleva <spawn|list|...>, lo que antes lo
    // mandaba a MiniMessage — que no entiende &e — y el código quedaba impreso.
    @Test
    void aUsageMessageWithAngleBracketsStillAppliesItsLegacyColour() {
        Component parsed = ComponentUtils.parse("&eUso: /mobadmin <spawn|list|info>");

        assertEquals("Uso: /mobadmin <spawn|list|info>", plain(parsed));
        assertEquals(NamedTextColor.YELLOW, parsed.color());
    }

    // Con varios códigos, Legacy reparte el color entre los hijos y la raíz
    // queda sin color: lo que importa es que NINGÚN código sobreviva como
    // texto visible, que es exactamente lo que veía el comprador.
    @ParameterizedTest
    @ValueSource(strings = {
        "&cUso: /rpg race <raza>",
        "&aDiste &f<cantidad> &aa <jugador>",
        "&7Objetivo: <mob> x<n>",
        "&eUso: /trapadmin <create|edit|browser|reload|place|remove|list|info|forcetrigger|turret> [args]",
    })
    void placeholdersInAngleBracketsNeverLeakColourCodes(String raw) {
        String rendered = plain(ComponentUtils.parse(raw));

        assertFalse(rendered.contains("&c") || rendered.contains("&a")
                        || rendered.contains("&e") || rendered.contains("&7") || rendered.contains("&f"),
                "quedó un código de color sin traducir: " + rendered);
    }

    @Test
    void realMiniMessageStillWorks() {
        assertEquals("Arquero", plain(ComponentUtils.parse("<red>Arquero</red>")));
        assertEquals(NamedTextColor.RED, ComponentUtils.parse("<red>Arquero</red>").color());
    }

    @Test
    void miniMessageGradientsAreStillHonoured() {
        assertEquals("Arquero",
                plain(ComponentUtils.parse("<gradient:#54daf4:#545eb6>Arquero</gradient>")));
    }

    @Test
    void legacyHexIsStillHonoured() {
        assertEquals("Hola", plain(ComponentUtils.parse("&#54DAF4Hola")));
    }

    @Test
    void plainTextStaysPlain() {
        Component parsed = ComponentUtils.parse("sin formato");

        assertEquals("sin formato", plain(parsed));
        assertNull(parsed.color());
    }

    // parseWithDefault: el color del YAML tiene que ganarle al del código.
    @Test
    void theFallbackColourAppliesWhenTheTextHasNone() {
        assertEquals(NamedTextColor.RED,
                ComponentUtils.parseWithDefault("sin color propio", NamedTextColor.RED).color());
    }

    @Test
    void aColourWrittenInTheYamlBeatsTheJavaDefault() {
        assertEquals(NamedTextColor.GREEN,
                ComponentUtils.parseWithDefault("&averde", NamedTextColor.RED).color());
    }

    @Test
    void aMiniMessageColourAlsoBeatsTheJavaDefault() {
        assertEquals(NamedTextColor.GREEN,
                ComponentUtils.parseWithDefault("<green>verde</green>", NamedTextColor.RED).color());
    }

    @Test
    void aNullFallbackLeavesTheComponentUntouched() {
        assertNull(ComponentUtils.parseWithDefault("sin color", null).color());
    }
}
