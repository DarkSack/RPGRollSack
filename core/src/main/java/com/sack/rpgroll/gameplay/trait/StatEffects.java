package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Enseña S.P.E.C.I.A.L. en la lista de efectos del inventario: "Fuerza II",
 * "Suerte III"… El nivel es el modificador del atributo, que es de donde
 * salen sus bonos (ver {@link PassiveEffects}); un atributo sin bono no sale.
 * <p>
 * Minecraft no deja añadir efectos a esa lista, así que cada atributo toma
 * prestado el hueco de un efecto que un jugador no recibe casi nunca, y el
 * resource pack le cambia el nombre y el icono ({@code resourcepack/} de este
 * jar, generado por sackito-network/infra/texturas/efectos_special.py).
 * <p>
 * No son efectos de verdad: solo se le manda el paquete al cliente y el
 * servidor no sabe nada de ellos. Por eso no dan nada, no caducan y ni la
 * leche, ni morir, ni /effect clear los quitan.
 */
public class StatEffects implements Runnable {

    public static final long PERIOD_TICKS = 40;

    private final PlayerManager playerManager;
    // En el constructor y no en un static: PotionEffectType necesita el servidor arrancado.
    private final Map<StatType, PotionEffectType> slots = new EnumMap<>(StatType.class);

    public StatEffects(PlayerManager playerManager) {
        this.playerManager = playerManager;
        slots.put(StatType.STRENGTH, PotionEffectType.HEALTH_BOOST);
        slots.put(StatType.WISDOM, PotionEffectType.GLOWING);
        slots.put(StatType.CONSTITUTION, PotionEffectType.OOZING);
        slots.put(StatType.CHARISMA, PotionEffectType.INFESTED);
        slots.put(StatType.INTELLIGENCE, PotionEffectType.WIND_CHARGED);
        slots.put(StatType.DEXTERITY, PotionEffectType.UNLUCK);
        slots.put(StatType.LUCK, PotionEffectType.LUCK);
    }

    // shortcut: se reenvía a todos cada 2 s en vez de escuchar cada cosa que vacía la lista del cliente (morir,
    // cambiar de mundo, que caduque el efecto real que presta el hueco), escuchar eventos si el tráfico pesa.
    // Mientras un jugador tenga de verdad uno de esos siete efectos, su hueco enseña el atributo.
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerManager.getPlayer(player.getUniqueId()).ifPresent(rpgPlayer -> show(player, rpgPlayer));
        }
    }

    private void show(Player player, RPGPlayer rpgPlayer) {

        slots.forEach((stat, type) -> {

            int level = level(player, rpgPlayer, stat);

            if (level > 0) {
                // Sin partículas y sin icono en pantalla: solo la línea de la lista del inventario.
                player.sendPotionEffectChange(player,
                        new PotionEffect(type, PotionEffect.INFINITE_DURATION, level - 1, false, false, false));
            } else {
                player.sendPotionEffectChangeRemove(player, type);
            }
        });
    }

    /** La suerte es la que de verdad tiene encima: la del atributo más rasgos, amuletos e ítems. */
    private static int level(Player player, RPGPlayer rpgPlayer, StatType stat) {

        if (stat == StatType.LUCK) {
            AttributeInstance luck = player.getAttribute(Attribute.LUCK);
            return luck == null ? 0 : (int) Math.round(luck.getValue());
        }

        return (rpgPlayer.getStats().get(stat) - 10) / 2;
    }

}
