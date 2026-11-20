package com.sack.rpgroll.guilds.guild;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildNameStyleTest {

    @Test
    void acceptsTheRepeatedHexFormatAndStoresItCompact() {
        String input = "&x&5&4&D&A&F&4P&x&5&4&9&C&D&5a&x&5&4&5&E&B&6p&x&1&E&2&7&F&Fo&x&2&2&2&D&E&Bi&x&2&6&3&2&D&7s";

        assertEquals(Optional.of("&#54DAF4P&#549CD5a&#545EB6p&#1E27FFo&#222DEBi&#2632D7s"),
                GuildNameStyle.parse(input, "Papois"));
    }

    @Test
    void acceptsPlainHexAndClassicColorsAndTheSectionSign() {
        assertEquals(Optional.of("&#FF0000Pa&apois"), GuildNameStyle.parse("&#ff0000Pa§Apois", "Papois"));
        assertEquals(Optional.of("&6Los &bReyes"), GuildNameStyle.parse("&6Los &bReyes", "Los Reyes"));
    }

    @Test
    void dropsFormatsThatAreNotColors() {
        assertEquals(Optional.of("&cPapois"), GuildNameStyle.parse("&c&l&kPap&ro&nis", "Papois"));
    }

    @Test
    void rejectsTextThatIsNotTheGuildName() {
        assertEquals(Optional.empty(), GuildNameStyle.parse("&aStaff", "Papois"));
        assertEquals(Optional.empty(), GuildNameStyle.parse("&apapois", "Papois"));
        assertEquals(Optional.empty(), GuildNameStyle.parse("&aPapois [Admin]", "Papois"));
    }

    @Test
    void rejectsMiniMessageTagsAndBrokenHex() {
        assertEquals(Optional.empty(), GuildNameStyle.parse("<click:run_command:/op x>Papois", "Papois"));
        assertEquals(Optional.empty(), GuildNameStyle.parse("<red>Papois", "Papois"));
        assertEquals(Optional.empty(), GuildNameStyle.parse("&x&5&4Papois", "Papois"));
    }

    @Test
    void rejectsBlankAndTooLongInput() {
        assertEquals(Optional.empty(), GuildNameStyle.parse("", "Papois"));
        assertEquals(Optional.empty(), GuildNameStyle.parse(null, "Papois"));
        assertEquals(Optional.empty(), GuildNameStyle.parse("&a".repeat(300) + "Papois", "Papois"));
    }

    @Test
    void animationMovesEachColorOneLetterPerStep() {
        String styled = "&#111111a&#222222b&#333333c";

        assertEquals(styled, GuildNameStyle.animate(styled, 0));
        assertEquals("&#333333a&#111111b&#222222c", GuildNameStyle.animate(styled, 1));
        assertEquals("&#222222a&#333333b&#111111c", GuildNameStyle.animate(styled, 2));
        assertEquals(styled, GuildNameStyle.animate(styled, 3));
    }

    @Test
    void animationSpreadsAColorOverTheLettersItCovers() {
        // "&aLos &bRey": a a a a b b b → un paso después la ola entra por la izquierda.
        assertEquals("&bL&aos R&bey", GuildNameStyle.animate("&aLos &bRey", 1));
    }

    @Test
    void aSingleColorOrNoStyleDoesNotMove() {
        assertEquals("&6Papois", GuildNameStyle.animate("&6Papois", 5));
        assertEquals("Papois", GuildNameStyle.animate("Papois", 5));
    }

    @Test
    void animatedPlaceholderTextKeepsTheVisibleName() {
        Guild guild = new Guild("papois", "Papois", java.util.UUID.randomUUID());
        guild.setStyledName("&#54DAF4P&#549CD5a&#545EB6p&#1E27FFo&#222DEBi&#2632D7s");

        for (long step = 0; step < 6; step++) {
            String frame = GuildNameStyle.animate(guild.styledName(), step);
            assertEquals(Optional.of(frame), GuildNameStyle.parse(frame, "Papois"));
        }
        assertEquals("&6Papois", GuildNameStyle.displayAnimated(new Guild("x", "Papois", java.util.UUID.randomUUID()), 3));
    }

    @Test
    void fallbackUsesTheGuildColor() {
        assertEquals("&6Papois", GuildNameStyle.fallback("Papois", NamedTextColor.GOLD));
    }

    @Test
    void guildDropsItsStyleWhenRenamed() {
        Guild guild = new Guild("papois", "Papois", java.util.UUID.randomUUID());
        guild.setStyledName("&#54DAF4Papois");

        guild.setName("Papois");
        assertEquals("&#54DAF4Papois", guild.styledName());

        guild.setName("Otros");
        assertTrue(guild.styledName() == null);
    }

}
