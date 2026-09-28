package com.sack.rpgroll.recipes.index;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.common.recipe.RecipeSource;
import com.sack.rpgroll.common.recipe.RecipeStation;
import com.sack.rpgroll.recipes.Settings;
import com.sack.rpgroll.recipes.read.BrewingReader;
import com.sack.rpgroll.recipes.read.BukkitRecipeReader;
import com.sack.rpgroll.recipes.read.Notes;
import com.sack.rpgroll.recipes.read.YamlRecipeReader;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dueño del índice: lo monta cuando el servidor termina de cargar (todos los plugins ya
 * registraron lo suyo), lo marca como viejo si entra o sale un plugin o un {@link RecipeSource},
 * y lo rehace al abrir el menú si está viejo o si pasó el tiempo de {@code index.refresh-minutes}
 * (una receta editada desde el menú de RPGRoll-Crafting no avisa a nadie).
 */
public final class IndexService implements Listener {

    /** Qué aportó cada origen en el último montaje (para /recetas fuentes). */
    public record Report(Map<String, Integer> bySource, int special, int failed, boolean brewingFromServer,
            List<String> sourceErrors, long millis) {
    }

    private final Plugin plugin;
    private final LangManager lang;
    private Settings settings;
    private RecipeIndex index = RecipeIndex.empty();
    private Report report = new Report(Map.of(), 0, 0, false, List.of(), 0);
    private boolean dirty = true;

    public IndexService(Plugin plugin, LangManager lang, Settings settings) {
        this.plugin = plugin;
        this.lang = lang;
        this.settings = settings;
    }

    public void settings(Settings settings) {
        this.settings = settings;
        this.dirty = true;
    }

    public RecipeIndex current() {
        long age = System.currentTimeMillis() - index.builtAt();
        if (dirty || (settings.refreshMillis() > 0 && age > settings.refreshMillis())) {
            rebuild();
        }
        return index;
    }

    public Report report() {
        return report;
    }

    public void markDirty() {
        dirty = true;
    }

    public RecipeIndex rebuild() {

        long start = System.nanoTime();
        Notes notes = new Notes(lang);
        List<RecipeEntry> entries = new ArrayList<>();
        List<String> sourceErrors = new ArrayList<>();

        BukkitRecipeReader.Result bukkit = new BukkitRecipeReader(plugin.getLogger(), notes).read();
        entries.addAll(bukkit.entries());

        boolean brewingFromServer = false;
        if (settings.brewing()) {
            BrewingReader.Result brewing = new BrewingReader(plugin.getLogger(), notes).read();
            entries.addAll(brewing.entries());
            brewingFromServer = brewing.fromServer();
        }

        for (RegisteredServiceProvider<RecipeSource> registration
                : Bukkit.getServicesManager().getRegistrations(RecipeSource.class)) {
            RecipeSource source = registration.getProvider();
            try {
                Collection<RecipeEntry> provided = source.recipes();
                for (RecipeEntry entry : provided) {
                    entries.add(entry.source().isBlank() ? entry.withSource(source.name()) : entry);
                }
            } catch (RuntimeException | LinkageError e) {
                sourceErrors.add(source.name() + ": " + e);
                plugin.getLogger().warning("✘ El origen de recetas '" + source.name() + "' ("
                        + registration.getPlugin().getName() + ") falló: " + e);
            }
        }

        entries.addAll(new YamlRecipeReader(new File(plugin.getDataFolder(), "extra"), plugin.getLogger()).read());

        List<RecipeEntry> kept = new ArrayList<>(entries.size());
        Set<String> seen = new HashSet<>();
        Map<String, Integer> bySource = new LinkedHashMap<>();
        for (RecipeEntry entry : entries) {
            if (settings.hidden(entry) || !seen.add(entry.source() + "|" + entry.id())) {
                continue;
            }
            kept.add(entry);
            bySource.merge(entry.source(), 1, Integer::sum);
        }

        index = RecipeIndex.build(kept, this::plainName);
        dirty = false;
        long millis = (System.nanoTime() - start) / 1_000_000;
        report = new Report(Map.copyOf(bySource), bukkit.special(), bukkit.failed(), brewingFromServer,
                List.copyOf(sourceErrors), millis);

        plugin.getLogger().info("✔ Recetario: " + kept.size() + " recetas de " + bySource.size() + " orígenes, "
                + index.catalog().size() + " objetos (" + millis + " ms)");
        return index;
    }

    // --- Nombres de estación ---

    public Component displayName(RecipeStation station) {
        if (station.name() != null && !station.name().isBlank()) {
            return ComponentUtils.parse(station.name());
        }
        String key = "station." + station.id();
        String raw = lang.raw(key);
        return ComponentUtils.parse(raw.equals(key) ? station.id() : raw);
    }

    public String plainName(RecipeStation station) {
        return PlainTextComponentSerializer.plainText().serialize(displayName(station));
    }

    // --- Cuándo rehacer ---

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        rebuild();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin() != plugin) {
            dirty = true;
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) {
            dirty = true;
        }
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == RecipeSource.class) {
            dirty = true;
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getService() == RecipeSource.class) {
            dirty = true;
        }
    }
}
