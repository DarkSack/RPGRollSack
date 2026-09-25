package com.sack.rpgroll.pass.vote;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * Escucha {@code VotifierEvent} sin compilar contra Votifier: su API no está
 * en ningún repositorio Maven estable y el evento lleva años sin cambiar.
 * Sirve con NuVotifier y con sus forks (AzuVotifier).
 */
public final class VotifierBridge implements Listener {

    private static final String EVENT_CLASS = "com.vexsoftware.votifier.model.VotifierEvent";

    private VotifierBridge() {
    }

    /** @return true si encontró Votifier y registró el listener */
    @SuppressWarnings("unchecked")
    public static boolean register(Plugin plugin, BiConsumer<String, String> onVote) {

        Plugin votifier = Bukkit.getPluginManager().getPlugin("Votifier");

        if (votifier == null || !votifier.isEnabled()) {
            return false;
        }

        try {
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(EVENT_CLASS, true,
                    votifier.getClass().getClassLoader());
            Method getVote = eventClass.getMethod("getVote");

            Bukkit.getPluginManager().registerEvent(eventClass, new VotifierBridge(), EventPriority.NORMAL,
                    (listener, event) -> {
                        if (!eventClass.isInstance(event)) {
                            return;
                        }
                        try {
                            Object vote = getVote.invoke(event);
                            String username = (String) vote.getClass().getMethod("getUsername").invoke(vote);
                            String service = (String) vote.getClass().getMethod("getServiceName").invoke(vote);

                            if (username == null || username.isBlank()) {
                                return;
                            }

                            // Votifier puede avisar fuera del hilo principal.
                            if (Bukkit.isPrimaryThread()) {
                                onVote.accept(username, service);
                            } else {
                                Bukkit.getScheduler().runTask(plugin, () -> onVote.accept(username, service));
                            }
                        } catch (ReflectiveOperationException e) {
                            plugin.getLogger().log(Level.WARNING, "No se pudo leer un voto de Votifier", e);
                        }
                    }, plugin);

            return true;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "Votifier está instalado pero su evento no es el esperado", e);
            return false;
        }
    }

}
