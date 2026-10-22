package com.sack.rpgroll.worldforge.gui;

import com.sack.rpgroll.worldforge.core.WorldForgePlugin;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Menu plumbing: opening on the player's own thread (Folia), cancelling every click in a
 * WorldForge menu, permission per click, stale-menu detection and one-line chat prompts.
 */
public final class Gui implements Listener {

    private record Prompt(Consumer<String> answer, long expires) {
    }

    private final WorldForgePlugin plugin;
    private final AtomicLong generation = new AtomicLong();
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();
    private final Screens screens;

    public Gui(WorldForgePlugin plugin) {
        this.plugin = plugin;
        this.screens = new Screens(plugin, this);
    }

    public void openMain(Player p) {
        open(p, screens.main(p));
    }

    Menu menu(Player p, String title, int rows) {
        return new Menu(p, generation.get(), title, rows);
    }

    void open(Player p, Menu m) {
        plugin.platform().entity(p, () -> p.openInventory(m.getInventory()));
    }

    void close(Player p) {
        plugin.platform().entity(p, p::closeInventory);
    }

    void tell(Player p, String text, NamedTextColor color) {
        plugin.messages().sendPlain(p, Component.text(text, color));
    }

    /** Closes the menu and waits for one chat line ("cancelar" aborts, 60 s limit). */
    void ask(Player p, String question, Consumer<String> answer) {
        close(p);
        prompts.put(p.getUniqueId(), new Prompt(answer, System.currentTimeMillis() + 60_000));
        tell(p, question + " (escríbelo en el chat; «cancelar» para salir)", NamedTextColor.YELLOW);
    }

    /** Called after a reload: menus opened before it stop working and are closed. */
    public void invalidate() {
        generation.incrementAndGet();
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.platform().entity(p, () -> {
                if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
                    p.closeInventory();
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Menu m)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(m.owner())
                || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        Consumer<Player> action = m.action(e.getRawSlot());
        if (action == null) {
            return;
        }
        if (m.generation() != generation.get()) {
            p.closeInventory();
            tell(p, "Este menú es de antes de recargar WorldForge; ábrelo otra vez con /wf gui.", NamedTextColor.RED);
            return;
        }
        String permission = m.permission(e.getRawSlot());
        if (permission != null && !p.hasPermission(permission)) {
            plugin.messages().send(p, "no-permission", "permission", permission);
            return;
        }
        try {
            action.accept(p);
        } catch (RuntimeException ex) {
            plugin.messages().send(p, "failed", "reason", ex.toString());
            plugin.getLogger().warning("GUI: " + ex);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Menu) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Prompt prompt = prompts.remove(e.getPlayer().getUniqueId());
        if (prompt == null) {
            return;
        }
        e.setCancelled(true);
        Player p = e.getPlayer();
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if (System.currentTimeMillis() > prompt.expires()) {
            tell(p, "Tardaste demasiado; vuelve a intentarlo desde /wf gui.", NamedTextColor.RED);
            return;
        }
        if (text.equalsIgnoreCase("cancelar")) {
            tell(p, "Cancelado.", NamedTextColor.GRAY);
            return;
        }
        // Chat runs off the player's thread: hop back before touching menus or the world.
        plugin.platform().entity(p, () -> prompt.answer().accept(text));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        prompts.remove(e.getPlayer().getUniqueId());
    }
}
