package com.sack.rpgroll.sackresourcepack.gui;

import com.sack.rpgroll.sackresourcepack.build.BuildEngine;
import com.sack.rpgroll.sackresourcepack.build.BuildResult;
import com.sack.rpgroll.sackresourcepack.gui.util.ItemBuilder;
import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;
import com.sack.rpgroll.sackresourcepack.validation.ValidationIssue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Panel principal de SackResourcePack — lista de módulos con su estado,
 * resumen de warnings/errores del último build, y botones de acción. No
 * ejecuta la lógica de rebuild/validate/publish/dev-mode directamente:
 * recibe callbacks del plugin principal, que es quien conoce cómo están
 * cableados el {@code BuildEngine}, el host HTTP, el upload remoto y el
 * watcher de desarrollo.
 */
public class DashboardGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int INFO_SLOT = 4;
    private static final int MODULE_START = 9;
    private static final int MODULE_END = 35;
    private static final int MODULES_PER_PAGE = MODULE_END - MODULE_START + 1;

    private static final int REBUILD_SLOT = 36;
    private static final int VALIDATE_SLOT = 37;
    private static final int PUBLISH_SLOT = 38;
    private static final int DEV_MODE_SLOT = 39;
    private static final int BROWSER_SLOT = 40;
    private static final int PREV_SLOT = 41;
    private static final int NEXT_SLOT = 43;
    private static final int CLOSE_SLOT = 44;

    private final Plugin plugin;
    private final BuildEngine buildEngine;
    private final String assetBaseUrl;
    private final Runnable onRebuild;
    private final Runnable onValidate;
    private final Runnable onPublish;
    private final Runnable onToggleDevMode;
    private final BooleanSupplier devModeActive;

    private int page = 0;

    public DashboardGUI(Player player, Plugin plugin, BuildEngine buildEngine, String assetBaseUrl,
            Runnable onRebuild, Runnable onValidate, Runnable onPublish, Runnable onToggleDevMode,
            BooleanSupplier devModeActive) {
        super(player, Component.text("SackResourcePack", NamedTextColor.GOLD), SIZE);
        this.plugin = plugin;
        this.buildEngine = buildEngine;
        this.assetBaseUrl = assetBaseUrl;
        this.onRebuild = onRebuild;
        this.onValidate = onValidate;
        this.onPublish = onPublish;
        this.onToggleDevMode = onToggleDevMode;
        this.devModeActive = devModeActive;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.filler());
        }

        BuildResult result = buildEngine.getLastResult();
        List<AssetModule> modules = result != null ? result.modules() : List.of();

        setItem(INFO_SLOT, buildInfoItem(result, modules));

        int totalPages = Math.max(1, (modules.size() + MODULES_PER_PAGE - 1) / MODULES_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * MODULES_PER_PAGE;

        for (int i = 0; i < MODULES_PER_PAGE && start + i < modules.size(); i++) {
            setItem(MODULE_START + i, buildModuleItem(modules.get(start + i)));
        }

        setItem(REBUILD_SLOT, ItemBuilder.of(Material.ANVIL, "Reconstruir", NamedTextColor.YELLOW,
                "Fuerza un build completo,", "ignorando el caché."));
        setItem(VALIDATE_SLOT, ItemBuilder.of(Material.WRITABLE_BOOK, "Validar", NamedTextColor.AQUA,
                "Reconstruye solo si algo", "cambió y muestra los issues."));
        setItem(PUBLISH_SLOT, ItemBuilder.of(Material.ENDER_EYE, "Publicar", NamedTextColor.LIGHT_PURPLE,
                "Distribuye el pack actual", "a todos los jugadores conectados."));
        setItem(DEV_MODE_SLOT, buildDevModeItem());
        setItem(BROWSER_SLOT, ItemBuilder.of(Material.CHEST, "Explorador de Assets", NamedTextColor.GREEN,
                "Ver todos los assets", "del pack fusionado."));

        if (page > 0) {
            setItem(PREV_SLOT, ItemBuilder.of(Material.ARROW, "Página anterior", NamedTextColor.GRAY));
        }

        if (page < totalPages - 1) {
            setItem(NEXT_SLOT, ItemBuilder.of(Material.ARROW, "Página siguiente", NamedTextColor.GRAY));
        }

        setItem(CLOSE_SLOT, ItemBuilder.of(Material.BARRIER, "Cerrar", NamedTextColor.RED));
    }

    private ItemStack buildInfoItem(BuildResult result, List<AssetModule> modules) {

        if (result == null) {
            return ItemBuilder.of(Material.PAPER, "Sin build todavía", NamedTextColor.GRAY,
                    "Usá 'Reconstruir' para generar", "el pack por primera vez.");
        }

        long errors = result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count()
                + result.resolutionErrors().size();
        long warnings = result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.WARNING)
                .count();

        NamedTextColor color = errors > 0 ? NamedTextColor.RED : warnings > 0 ? NamedTextColor.YELLOW
                : NamedTextColor.GREEN;

        String sha1Short = result.sha1() == null || result.sha1().isBlank() ? "—"
                : result.sha1().substring(0, Math.min(10, result.sha1().length()));

        return ItemBuilder.of(Material.KNOWLEDGE_BOOK, "Estado del pack", color,
                modules.size() + " módulo(s) cargado(s)",
                errors + " error(es), " + warnings + " warning(s)",
                "SHA1: " + sha1Short,
                result.fromCache() ? "(resultado cacheado)" : "(build recién generado)");
    }

    private ItemStack buildModuleItem(AssetModule module) {
        return ItemBuilder.of(Material.BOOK, module.name(), NamedTextColor.WHITE,
                "id: " + module.id(),
                "versión: " + module.version(),
                "autor: " + (module.author().isBlank() ? "—" : module.author()),
                "prioridad: " + module.priority(),
                module.description().isBlank() ? "" : module.description());
    }

    private ItemStack buildDevModeItem() {

        boolean active = devModeActive.getAsBoolean();

        return ItemBuilder.of(active ? Material.LIME_DYE : Material.GRAY_DYE,
                active ? "Modo desarrollo: ACTIVO" : "Modo desarrollo: inactivo",
                active ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                "Click para " + (active ? "desactivar" : "activar") + ".",
                "Reconstruye solo al detectar", "cambios en content/.");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == REBUILD_SLOT) {
            onRebuild.run();
            build();
        } else if (slot == VALIDATE_SLOT) {
            onValidate.run();
            build();
        } else if (slot == PUBLISH_SLOT) {
            onPublish.run();
            build();
        } else if (slot == DEV_MODE_SLOT) {
            onToggleDevMode.run();
            build();
        } else if (slot == BROWSER_SLOT) {
            new AssetBrowserGUI(player, buildEngine, assetBaseUrl, this::reopen).open();
        } else if (slot == PREV_SLOT) {
            page--;
            build();
        } else if (slot == NEXT_SLOT) {
            page++;
            build();
        } else if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        open();
    }

}
