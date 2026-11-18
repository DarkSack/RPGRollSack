package com.sack.rpgroll.gui;

import net.kyori.adventure.text.Component;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuSkinTest {

    @Test
    void offByDefault() {
        assertSame(MenuSkin.OFF, MenuSkin.get("nadie"));
        assertFalse(MenuSkin.OFF.drawn());
        assertEquals("", MenuSkin.OFF.background("tienda"));
        assertNull(MenuSkin.OFF.fillerModel());
    }

    @Test
    void drawnPackBuildsKeys() {
        MenuSkin skin = MenuSkin.of("sackito:menus");
        assertTrue(skin.drawn());
        assertEquals("sackito:menus/tienda", skin.background("tienda"));
        assertEquals("sackito:menus/invisible", skin.fillerModel());
    }

    @Test
    void invalidPackIsOff() {
        assertEquals("sackito:menus/x", MenuSkin.of(" Sackito:Menus ").background("x"));
        assertFalse(MenuSkin.of("con espacio:x y").drawn());
        assertFalse(MenuSkin.of("").drawn());
        assertFalse(MenuSkin.of(null).drawn());
        assertEquals("", MenuSkin.of("con espacio:x y").background("x"));
    }

    @Test
    void readsPackFromSection() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("menus.pack", "sackito:menus");
        assertTrue(MenuSkin.from(yaml.getConfigurationSection("menus")).drawn());
        assertFalse(MenuSkin.from(null).drawn());
    }

    @Test
    void titleWithoutPackIsSameComponent() {
        Component t = Component.text("Tienda");
        assertSame(t, MenuSkin.OFF.title("tienda", t));
    }

    @Test
    void registerReplacesAndGets() {
        MenuSkin.register("prueba", MenuSkin.of("sackito:menus"));
        assertTrue(MenuSkin.get("prueba").drawn());
        MenuSkin.register("prueba", MenuSkin.OFF);
        assertSame(MenuSkin.OFF, MenuSkin.get("prueba"));
    }
}
