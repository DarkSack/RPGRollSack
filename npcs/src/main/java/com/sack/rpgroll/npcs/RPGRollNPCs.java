package com.sack.rpgroll.npcs;

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
import com.sack.rpgroll.npcs.listener.NpcInteractListener;
import com.sack.rpgroll.npcs.listener.NpcVisibilityListener;
import com.sack.rpgroll.npcs.render.FakePlayerRenderer;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RPGRollNPCs extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("npcs", "menus");

    private NpcManager npcManager;
    private NpcSpawnManager spawnManager;

    @Override
    public void onEnable() {

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        npcManager = new NpcManager(this);
        npcManager.initialize();

        FakePlayerRenderer renderer = new FakePlayerRenderer(this);
        spawnManager = new NpcSpawnManager(this, renderer);

        npcManager.getAll().forEach(spawnManager::register);

        getServer().getPluginManager().registerEvents(
                new NpcVisibilityListener(npcManager, spawnManager),
                this);

        NpcMenuManager menuManager = new NpcMenuManager(this);
        menuManager.initialize();
        NpcActionExecutor actionExecutor = new NpcActionExecutor(this, menuManager);

        NpcInteractListener interactListener = new NpcInteractListener(
                this,
                npcManager,
                spawnManager,
                actionExecutor);

        interactListener.register();

        ChatPromptManager chatPromptManager = new ChatPromptManager(this);

        getServer().getPluginManager()
                .registerEvents(chatPromptManager, this);

        MineSkinClient mineSkinClient = new MineSkinClient(this);

        NpcSessionManager sessionManager = new NpcSessionManager();

        NpcWriter writer = new NpcWriter(this);

        NpcAdminCommand adminCommand = new NpcAdminCommand(
                npcManager,
                spawnManager,
                sessionManager,
                chatPromptManager,
                mineSkinClient,
                writer,
                menuManager);

        var npcCommand = getCommand("npc");
        if (npcCommand == null) {
            getLogger().severe("✘ El comando 'npc' no está declarado en plugin.yml");
        } else {
            npcCommand.setExecutor(adminCommand);
            npcCommand.setTabCompleter(adminCommand);
        }

        getLogger().info(
                "✔ RPGRoll-NPCs habilitado. "
                        + npcManager.count()
                        + " NPC(s) cargados.");
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }

    public NpcSpawnManager getSpawnManager() {
        return spawnManager;
    }
}