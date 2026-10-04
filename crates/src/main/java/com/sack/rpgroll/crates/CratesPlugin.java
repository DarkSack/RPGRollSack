package com.sack.rpgroll.crates;

import com.sack.rpgroll.crates.lucky.LuckyCommand;
import com.sack.rpgroll.crates.lucky.LuckyExecutor;
import com.sack.rpgroll.crates.lucky.LuckyItems;
import com.sack.rpgroll.crates.lucky.LuckyListener;
import com.sack.rpgroll.crates.lucky.LuckyManager;
import com.sack.rpgroll.crates.lucky.LuckyStore;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

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

    private static final List<String> DIRECTORIES = List.of("crates", "lucky");

    private CrateManager crateManager;
    private PlacedCrateManager placedCrateManager;
    private DecentHologramsHook hologramsHook;
    private LangManager langManager;
    private LuckyManager luckyManager;
    private LuckyExecutor luckyExecutor;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


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
        luckyManager = new LuckyManager(this);
        luckyManager.initialize();
        LuckyItems luckyItems = new LuckyItems(this, luckyManager);

        CrateActionExecutor actionExecutor = new CrateActionExecutor(this, langManager, luckyManager, luckyItems);

        getServer().getPluginManager().registerEvents(
                new CrateInteractListener(this, crateManager, placedCrateManager, crateKeyItem, actionExecutor,
                        langManager),
                this);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        var crateAdminCommand = new CrateAdminCommand(this, crateManager, placedCrateManager, hologramsHook,
                    crateKeyItem, chatPromptManager, langManager);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "crate",
                "Gestiona crates", "rpgrollcrates.admin.*", crateAdminCommand);

        enableLuckyBlocks(luckyItems);

        rebuildHolograms();

        getLogger().info("✔ RPGRoll-Crates habilitado. " + crateManager.count() + " tipo(s) de crate cargados, "
                + placedCrateManager.getAll().size() + " ubicación(es), " + luckyManager.count()
                + " lucky block(s).");
    }

    @Override
    public void onDisable() {
        // Las ruletas a medias se entregan antes de que se guarden los inventarios.
        com.sack.rpgroll.crates.gui.CrateSpinGUI.finishAll();
        if (luckyExecutor != null) {
            luckyExecutor.restoreAll();
        }
    }

    /** Lucky blocks: bloques musicales de esqueleto que se abren al romperlos (lucky/*.yml). */
    private void enableLuckyBlocks(LuckyItems luckyItems) {

        Set<String> disabledWorlds = new HashSet<>();
        getConfig().getStringList("lucky-blocks.disabled-worlds")
                .forEach(world -> disabledWorlds.add(world.toLowerCase(Locale.ROOT)));

        luckyExecutor = new LuckyExecutor(this, langManager, luckyManager, luckyItems);
        LuckyListener luckyListener = new LuckyListener(this, luckyManager, luckyItems, new LuckyStore(this),
                luckyExecutor, Set.copyOf(disabledWorlds));

        getServer().getPluginManager().registerEvents(luckyExecutor, this);
        getServer().getPluginManager().registerEvents(luckyListener, this);

        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "lucky", "Gestiona los lucky blocks",
                LuckyCommand.PERMISSION, new LuckyCommand(luckyManager, luckyItems, luckyListener, langManager));
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

    public LuckyManager getLuckyManager() {
        return luckyManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}
