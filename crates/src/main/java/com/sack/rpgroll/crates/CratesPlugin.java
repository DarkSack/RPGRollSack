package com.sack.rpgroll.crates;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.crates.command.CrateAdminCommand;
import com.sack.rpgroll.crates.core.Crate;
import com.sack.rpgroll.crates.core.CrateActionExecutor;
import com.sack.rpgroll.crates.core.CrateManager;
import com.sack.rpgroll.crates.gui.ChatPromptManager;
import com.sack.rpgroll.crates.hologram.DecentHologramsHook;
import com.sack.rpgroll.crates.key.CrateKeyItem;
import com.sack.rpgroll.crates.listener.CrateInteractListener;
import com.sack.rpgroll.crates.location.PlacedCrateManager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CratesPlugin extends JavaPlugin {

    private static final double HOLOGRAM_Y_OFFSET = 2.0;

    private static final List<String> DIRECTORIES = List.of("crates");

    private CrateManager crateManager;
    private PlacedCrateManager placedCrateManager;
    private DecentHologramsHook hologramsHook;
    private LangManager langManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        crateManager = new CrateManager(this);
        crateManager.initialize();

        placedCrateManager = new PlacedCrateManager(this);
        placedCrateManager.load();

        hologramsHook = new DecentHologramsHook(this);
        CrateKeyItem crateKeyItem = new CrateKeyItem(this, langManager);
        CrateActionExecutor actionExecutor = new CrateActionExecutor(this, langManager);

        getServer().getPluginManager().registerEvents(
                new CrateInteractListener(this, crateManager, placedCrateManager, crateKeyItem, actionExecutor,
                        langManager),
                this);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        var crateCommand = getCommand("crate");
        if (crateCommand == null) {
            getLogger().severe("✘ El comando 'crate' no está declarado en plugin.yml");
        } else {
            var crateAdminCommand = new CrateAdminCommand(this, crateManager, placedCrateManager, hologramsHook,
                    crateKeyItem, chatPromptManager, langManager);
            crateCommand.setExecutor(crateAdminCommand);
            crateCommand.setTabCompleter(crateAdminCommand);
        }

        rebuildHolograms();

        getLogger().info("✔ RPGRoll-Crates habilitado. " + crateManager.count() + " tipo(s) de crate cargados, "
                + placedCrateManager.getAll().size() + " ubicación(es).");
    }

    /** Recrea todos los hologramas al arrancar (DecentHolograms no los persiste entre reinicios). */
    private void rebuildHolograms() {

        for (var placed : placedCrateManager.getAll()) {

            crateManager.get(placed.crateId()).ifPresent((Crate crate) -> {

                World world = getServer().getWorld(placed.world());
                if (world == null) {
                    getLogger().warning("✘ Mundo no encontrado para el crate '" + placed.placementId() + "': "
                            + placed.world());
                    return;
                }

                Location location = new Location(
                        world, placed.x() + 0.5, placed.y() + HOLOGRAM_Y_OFFSET, placed.z() + 0.5);

                hologramsHook.createOrUpdate(placed.hologramName(), location, crate.hologramLines());
            });
        }
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }

    public PlacedCrateManager getPlacedCrateManager() {
        return placedCrateManager;
    }

    public DecentHologramsHook getHologramsHook() {
        return hologramsHook;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}
