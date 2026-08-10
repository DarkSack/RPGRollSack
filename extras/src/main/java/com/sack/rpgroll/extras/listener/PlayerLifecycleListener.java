package com.sack.rpgroll.extras.listener;

import com.sack.rpgroll.extras.activity.ActivityStateResolver;
import com.sack.rpgroll.extras.condition.ConditionRuntime;
import com.sack.rpgroll.extras.stat.StatEngine;
import com.sack.rpgroll.extras.temperature.BodyTemperatureEngine;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLifecycleListener implements Listener {

    private final StatEngine statEngine;
    private final ConditionRuntime conditionRuntime;
    private final BodyTemperatureEngine temperatureEngine;
    private final ActivityStateResolver activityStateResolver;

    public PlayerLifecycleListener(StatEngine statEngine, ConditionRuntime conditionRuntime,
            BodyTemperatureEngine temperatureEngine, ActivityStateResolver activityStateResolver) {
        this.statEngine = statEngine;
        this.conditionRuntime = conditionRuntime;
        this.temperatureEngine = temperatureEngine;
        this.activityStateResolver = activityStateResolver;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        statEngine.initializePlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        statEngine.clear(event.getPlayer());
        conditionRuntime.clear(event.getPlayer());
        temperatureEngine.clear(event.getPlayer());
        activityStateResolver.clear(event.getPlayer());
    }

}
