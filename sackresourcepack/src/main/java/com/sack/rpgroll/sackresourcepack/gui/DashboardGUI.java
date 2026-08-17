package com.sack.rpgroll.sackresourcepack.gui;

import com.sack.rpgroll.sackresourcepack.build.BuildEngine;
import com.sack.rpgroll.sackresourcepack.build.BuildResult;
import com.sack.rpgroll.sackresourcepack.gui.util.ItemBuilder;
import com.sack.rpgroll.sackresourcepack.lang.LangManager;
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
    private final LangManager lang;
    private final Runnable onRebuild;
    private final Runnable onValidate;
    private final Runnable onPublish;
    private final Runnable onToggleDevMode;
    private final BooleanSupplier devModeActive;

    private int page = 0;

    public DashboardGUI(Player player, Plugin plugin, BuildEngine buildEngine, String assetBaseUrl, LangManager lang,
            Runnable onRebuild, Runnable onValidate, Runnable onPublish, Runnable onToggleDevMode,
            BooleanSupplier devModeActive) {
        super(player, Component.text("SackResourcePack", NamedTextColor.GOLD), SIZE);
        this.plugin = plugin;
        this.buildEngine = buildEngine;
        this.assetBaseUrl = assetBaseUrl;
        this.lang = lang;
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

        setItem(REBUILD_SLOT, ItemBuilder.of(Material.ANVIL, lang.raw("gui.dashboard.rebuild-name"), NamedTextColor.YELLOW,
                lang.raw("gui.dashboard.rebuild-lore-1"), lang.raw("gui.dashboard.rebuild-lore-2")));
        setItem(VALIDATE_SLOT, ItemBuilder.of(Material.WRITABLE_BOOK, lang.raw("gui.dashboard.validate-name"), NamedTextColor.AQUA,
                lang.raw("gui.dashboard.validate-lore-1"), lang.raw("gui.dashboard.validate-lore-2")));
        setItem(PUBLISH_SLOT, ItemBuilder.of(Material.ENDER_EYE, lang.raw("gui.dashboard.publish-name"), NamedTextColor.LIGHT_PURPLE,
                lang.raw("gui.dashboard.publish-lore-1"), lang.raw("gui.dashboard.publish-lore-2")));
        setItem(DEV_MODE_SLOT, buildDevModeItem());
        setItem(BROWSER_SLOT, ItemBuilder.of(Material.CHEST, lang.raw("gui.dashboard.browser-name"), NamedTextColor.GREEN,
                lang.raw("gui.dashboard.browser-lore-1"), lang.raw("gui.dashboard.browser-lore-2")));

        if (page > 0) {
            setItem(PREV_SLOT, ItemBuilder.of(Material.ARROW, lang.raw("gui.prev-page"), NamedTextColor.GRAY));
        }

        if (page < totalPages - 1) {
            setItem(NEXT_SLOT, ItemBuilder.of(Material.ARROW, lang.raw("gui.next-page"), NamedTextColor.GRAY));
        }

        setItem(CLOSE_SLOT, ItemBuilder.of(Material.BARRIER, lang.raw("gui.dashboard.close"), NamedTextColor.RED));
    }

    private ItemStack buildInfoItem(BuildResult result, List<AssetModule> modules) {

        if (result == null) {
            return ItemBuilder.of(Material.PAPER, lang.raw("gui.dashboard.no-build-name"), NamedTextColor.GRAY,
                    lang.raw("gui.dashboard.no-build-lore-1"), lang.raw("gui.dashboard.no-build-lore-2"));
        }

        long errors = result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count()
                + result.resolutionErrors().size();
        long warnings = result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.WARNING)
                .count();

        NamedTextColor color = errors > 0 ? NamedTextColor.RED : warnings > 0 ? NamedTextColor.YELLOW
                : NamedTextColor.GREEN;

        String sha1Short = result.sha1() == null || result.sha1().isBlank() ? "—"
                : result.sha1().substring(0, Math.min(10, result.sha1().length()));

        return ItemBuilder.of(Material.KNOWLEDGE_BOOK, lang.raw("gui.dashboard.status-name"), color,
                lang.raw("gui.dashboard.status-modules-loaded", "count", modules.size()),
                lang.raw("gui.dashboard.status-issues", "errors", errors, "warnings", warnings),
                lang.raw("gui.dashboard.status-sha1", "sha1", sha1Short),
                lang.raw(result.fromCache() ? "gui.dashboard.status-cached" : "gui.dashboard.status-fresh"));
    }

    private ItemStack buildModuleItem(AssetModule module) {
        return ItemBuilder.of(Material.BOOK, module.name(), NamedTextColor.WHITE,
                lang.raw("gui.dashboard.module-id", "id", module.id()),
                lang.raw("gui.dashboard.module-version", "version", module.version()),
                lang.raw("gui.dashboard.module-author", "author", module.author().isBlank() ? "—" : module.author()),
                lang.raw("gui.dashboard.module-priority", "priority", module.priority()),
                module.description().isBlank() ? "" : module.description());
    }

    private ItemStack buildDevModeItem() {

        boolean active = devModeActive.getAsBoolean();

        return ItemBuilder.of(active ? Material.LIME_DYE : Material.GRAY_DYE,
                lang.raw(active ? "gui.dashboard.devmode-active-name" : "gui.dashboard.devmode-inactive-name"),
                active ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                lang.raw(active ? "gui.dashboard.devmode-lore-toggle-deactivate" : "gui.dashboard.devmode-lore-toggle-activate"),
                lang.raw("gui.dashboard.devmode-lore-1"), lang.raw("gui.dashboard.devmode-lore-2"));
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
            new AssetBrowserGUI(player, buildEngine, assetBaseUrl, lang, this::reopen).open();
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
