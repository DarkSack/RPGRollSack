package com.sack.rpgroll.magic;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.magic.api.MagicAPI;
import com.sack.rpgroll.magic.command.MagicAdminCommand;
import com.sack.rpgroll.magic.command.MagicCommand;
import com.sack.rpgroll.magic.core.CatalystManager;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.SchoolManager;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.engine.SpellCastEngine;
import com.sack.rpgroll.magic.gui.ChatPromptManager;
import com.sack.rpgroll.magic.listener.GrimoireListener;
import com.sack.rpgroll.magic.listener.SpellCastListener;
import com.sack.rpgroll.magic.listener.SpellChannelManager;
import com.sack.rpgroll.magic.runtime.SpellbookManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class MagicPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("schools", "spells", "grimoires", "runes", "catalysts");

    private SchoolManager schoolManager;
    private SpellManager spellManager;
    private GrimoireManager grimoireManager;
    private RuneManager runeManager;
    private CatalystManager catalystManager;
    private SpellbookManager spellbookManager;
    private SpellCastEngine engine;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        schoolManager = new SchoolManager(this);
        schoolManager.initialize();

        spellManager = new SpellManager(this);
        spellManager.initialize();

        grimoireManager = new GrimoireManager(this);
        grimoireManager.initialize();

        runeManager = new RuneManager(this);
        runeManager.initialize();

        catalystManager = new CatalystManager(this);
        catalystManager.initialize();

        spellbookManager = new SpellbookManager(this);

        engine = new SpellCastEngine(this, schoolManager, runeManager, langManager);

        MagicAPI.init(schoolManager, spellManager, grimoireManager, runeManager, catalystManager, spellbookManager,
                engine);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        SpellChannelManager channelManager = new SpellChannelManager(this, engine, langManager);
        getServer().getPluginManager().registerEvents(channelManager, this);

        getServer().getPluginManager().registerEvents(
                new SpellCastListener(spellManager, catalystManager, spellbookManager, engine, channelManager,
                        langManager),
                this);

        getServer().getPluginManager().registerEvents(
                new GrimoireListener(grimoireManager, spellManager, spellbookManager, langManager), this);

        var magicAdminCommand = new MagicAdminCommand(schoolManager, spellManager, grimoireManager, runeManager,
                    catalystManager, chatPromptManager, langManager);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "magicadmin",
                "Comandos administrativos de RPGRoll-Magic (Magic Studio)", "rpgrollmagic.admin.*", magicAdminCommand);

        var magicCommand = new MagicCommand(spellManager, runeManager, spellbookManager, engine,
                    chatPromptManager);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "magic",
                "Comandos de jugador de RPGRoll-Magic", "rpgrollmagic.use", magicCommand);

        getLogger().info("✔ RPGRoll-Magic habilitado. " + schoolManager.count() + " escuela(s), "
                + spellManager.count() + " hechizo(s), " + grimoireManager.count() + " grimorio(s), "
                + runeManager.count() + " runa(s), " + catalystManager.count() + " catalizador(es).");
    }

    @Override
    public void onDisable() {
        if (spellbookManager != null) {
            spellbookManager.saveAll();
        }
    }

    public SchoolManager getSchoolManager() {
        return schoolManager;
    }

    public SpellManager getSpellManager() {
        return spellManager;
    }

    public GrimoireManager getGrimoireManager() {
        return grimoireManager;
    }

    public RuneManager getRuneManager() {
        return runeManager;
    }

    public CatalystManager getCatalystManager() {
        return catalystManager;
    }

    public SpellbookManager getSpellbookManager() {
        return spellbookManager;
    }

    public SpellCastEngine getEngine() {
        return engine;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}
