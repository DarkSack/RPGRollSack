package com.sack.rpgroll.fishing;

import com.sack.rpgroll.common.assets.ModuleAssetSync;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.fishing.api.FishingAPI;
import com.sack.rpgroll.fishing.command.FishingAdminCommand;
import com.sack.rpgroll.fishing.command.FishingCommand;
import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.FishingRodManager;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.fishing.engine.FishingCatchEngine;
import com.sack.rpgroll.fishing.engine.FishingConditionsResolver;
import com.sack.rpgroll.fishing.gui.ChatPromptManager;
import com.sack.rpgroll.fishing.listener.FishingCastListener;
import com.sack.rpgroll.fishing.minigame.FishingMinigameManager;
import com.sack.rpgroll.fishing.runtime.FishingProfileManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class FishingPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("species", "rods", "baits", "treasures", "junk",
            "regions");

    private FishSpeciesManager speciesManager;
    private FishingRodManager rodManager;
    private BaitManager baitManager;
    private TreasureManager treasureManager;
    private JunkManager junkManager;
    private FishingRegionManager regionManager;
    private FishingProfileManager profileManager;
    private LangManager langManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new ModuleAssetSync(this, "fishing").syncAll();

        speciesManager = new FishSpeciesManager(this);
        speciesManager.initialize();

        rodManager = new FishingRodManager(this);
        rodManager.initialize();

        baitManager = new BaitManager(this);
        baitManager.initialize();

        treasureManager = new TreasureManager(this);
        treasureManager.initialize();

        junkManager = new JunkManager(this);
        junkManager.initialize();

        regionManager = new FishingRegionManager(this);
        regionManager.initialize();

        profileManager = new FishingProfileManager(this);

        FishingConditionsResolver conditionsResolver = new FishingConditionsResolver(regionManager);

        double treasureChance = getConfig().getDouble("treasure-chance", 0.05);
        double junkChance = getConfig().getDouble("junk-chance", 0.10);
        boolean rpgMode = getConfig().getBoolean("rpg-mode", true);

        FishingCatchEngine catchEngine = new FishingCatchEngine(speciesManager, treasureManager, junkManager,
                conditionsResolver, treasureChance, junkChance);

        FishingAPI.init(speciesManager, rodManager, baitManager, treasureManager, junkManager, regionManager,
                profileManager, catchEngine);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        FishingMinigameManager minigameManager = new FishingMinigameManager(this, langManager);
        getServer().getPluginManager().registerEvents(minigameManager, this);

        getServer().getPluginManager().registerEvents(new FishingCastListener(rodManager, baitManager, catchEngine,
                minigameManager, profileManager, rpgMode, langManager), this);

        var adminCommand = getCommand("fishingadmin");
        if (adminCommand == null) {
            getLogger().severe("✘ El comando 'fishingadmin' no está declarado en plugin.yml");
        } else {
            var fishingAdminCommand = new FishingAdminCommand(speciesManager, rodManager, baitManager,
                    treasureManager, junkManager, regionManager, chatPromptManager, this);
            adminCommand.setExecutor(fishingAdminCommand);
            adminCommand.setTabCompleter(fishingAdminCommand);
        }

        var playerCommand = getCommand("fishing");
        if (playerCommand == null) {
            getLogger().severe("✘ El comando 'fishing' no está declarado en plugin.yml");
        } else {
            var fishingCommand = new FishingCommand(speciesManager, profileManager, langManager);
            playerCommand.setExecutor(fishingCommand);
            playerCommand.setTabCompleter(fishingCommand);
        }

        getLogger().info("✔ RPGRoll-Fishing habilitado (modo " + (rpgMode ? "RPG" : "vanilla") + "). "
                + speciesManager.count() + " especie(s), " + rodManager.count() + " caña(s), "
                + baitManager.count() + " carnada(s).");
    }

    @Override
    public void onDisable() {
        if (profileManager != null) {
            profileManager.saveAll();
        }
    }

    public FishSpeciesManager getSpeciesManager() {
        return speciesManager;
    }

    public FishingRodManager getRodManager() {
        return rodManager;
    }

    public BaitManager getBaitManager() {
        return baitManager;
    }

    public TreasureManager getTreasureManager() {
        return treasureManager;
    }

    public JunkManager getJunkManager() {
        return junkManager;
    }

    public FishingRegionManager getRegionManager() {
        return regionManager;
    }

    public FishingProfileManager getProfileManager() {
        return profileManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}
