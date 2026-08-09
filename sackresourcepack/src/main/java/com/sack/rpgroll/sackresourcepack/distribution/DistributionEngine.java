package com.sack.rpgroll.sackresourcepack.distribution;

import com.sack.rpgroll.sackresourcepack.event.PackSentEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manda el pack actual a cada jugador que entra (y a todos, bajo pedido,
 * tras un rebuild) — no reimplementa el protocolo, solo llama a {@code
 * Player#setResourcePack}. Trackea el último status reportado por
 * jugador para {@code /srp status}, y opcionalmente expulsa a quien
 * rechaza un pack marcado como obligatorio.
 */
public class DistributionEngine implements Listener {

    /** Copia local del mismo parser "inteligente" que usa el resto del ecosistema (core no es una dependencia acá). */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private final Plugin plugin;
    private final boolean required;
    private final String promptMessage;

    private String currentUrl;
    private byte[] currentSha1Bytes;
    private final Map<UUID, PlayerResourcePackStatusEvent.Status> lastStatus = new HashMap<>();

    public DistributionEngine(Plugin plugin, boolean required, String promptMessage) {
        this.plugin = plugin;
        this.required = required;
        this.promptMessage = promptMessage;
    }

    public void updateCurrentPack(String url, String sha1Hex) {
        this.currentUrl = url;
        this.currentSha1Bytes = hexToBytes(sha1Hex);
    }

    public boolean hasPack() {
        return currentUrl != null && !currentUrl.isBlank();
    }

    public void send(Player player) {

        if (!hasPack()) {
            return;
        }

        Component prompt = parseMessage(promptMessage);
        player.setResourcePack(currentUrl, currentSha1Bytes, prompt, required);

        Bukkit.getPluginManager().callEvent(new PackSentEvent(player));
    }

    public void sendToAllOnline() {
        Bukkit.getOnlinePlayers().forEach(this::send);
    }

    public PlayerResourcePackStatusEvent.Status statusFor(UUID uuid) {
        return lastStatus.get(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        send(event.getPlayer());
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {

        lastStatus.put(event.getPlayer().getUniqueId(), event.getStatus());

        if (required && event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {
            event.getPlayer().kick(parseMessage("&cEste servidor requiere aceptar el resource pack."));
        }
    }

    /** Soporta MiniMessage (&lt;gradient:...&gt;, etc.), legacy clásico y legacy hex (&amp;#RRGGBB / &amp;x&amp;R&amp;R...). */
    private static Component parseMessage(String text) {

        if (text == null || text.isBlank()) {
            return Component.empty();
        }

        if (text.indexOf('<') >= 0 && text.indexOf('>') >= 0) {
            return MINI_MESSAGE.deserialize(text);
        }

        return LEGACY.deserialize(text);
    }

    private byte[] hexToBytes(String hex) {

        if (hex == null || hex.isBlank()) {
            return new byte[0];
        }

        int length = hex.length();
        byte[] bytes = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }

        return bytes;
    }

}
