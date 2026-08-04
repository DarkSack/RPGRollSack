package com.sack.rpgroll.sackresourcepack.gui;

import com.sack.rpgroll.sackresourcepack.build.BuildEngine;
import com.sack.rpgroll.sackresourcepack.gui.util.ItemBuilder;
import com.sack.rpgroll.sackresourcepack.merge.MergeResult;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Explorador de todos los assets del pack ya fusionado — paginado,
 * filtrable por tipo. No hay preview visual real (Bukkit no puede
 * renderizar un PNG dentro de un item de inventario): cada entrada
 * muestra su información en el lore (ruta, tipo, módulo dueño, tamaño).
 */
public class AssetBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int GRID_START = 0;
    private static final int GRID_END = 35;
    private static final int PER_PAGE = GRID_END - GRID_START + 1;

    private static final int FILTER_SLOT = 37;
    private static final int PREV_SLOT = 38;
    private static final int NEXT_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private enum TypeFilter {
        ALL, MODELS, TEXTURES, SOUNDS, FONT, LANG, PARTICLES, OTHER
    }

    private record AssetEntry(String relativePath, String type, long sizeBytes, String owner) {
    }

    private final BuildEngine buildEngine;
    private final Runnable onBack;

    private List<AssetEntry> allEntries = List.of();
    private List<AssetEntry> filteredEntries = List.of();
    private TypeFilter filter = TypeFilter.ALL;
    private int page = 0;

    public AssetBrowserGUI(Player player, BuildEngine buildEngine, Runnable onBack) {
        super(player, Component.text("Explorador de Assets", NamedTextColor.GREEN), SIZE);
        this.buildEngine = buildEngine;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.filler());
        }

        if (allEntries.isEmpty()) {
            allEntries = scanEntries();
        }

        applyFilter();

        int totalPages = Math.max(1, (filteredEntries.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE && start + i < filteredEntries.size(); i++) {
            setItem(GRID_START + i, buildEntryItem(filteredEntries.get(start + i)));
        }

        setItem(FILTER_SLOT, ItemBuilder.of(Material.HOPPER, "Filtro: " + filter.name(), NamedTextColor.YELLOW,
                "Click para pasar al", "siguiente tipo.",
                filteredEntries.size() + " asset(s) con este filtro."));

        if (page > 0) {
            setItem(PREV_SLOT, ItemBuilder.of(Material.ARROW, "Página anterior", NamedTextColor.GRAY));
        }

        if (page < totalPages - 1) {
            setItem(NEXT_SLOT, ItemBuilder.of(Material.ARROW, "Página siguiente", NamedTextColor.GRAY));
        }

        setItem(BACK_SLOT, ItemBuilder.backButton());
    }

    private void applyFilter() {

        if (filter == TypeFilter.ALL) {
            filteredEntries = allEntries;
            return;
        }

        List<AssetEntry> filtered = new ArrayList<>();

        for (AssetEntry entry : allEntries) {
            if (entry.type().equalsIgnoreCase(filter.name())) {
                filtered.add(entry);
            }
        }

        filteredEntries = filtered;
    }

    private List<AssetEntry> scanEntries() {

        File mergedDirectory = buildEngine.getMergedDirectory();
        File assetsRoot = new File(mergedDirectory, "assets");

        if (!assetsRoot.isDirectory()) {
            return List.of();
        }

        MergeResult mergeResult = buildEngine.getLastMergeResult();
        List<AssetEntry> entries = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(assetsRoot.toPath())) {

            for (Path path : walk.filter(Files::isRegularFile).toList()) {

                String relativeToAssets = assetsRoot.toPath().relativize(path).toString().replace('\\', '/');
                String type = classify(relativeToAssets);
                long size = path.toFile().length();

                String owner = mergeResult != null
                        ? mergeResult.fileOwners().getOrDefault(relativeToAssets, "?")
                        : "?";

                entries.add(new AssetEntry(relativeToAssets, type, size, owner));
            }

        } catch (IOException ignored) {
        }

        entries.sort((a, b) -> a.relativePath().compareToIgnoreCase(b.relativePath()));
        return entries;
    }

    private String classify(String relativeToAssets) {

        if (relativeToAssets.contains("/models/")) {
            return TypeFilter.MODELS.name();
        } else if (relativeToAssets.contains("/textures/")) {
            return TypeFilter.TEXTURES.name();
        } else if (relativeToAssets.contains("/sounds/") || relativeToAssets.endsWith("sounds.json")) {
            return TypeFilter.SOUNDS.name();
        } else if (relativeToAssets.contains("/font/")) {
            return TypeFilter.FONT.name();
        } else if (relativeToAssets.contains("/lang/")) {
            return TypeFilter.LANG.name();
        } else if (relativeToAssets.contains("/particles/")) {
            return TypeFilter.PARTICLES.name();
        }

        return TypeFilter.OTHER.name();
    }

    private ItemStack buildEntryItem(AssetEntry entry) {

        Material icon = switch (TypeFilter.valueOf(entry.type())) {
            case MODELS -> Material.LEATHER_HELMET;
            case TEXTURES -> Material.PAINTING;
            case SOUNDS -> Material.NOTE_BLOCK;
            case FONT -> Material.WRITABLE_BOOK;
            case LANG -> Material.BOOK;
            case PARTICLES -> Material.BLAZE_POWDER;
            default -> Material.PAPER;
        };

        String fileName = entry.relativePath().substring(entry.relativePath().lastIndexOf('/') + 1);

        return ItemBuilder.of(icon, fileName, NamedTextColor.WHITE,
                "assets/" + entry.relativePath(),
                "tipo: " + entry.type(),
                "módulo(s): " + entry.owner(),
                "tamaño: " + humanSize(entry.sizeBytes()));
    }

    private String humanSize(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }

        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == FILTER_SLOT) {
            TypeFilter[] values = TypeFilter.values();
            filter = values[(filter.ordinal() + 1) % values.length];
            page = 0;
            build();
        } else if (slot == PREV_SLOT) {
            page--;
            build();
        } else if (slot == NEXT_SLOT) {
            page++;
            build();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
