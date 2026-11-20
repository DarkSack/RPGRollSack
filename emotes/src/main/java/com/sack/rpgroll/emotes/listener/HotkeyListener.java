package com.sack.rpgroll.emotes.listener;

import com.sack.rpgroll.emotes.EmotesSettings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shift + F (cambiar de mano estando agachado) abre el menú de emotes en vez de cambiar el objeto de mano.
 * Sin agacharse, sin permiso o con {@code hotkeys.sneak-swap: false} no toca nada.
 */
public class HotkeyListener implements Listener {

    private final Supplier<EmotesSettings> settings;
    private final Consumer<Player> openMenu;

    /** @param openMenu el mismo que usa {@code /emotes} */
    public HotkeyListener(Supplier<EmotesSettings> settings, Consumer<Player> openMenu) {
        this.settings = settings;
        this.openMenu = openMenu;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player p = event.getPlayer();
        if (settings.get().sneakSwapHotkey() && p.isSneaking() && p.hasPermission("rpgrollemotes.use")) {
            event.setCancelled(true);
            openMenu.accept(p);
        }
    }
}
