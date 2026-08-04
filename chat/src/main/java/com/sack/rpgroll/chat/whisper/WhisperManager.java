package com.sack.rpgroll.chat.whisper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Mensajes privados — spec "Responder / Ignorar / Historial / Espías para Staff". */
public class WhisperManager {

    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();
    private final Set<UUID> socialSpyEnabled = ConcurrentHashMap.newKeySet();

    public void send(Player sender, Player target, String message) {

        Component toSender = Component.text("Tú -> " + target.getName() + ": ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(message, NamedTextColor.WHITE));
        Component toTarget = Component.text(sender.getName() + " -> Tú: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(message, NamedTextColor.WHITE));

        sender.sendMessage(toSender);
        target.sendMessage(toTarget);

        lastPartner.put(sender.getUniqueId(), target.getUniqueId());
        lastPartner.put(target.getUniqueId(), sender.getUniqueId());

        if (!socialSpyEnabled.isEmpty()) {

            Component spyLine = Component.text("[Spy] " + sender.getName() + " -> " + target.getName() + ": ",
                    NamedTextColor.DARK_GRAY).append(Component.text(message, NamedTextColor.GRAY));

            for (UUID spyId : socialSpyEnabled) {
                if (spyId.equals(sender.getUniqueId()) || spyId.equals(target.getUniqueId())) {
                    continue;
                }
                Player spy = Bukkit.getPlayer(spyId);
                if (spy != null) {
                    spy.sendMessage(spyLine);
                }
            }
        }
    }

    public UUID lastPartner(UUID playerId) {
        return lastPartner.get(playerId);
    }

    public boolean toggleSocialSpy(UUID playerId) {
        if (!socialSpyEnabled.remove(playerId)) {
            socialSpyEnabled.add(playerId);
            return true;
        }
        return false;
    }

}
