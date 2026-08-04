package com.sack.rpgroll.seasons.event;

import com.sack.rpgroll.effects.api.EffectsAPI;
import com.sack.rpgroll.sackeffects.api.SackEffectsAPI;
import com.sack.rpgroll.seasons.core.WorldEvent;
import com.sack.rpgroll.seasons.core.WorldEventComponent;
import com.sack.rpgroll.seasons.integration.MobsIntegration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Dispara un {@link WorldEvent} sobre un mundo entero — a diferencia de
 * RPGRoll-Magic/Effects, no hay noción de "target" individual: cada
 * componente corre sobre TODOS los jugadores online de ese mundo (salvo
 * COMMAND/SET_WEATHER, que son una sola vez por mundo).
 */
public class WorldEventEngine {

    private final Plugin plugin;

    public WorldEventEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void trigger(WorldEvent event, World world) {

        Component announce = Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text(event.displayName(), NamedTextColor.YELLOW))
                .append(Component.text(" ha comenzado.", NamedTextColor.GOLD));

        world.getPlayers().forEach(player -> player.sendMessage(announce));

        for (WorldEventComponent component : event.components()) {
            executeComponent(component, world);
        }

        if (event.durationTicks() > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Component end = Component.text("✦ ", NamedTextColor.GOLD)
                        .append(Component.text(event.displayName(), NamedTextColor.YELLOW))
                        .append(Component.text(" ha terminado.", NamedTextColor.GRAY));
                world.getPlayers().forEach(player -> player.sendMessage(end));
            }, event.durationTicks());
        }
    }

    private void executeComponent(WorldEventComponent component, World world) {

        try {
            switch (component.type()) {
                case PARTICLE -> forEachPlayer(world, player -> executeParticle(component, player));
                case SOUND -> forEachPlayer(world, player -> executeSound(component, player));
                case VISUAL -> forEachPlayer(world, player -> executeVisual(component, player));
                case APPLY_EFFECT -> forEachPlayer(world, player -> executeApplyEffect(component, player));
                case SPAWN_MOB -> forEachPlayer(world, player -> executeSpawnMob(component, player));
                case MESSAGE -> executeMessage(component, world);
                case COMMAND -> executeCommand(component, world);
                case SET_WEATHER -> executeSetWeather(component, world);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("✘ Error ejecutando componente " + component.type() + " de un evento mundial: "
                    + e.getMessage());
        }
    }

    private void forEachPlayer(World world, java.util.function.Consumer<Player> action) {
        for (Player player : world.getPlayers()) {
            action.accept(player);
        }
    }

    private void executeParticle(WorldEventComponent component, Player player) {

        Particle particle;

        try {
            particle = Particle.valueOf(component.param("particle", "FLAME").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            particle = Particle.FLAME;
        }

        int count = component.paramInt("count", 20);
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 1, 1, 1, 0.02);
    }

    private void executeSound(WorldEventComponent component, Player player) {

        Sound sound;

        try {
            sound = Sound.valueOf(component.param("sound", "AMBIENT_CAVE").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sound = Sound.AMBIENT_CAVE;
        }

        float volume = (float) component.paramDouble("volume", 1.0);
        float pitch = (float) component.paramDouble("pitch", 1.0);

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void executeVisual(WorldEventComponent component, Player player) {

        String effectId = component.param("effect-id", null);

        if (effectId != null && SackEffectsAPI.isReady()) {
            SackEffectsAPI.get().play(effectId, player);
        }
    }

    private void executeApplyEffect(WorldEventComponent component, Player player) {

        String effectId = component.param("effect-id", null);

        if (effectId != null && EffectsAPI.isReady()) {
            EffectsAPI.get().apply(effectId, player);
        }
    }

    private void executeSpawnMob(WorldEventComponent component, Player player) {

        String mobId = component.param("mob-id", null);
        double chance = component.paramDouble("chance", 1.0);

        if (mobId == null || Math.random() >= chance) {
            return;
        }

        MobsIntegration.spawnMob(mobId, player.getLocation());
    }

    private void executeMessage(WorldEventComponent component, World world) {

        String text = component.param("text", "");

        if (text.isBlank()) {
            return;
        }

        Component message = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(text).colorIfAbsent(NamedTextColor.WHITE);

        world.getPlayers().forEach(player -> player.sendMessage(message));
    }

    private void executeCommand(WorldEventComponent component, World world) {

        String raw = component.param("command", "");

        if (raw.isBlank()) {
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), raw.replace("%world%", world.getName()));
    }

    private void executeSetWeather(WorldEventComponent component, World world) {

        boolean storm = component.param("storm", "true").equalsIgnoreCase("true");
        boolean thunder = component.param("thunder", "false").equalsIgnoreCase("true");

        world.setStorm(storm);
        world.setThundering(thunder);
    }

}
