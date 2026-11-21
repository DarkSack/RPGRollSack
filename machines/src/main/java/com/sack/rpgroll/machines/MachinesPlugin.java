package com.sack.rpgroll.machines;

import com.sack.rpgroll.machines.area.AreaKind;
import com.sack.rpgroll.machines.area.AreaListener;
import com.sack.rpgroll.machines.area.AreaMenu;
import com.sack.rpgroll.machines.area.AreaService;
import com.sack.rpgroll.machines.area.AreaStore;
import com.sack.rpgroll.machines.area.AreaWorker;
import com.sack.rpgroll.machines.area.FarmKind;
import com.sack.rpgroll.machines.area.FisherKind;
import com.sack.rpgroll.machines.area.LumberKind;
import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.machines.command.MachinesCommand;
import com.sack.rpgroll.machines.core.Displays;
import com.sack.rpgroll.machines.furnace.FurnaceListener;
import com.sack.rpgroll.machines.furnace.FurnaceMenu;
import com.sack.rpgroll.machines.furnace.FurnaceService;
import com.sack.rpgroll.machines.furnace.FurnaceSettings;
import com.sack.rpgroll.machines.quarry.Claims;
import com.sack.rpgroll.machines.quarry.QuarryListener;
import com.sack.rpgroll.machines.quarry.QuarryMenu;
import com.sack.rpgroll.machines.quarry.QuarryService;
import com.sack.rpgroll.machines.quarry.QuarrySettings;
import com.sack.rpgroll.machines.quarry.QuarryStore;
import com.sack.rpgroll.machines.quarry.QuarryWorker;
import com.sack.rpgroll.machines.spawner.SpawnerData;
import com.sack.rpgroll.machines.spawner.SpawnerListener;
import com.sack.rpgroll.machines.spawner.SpawnerMenu;
import com.sack.rpgroll.machines.spawner.SpawnerService;
import com.sack.rpgroll.machines.spawner.SpawnerSettings;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/**
 * RPGRoll-Machines: hornos, altos hornos y ahumadores mejorables (furnaces.yml), spawners con
 * mejoras, pila y recogida con toque de seda (spawners.yml) y canteras que excavan solas
 * (quarries.yml).
 */
public class MachinesPlugin extends JavaPlugin {

    private static final List<String> FILES = List.of("furnaces.yml", "spawners.yml", "quarries.yml",
            "fishers.yml", "farms.yml", "lumberjacks.yml");

    private LangManager lang;
    private FurnaceService furnaces;
    private SpawnerService spawners;
    private QuarryService quarries;
    private QuarryStore store;
    private QuarryWorker worker;
    private BukkitTask workerTask;
    private BukkitTask saveTask;
    private AreaService areas;
    private AreaStore areaStore;
    private BukkitTask areaTask;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        for (String file : FILES) {
            if (!new File(getDataFolder(), file).exists()) {
                saveResource(file, false);
            }
        }
        lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        lang.reload(getConfig().getString("language", "es"));
        com.sack.rpgroll.gui.MenuSkin.register(com.sack.rpgroll.machines.core.MachineMenus.MODULE,
                com.sack.rpgroll.gui.MenuSkin.from(getConfig().getConfigurationSection("menus")));

        Displays displays = new Displays(this);
        furnaces = new FurnaceService(this, lang, displays, FurnaceSettings.from(load("furnaces.yml"), this::warn));
        spawners = new SpawnerService(this, lang, displays, SpawnerSettings.from(load("spawners.yml"), this::warn));
        store = new QuarryStore(this);
        store.load();
        quarries = new QuarryService(this, lang, displays, store, new Claims(getLogger()),
                QuarrySettings.from(load("quarries.yml"), this::warn));
        worker = new QuarryWorker(quarries);
        List<AreaKind> kinds = List.of(new FisherKind(), new FarmKind(), new LumberKind());
        kinds.forEach(kind -> kind.load(load(kind.file()), this::warn));
        areaStore = new AreaStore(new File(getDataFolder(), "data/machines.yml"), getLogger());
        areaStore.load();
        areas = new AreaService(this, lang, displays, areaStore, quarries.claims(), kinds);
        AreaWorker areaWorker = new AreaWorker(areas);

        var events = getServer().getPluginManager();
        events.registerEvents(new FurnaceListener(this, furnaces,
                (player, block) -> new FurnaceMenu(player, block, furnaces, lang).open()), this);
        events.registerEvents(new SpawnerListener(spawners, lang, displays, (player, block) -> spawners.spawner(block)
                .ifPresent(s -> new SpawnerMenu(player, block, spawners, lang, s.getSpawnedType()).open())), this);
        events.registerEvents(new QuarryListener(quarries, lang,
                (player, quarry) -> new QuarryMenu(player, quarry, quarries, lang).open()), this);
        events.registerEvents(worker, this);
        events.registerEvents(new AreaListener(areas, lang, (player, machine) -> areas.kind(machine.kind())
                .ifPresent(kind -> new AreaMenu(player, machine, kind, areas, lang).open())), this);
        events.registerEvents(areaWorker, this);

        BrigadierCommands.register(this, "machines", "Máquinas de RPGRoll: dar, ver y recargar",
                List.of("maquinas", "rpgmachines"),
                new MachinesCommand(this, lang, furnaces, spawners, quarries, areas), null, MachinesCommand.PERMISSION);

