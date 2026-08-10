package com.sack.rpgroll.tab.profile;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga los perfiles desde plugins/RPGRoll-TAB/profiles/*.yml. */
public class ProfileManager extends ContentManager<TABProfile> {

    public static final String DEFAULT_PROFILE_ID = "default";

    public ProfileManager(JavaPlugin tabPlugin) {
        super(resolveCoreInstance(), new YamlLoader(tabPlugin), "profiles", "perfil", new ProfileParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
