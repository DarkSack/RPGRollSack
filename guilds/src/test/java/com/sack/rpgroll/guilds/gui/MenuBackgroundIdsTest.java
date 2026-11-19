package com.sack.rpgroll.guilds.gui;

import com.sack.rpgroll.guilds.gui.guild.GuildAchievementsGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildBrowserGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildCalendarGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildCustomizationGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildDiplomacyGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildHubGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildMembersGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildQuestsGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildTerritoryGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildUpgradeTreeGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildVaultGUI;
import com.sack.rpgroll.guilds.gui.team.TeamHubGUI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("gremios_lista", GuildBrowserGUI.BACKGROUND);
        assertEquals("gremio", GuildHubGUI.BACKGROUND);
        assertEquals("gremio_miembros", GuildMembersGUI.BACKGROUND);
        assertEquals("gremio_cofre", GuildVaultGUI.BACKGROUND);
        assertEquals("gremio_territorio", GuildTerritoryGUI.BACKGROUND);
        assertEquals("gremio_mejoras", GuildUpgradeTreeGUI.BACKGROUND);
        assertEquals("gremio_diplomacia", GuildDiplomacyGUI.BACKGROUND);
        assertEquals("gremio_misiones", GuildQuestsGUI.BACKGROUND);
        assertEquals("gremio_logros", GuildAchievementsGUI.BACKGROUND);
        assertEquals("gremio_calendario", GuildCalendarGUI.BACKGROUND);
        assertEquals("gremio_personalizar", GuildCustomizationGUI.BACKGROUND);
        assertEquals("equipo", TeamHubGUI.BACKGROUND);
        assertEquals("guilds", GuildMenus.MODULE);
    }
}
