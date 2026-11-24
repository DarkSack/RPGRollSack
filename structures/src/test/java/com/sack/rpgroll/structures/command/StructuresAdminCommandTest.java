package com.sack.rpgroll.structures.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.StructuresPlugin;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StructuresAdminCommandTest {
    final StructuresPlugin plugin = mock(StructuresPlugin.class);
    final LangManager lang = mock(LangManager.class);
    final CommandSender sender = mock(CommandSender.class);
    final StructuresAdminCommand cmd = new StructuresAdminCommand(plugin);

    StructuresAdminCommandTest() {
        when(plugin.lang()).thenReturn(lang);
    }

    @Test void giveNeedsPermission() {
        cmd.onCommand(sender, null, "sa", new String[]{"give", "Sack", "ingeniero"});
        verify(lang).send(sender, "admin.no_permission");
        verify(plugin, never()).issuer();
    }

    @Test void reloadNeedsPermission() {
        cmd.onCommand(sender, null, "sa", new String[]{"reload"});
        verify(lang).send(sender, "admin.no_permission");
        verify(plugin, never()).reloadAll();
    }

    @Test void reloadCallsReloadAll() {
        when(sender.hasPermission("rpgrollstructures.admin.reload")).thenReturn(true);
        cmd.onCommand(sender, null, "sa", new String[]{"reload"});
        verify(plugin).reloadAll();
        verify(lang).send(sender, "admin.reload.done");
    }

    @Test void reloadFailureIsReported() {
        when(sender.hasPermission("rpgrollstructures.admin.reload")).thenReturn(true);
        doThrow(new IllegalStateException("x")).when(plugin).reloadAll();
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        cmd.onCommand(sender, null, "sa", new String[]{"reload"});
        verify(lang).send(sender, "admin.reload.error");
    }

    @Test void badTierIsReportedWithItsLangKey() {
        when(sender.hasPermission("rpgrollstructures.admin.give")).thenReturn(true);
        cmd.onCommand(sender, null, "sa", new String[]{"give", "Sack", "maestro"});
        verify(lang).send(sender, "admin.give.bad_tier");
    }

    @Test void noArgsShowsUsageAndTabCompletes() {
        cmd.onCommand(sender, null, "sa", new String[]{});
        verify(lang).send(sender, "admin.usage");
        assertEquals(List.of("give", "reload"), cmd.onTabComplete(sender, null, "sa", new String[]{""}));
        assertEquals(List.of("constructor", "ingeniero", "arquitecto"),
                cmd.onTabComplete(sender, null, "sa", new String[]{"give", "Sack", ""}));
    }
}
