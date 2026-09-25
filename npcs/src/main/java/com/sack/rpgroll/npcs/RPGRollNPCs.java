package com.sack.rpgroll.npcs;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.npcs.command.NpcAdminCommand;
import com.sack.rpgroll.npcs.core.NpcActionExecutor;
import com.sack.rpgroll.npcs.core.NpcManager;
import com.sack.rpgroll.npcs.core.NpcMenuManager;
import com.sack.rpgroll.npcs.core.NpcSessionManager;
import com.sack.rpgroll.npcs.core.NpcSpawnManager;
import com.sack.rpgroll.npcs.core.NpcWriter;
import com.sack.rpgroll.npcs.integration.MineSkinClient;
import com.sack.rpgroll.npcs.listener.ChatPromptManager;
import com.sack.rpgroll.npcs.listener.NpcEntityListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RPGRollNPCs extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("npcs", "menus");

    private NpcManager npcManager;
    private NpcSpawnManager spawnManager;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        npcManager = new NpcManager(this);
        npcManager.initialize();

        spawnManager = new NpcSpawnManager(this);

        NpcMenuManager menuManager = new NpcMenuManager(this);
        menuManager.initialize();
        NpcActionExecutor actionExecutor = new NpcActionExecutor(this, menuManager);

        getServer().getPluginManager().registerEvents(
                new NpcEntityListener(npcManager, spawnManager, actionExecutor),
                this);

        // Los mundos ya están cargados cuando se habilitan los plugins; los
        // NPCs en chunks aún sin cargar aparecen con el ChunkLoadEvent.
        spawnManager.respawnAll(npcManager.getAll());

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);

        getServer().getPluginManager()
                .registerEvents(chatPromptManager, this);

        MineSkinClient mineSkinClient = new MineSkinClient(this);

        NpcSessionManager sessionManager = new NpcSessionManager();

        NpcWriter writer = new NpcWriter(this);

        NpcAdminCommand adminCommand = new NpcAdminCommand(
                this,
                npcManager,
                spawnManager,
                sessionManager,
                chatPromptManager,
                mineSkinClient,
                writer,
                menuManager,
                langManager);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "npc",
                "Gestiona NPCs", "rpgrollnpcs.admin.*", adminCommand);

        getLogger().info(
                "✔ RPGRoll-NPCs habilitado. "
                        + npcManager.count()
                        + " NPC(s) cargados.");
    }

    @Override
    public void onDisable() {
        if (spawnManager != null) {
            spawnManager.despawnAll();
        }
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }

    public NpcSpawnManager getSpawnManager() {
        return spawnManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }
}