        // La receta de la cantera ya: RPGRoll-Recipes monta su índice al cargar el servidor (antes del
        // primer tick) y una receta registrada después no sale en el recetario hasta /recetas recargar.
        quarries.registerRecipe();
        areas.registerRecipes();
        // Las de horno (para el autofundido), cuando todos los plugins ya registraron las suyas.
        getServer().getScheduler().runTask(this, worker::reloadRecipes);
        workerTask = getServer().getScheduler().runTaskTimer(this, worker, 20L, 1L);
        areaTask = getServer().getScheduler().runTaskTimer(this, areaWorker, 20L, 1L);
        saveTask = getServer().getScheduler().runTaskTimer(this, () -> {
            store.save(false);
            areaStore.save(false);
        }, 200L, 200L);
        checkClaims();
        registerQuickGive();
    }

    /** /dar spawner:<mob> y /dar maquina:<id> (comando de la Lib). */
    private void registerQuickGive() {
        com.sack.rpgroll.common.quick.Things.give(this, "spawner", () -> java.util.Arrays.stream(EntityType.values())
                .filter(t -> t.isSpawnable() && t.isAlive() && t != EntityType.PLAYER)
                .map(t -> t.getKey().getKey()).toList(), this::createSpawner);
        com.sack.rpgroll.common.quick.Things.give(this, "maquina", this::machineIds, this::createMachine);
    }

    /** cantera, las máquinas de área por su id y los hornos como {@code <horno>.<nivel>}. */
    private java.util.List<String> machineIds() {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add("cantera");
        areas.kinds().forEach(k -> out.add(k.id()));
        for (org.bukkit.Material kind : furnaces.settings().kinds()) {
            furnaces.settings().tiers().forEach(t -> out.add(kind.getKey().getKey() + "." + t.id()));
        }
        return out;
    }

    private Optional<ItemStack> createMachine(String id) {
        if (id.equals("cantera")) {
            return Optional.of(quarries.item(null, 1));
        }
        int dot = id.lastIndexOf('.');
        if (dot > 0) {
            org.bukkit.Material kind = org.bukkit.Material.matchMaterial(id.substring(0, dot));
            return kind == null || !furnaces.settings().kinds().contains(kind) ? Optional.empty()
                    : furnaces.settings().tier(id.substring(dot + 1)).map(t -> furnaces.item(kind, t, 1));
        }
        return areas.kind(id).map(k -> areas.item(k, null, 1));
    }

    @Override
    public void onDisable() {
        if (workerTask != null) {
            workerTask.cancel();
        }
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (areaTask != null) {
            areaTask.cancel();
        }
        if (store != null) {
            store.save(true);
        }
        if (quarries != null) {
            quarries.unregisterRecipe();
        }
        if (areaStore != null) {
            areaStore.save(true);
        }
        if (areas != null) {
            areas.unregisterRecipes();
        }
    }

    /** /machines reload: config, idioma y los ficheros de cada máquina. Las canteras y las demás siguen donde iban. */
    /**
     * Un spawner nuestro (sin mejoras) del mob {@code mob} ({@code minecraft:zombie} o {@code zombie}), para otros
     * plugins (la tienda de Economy). Vacío si el mob no existe, no puede salir de un spawner o Machines no ha
     * arrancado.
     */
    public Optional<ItemStack> createSpawner(String mob) {

        if (spawners == null || mob == null) {
            return Optional.empty();
        }
        EntityType type = SpawnerService.entity(mob.contains(":") ? mob : "minecraft:" + mob);
        if (type == null || !type.isSpawnable() || !type.isAlive()) {
            return Optional.empty();
        }
        return Optional.of(spawners.item(type, SpawnerData.NONE, 1));
    }

    public void reload() {
        reloadConfig();
        lang.reload(getConfig().getString("language", "es"));
        com.sack.rpgroll.gui.MenuSkin.register(com.sack.rpgroll.machines.core.MachineMenus.MODULE,
                com.sack.rpgroll.gui.MenuSkin.from(getConfig().getConfigurationSection("menus")));
        furnaces.settings(FurnaceSettings.from(load("furnaces.yml"), this::warn));
        spawners.settings(SpawnerSettings.from(load("spawners.yml"), this::warn));
        quarries.settings(QuarrySettings.from(load("quarries.yml"), this::warn));
        quarries.registerRecipe();
        areas.kinds().forEach(kind -> kind.load(load(kind.file()), this::warn));
        areas.registerRecipes();
        worker.reloadRecipes();
        checkClaims();
    }

    private void checkClaims() {
        if (quarries.settings().enabled() && quarries.settings().requireClaim() && !quarries.claims().available()) {
            getLogger().warning("• quarries.yml pide claims (require-claim) pero GriefPrevention no está: "
                    + "las canteras se podrán poner en cualquier sitio.");
        }
    }

    /**
     * Un fichero de la carpeta del plugin, sin el del jar como respaldo: si no, los niveles de
     * serie se mezclarían con los propios (las secciones del respaldo suman claves).
     */
    private FileConfiguration load(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name));
    }

    private void warn(String message) {
        getLogger().warning("✘ " + message);
    }
}
