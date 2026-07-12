package com.sack.rpgroll;

import com.sack.rpgroll.core.Bootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class RPGRoll extends JavaPlugin {

    private Bootstrap bootstrap;

    @Override
    public void onEnable() {

        bootstrap = new Bootstrap(this);

        bootstrap.initialize();

    }

    @Override
    public void onDisable() {

        if (bootstrap != null) {
            bootstrap.shutdown();
        }

    }

}