package com.sack.rpgroll.chat.listener;

import com.sack.rpgroll.chat.ignore.IgnoreManager;
import com.sack.rpgroll.chat.language.LanguageService;
import com.sack.rpgroll.chat.player.PlayerChannelStateManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Siembra idiomas por raza al entrar, y guarda/descarga el estado por jugador al salir. */
public class PlayerSessionListener implements Listener {

    private final PlayerChannelStateManager channelStateManager;
    private final IgnoreManager ignoreManager;
    private final LanguageService languageService;

    public PlayerSessionListener(PlayerChannelStateManager channelStateManager, IgnoreManager ignoreManager,
            LanguageService languageService) {
        this.channelStateManager = channelStateManager;
        this.ignoreManager = ignoreManager;
        this.languageService = languageService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        channelStateManager.getOrLoad(event.getPlayer());
        languageService.resolve(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        channelStateManager.unload(uuid);
        ignoreManager.unload(uuid);
        languageService.unload(uuid);
    }

}
