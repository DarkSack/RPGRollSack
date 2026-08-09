package com.sack.rpgroll.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Helper de tab-completion reusado por todos los comandos de RPGRoll y sus addons. */
public final class TabCompleteUtil {

    private TabCompleteUtil() {
    }

    /** Filtra {@code options} por lo que el jugador ya escribió de ese argumento (prefijo, sin importar mayúsculas). */
    public static List<String> filter(String token, Collection<String> options) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, result);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public static List<String> onlinePlayerNames(String token) {
        return filter(token, allOnlinePlayerNames());
    }

    /** Lista sin filtrar — para el contrato de {@code RPGCommand#getTabCompletions} (core), que filtra por su cuenta. */
    public static List<String> allOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).toList();
    }

    public static List<String> worldNames(String token) {
        return filter(token, Bukkit.getWorlds().stream().map(org.bukkit.World::getName).toList());
    }

    /** Nombres de {@link EntityType} que representan mobs invocables (excluye PLAYER/UNKNOWN/etc.). */
    public static List<String> spawnableEntityTypes(String token) {
        return filter(token, java.util.Arrays.stream(EntityType.values())
                .filter(EntityType::isSpawnable)
                .filter(EntityType::isAlive)
                .map(EntityType::name)
                .collect(Collectors.toList()));
    }

    public static List<String> materialNames(String token) {
        return filter(token, java.util.Arrays.stream(org.bukkit.Material.values())
                .map(Enum::name)
                .toList());
    }

    public static List<String> soundNames(String token) {
        return filter(token, java.util.Arrays.stream(org.bukkit.Sound.values())
                .map(Enum::name)
                .toList());
    }

}
