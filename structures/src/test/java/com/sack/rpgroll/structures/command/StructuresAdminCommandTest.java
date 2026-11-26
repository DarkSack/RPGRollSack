package com.sack.rpgroll.structures.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.catalog.Problem;
import com.sack.rpgroll.structures.catalog.StructureLoader;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test void validateNeedsPermission() {
        cmd.onCommand(sender, null, "sa", new String[]{"validate"});
        verify(lang).send(sender, "admin.no_permission");
        verify(plugin, never()).scanCatalog();
    }

    @Test void validateListsEveryProblemWithFileKeyAndReason() {
        when(sender.hasPermission("rpgrollstructures.admin.validate")).thenReturn(true);
        when(plugin.scanCatalog()).thenReturn(new StructureLoader.LoadResult(List.of(),
                List.of(new Problem("casa.yml", "category", "categoría desconocida: x"),
                        new Problem("torre.yml", "name.es", "falta el nombre en español"))));
        cmd.onCommand(sender, null, "sa", new String[]{"validate"});
        verify(lang).send(sender, "admin.validate.header", "valid", 0, "problems", 2);
        verify(lang).send(sender, "admin.validate.problem", "file", "casa.yml", "key", "category", "reason",
                "categoría desconocida: x");
        verify(lang).send(sender, "admin.validate.problem", "file", "torre.yml", "key", "name.es", "reason",
                "falta el nombre en español");
    }

    @Test void validateWithoutProblemsSaysSo() {
        when(sender.hasPermission("rpgrollstructures.admin.validate")).thenReturn(true);
        when(plugin.scanCatalog()).thenReturn(new StructureLoader.LoadResult(List.of(), List.of()));
        cmd.onCommand(sender, null, "sa", new String[]{"validate"});
        verify(lang).send(sender, "admin.validate.ok", "valid", 0);
    }

    @Test void integrationsListsEveryHook() {
        when(sender.hasPermission("rpgrollstructures.admin")).thenReturn(true);
        when(lang.raw("integrations.reason.missing")).thenReturn("no está");
        when(plugin.integrations()).thenReturn(com.sack.rpgroll.structures.integration.Integrations.load(
                new org.bukkit.configuration.file.YamlConfiguration(),
                name -> !name.equals("Vault"), java.util.Map.of(), Logger.getAnonymousLogger()));
        cmd.onCommand(sender, null, "sa", new String[]{"integrations"});
        verify(lang).send(sender, "integrations.line", "mark", "✘", "plugin", "Vault", "reason", "no está");
        verify(lang).send(sender, "integrations.line", "mark", "✔", "plugin", "WorldGuard", "reason", "");
        verify(lang, times(com.sack.rpgroll.structures.integration.Hook.values().length))
                .send(eq(sender), eq("integrations.line"), any(Object[].class));
    }

    @Test void integrationsNeedsPermission() {
        cmd.onCommand(sender, null, "sa", new String[]{"integrations"});
        verify(lang).send(sender, "admin.no_permission");
    }

    @Test void badTierIsReportedWithItsLangKey() {
        when(sender.hasPermission("rpgrollstructures.admin.give")).thenReturn(true);
        cmd.onCommand(sender, null, "sa", new String[]{"give", "Sack", "maestro"});
        verify(lang).send(sender, "admin.give.bad_tier");
    }

    @Test void tabCompletionOnlyOffersWhatTheSenderMayRun() {
        assertEquals(List.of(), cmd.onTabComplete(sender, null, "sa", new String[]{""}));
        assertEquals(List.of(), cmd.onTabComplete(sender, null, "sa", new String[]{"give", "Sack", ""}));
        when(sender.hasPermission("rpgrollstructures.admin.reload")).thenReturn(true);
        assertEquals(List.of("reload"), cmd.onTabComplete(sender, null, "sa", new String[]{""}));
    }

    @Test void noArgsShowsUsageAndTabCompletes() {
        when(sender.hasPermission(anyString())).thenReturn(true);
        cmd.onCommand(sender, null, "sa", new String[]{});
        verify(lang).send(sender, "admin.usage");
        assertEquals(List.of("give", "reload", "validate", "integrations", "debug", "delete"), cmd.onTabComplete(sender, null, "sa", new String[]{""}));
        assertEquals(List.of("constructor", "ingeniero", "arquitecto"),
                cmd.onTabComplete(sender, null, "sa", new String[]{"give", "Sack", ""}));
    }

    @Test void deleteNeedsPermissionAndAnId() {
        cmd.onCommand(sender, null, "sa", new String[]{"delete", java.util.UUID.randomUUID().toString()});
        verify(lang).send(sender, "admin.no_permission");
        verify(plugin, never()).modules();
        when(sender.hasPermission("rpgrollstructures.admin")).thenReturn(true);
        cmd.onCommand(sender, null, "sa", new String[]{"delete", "casa"});
        verify(lang).send(sender, "admin.delete.usage");
        verify(plugin, never()).modules();
    }

    @Test void deleteRemovesTheBuildThroughModules() {
        when(sender.hasPermission("rpgrollstructures.admin")).thenReturn(true);
        var modules = mock(com.sack.rpgroll.structures.module.ModuleService.class);
        when(plugin.modules()).thenReturn(modules);
        java.util.UUID id = java.util.UUID.randomUUID(), gone = java.util.UUID.randomUUID();
        when(modules.remove(id)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));
        when(modules.remove(gone)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(false));
        cmd.onCommand(sender, null, "sa", new String[]{"delete", id.toString()});
        verify(lang).send(sender, "admin.delete.done", "id", id);
        cmd.onCommand(sender, null, "sa", new String[]{"delete", gone.toString()});
        verify(lang).send(sender, "admin.delete.none", "id", gone);
    }

    @Test void debugShowsProjectState() {
        when(sender.hasPermission("rpgrollstructures.admin")).thenReturn(true);
        java.util.UUID id = java.util.UUID.randomUUID(), bench = java.util.UUID.randomUUID();
        var p = new com.sack.rpgroll.structures.project.Project(new com.sack.rpgroll.structures.storage.ProjectRecord(
                id, bench, "casa", java.util.UUID.randomUUID(),
                com.sack.rpgroll.structures.project.ProjectState.PAUSED, "world", 10, 64, -20,
                com.sack.rpgroll.common.structure.Rotation.NONE, 3, 2, 1, 12, "project.paused.protected", 0L, 0L),
                java.util.Map.of());
        var projects = mock(com.sack.rpgroll.structures.project.ProjectService.class);
        when(plugin.projects()).thenReturn(projects);
        when(projects.byId(id)).thenReturn(java.util.Optional.of(p));
        cmd.onCommand(sender, null, "sa", new String[]{"debug", id.toString()});
        verify(lang).send(sender, "admin.debug.header", "id", id, "structure", "casa", "state", "PAUSED");
        verify(lang).send(sender, "admin.debug.progress", "cursor", 3, "total", 12, "placed", 2, "skipped", 1);
        verify(lang).send(sender, "admin.debug.reason", "reason", "project.paused.protected");
        verify(lang).send(sender, "admin.debug.where", "bench", bench, "world", "world", "x", 10, "y", 64, "z", -20);

        java.util.UUID unknown = java.util.UUID.randomUUID();
        when(projects.byId(unknown)).thenReturn(java.util.Optional.empty());
        cmd.onCommand(sender, null, "sa", new String[]{"debug", unknown.toString()});
        verify(lang).send(sender, "admin.debug.none", "target", unknown.toString());
        cmd.onCommand(sender, null, "sa", new String[]{"debug"});
        verify(lang).send(sender, "admin.debug.usage");
    }

    @Test void debugNeedsPermission() {
        cmd.onCommand(sender, null, "sa", new String[]{"debug", java.util.UUID.randomUUID().toString()});
        verify(lang).send(sender, "admin.no_permission");
        verify(plugin, never()).projects();
    }
}
