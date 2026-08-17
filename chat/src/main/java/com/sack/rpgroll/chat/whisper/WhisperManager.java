package com.sack.rpgroll.chat.whisper;

import com.sack.rpgroll.common.lang.LangManager;

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
    private final LangManager lang;

    public WhisperManager(LangManager lang) {
        this.lang = lang;
    }

    public void send(Player sender, Player target, String message) {

        lang.send(sender, "whisper.to_sender", "target", target.getName(), "message", message);
        lang.send(target, "whisper.to_target", "sender", sender.getName(), "message", message);

        lastPartner.put(sender.getUniqueId(), target.getUniqueId());
        lastPartner.put(target.getUniqueId(), sender.getUniqueId());

        if (!socialSpyEnabled.isEmpty()) {

            for (UUID spyId : socialSpyEnabled) {
                if (spyId.equals(sender.getUniqueId()) || spyId.equals(target.getUniqueId())) {
                    continue;
                }
                Player spy = Bukkit.getPlayer(spyId);
                if (spy != null) {
                    lang.send(spy, "whisper.spy_line", "sender", sender.getName(), "target", target.getName(),
                            "message", message);
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
