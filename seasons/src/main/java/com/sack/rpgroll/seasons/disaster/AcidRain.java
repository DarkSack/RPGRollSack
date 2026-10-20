package com.sack.rpgroll.seasons.disaster;

import com.sack.rpgroll.seasons.core.DisasterPhenomenon;
import com.sack.rpgroll.seasons.disaster.Looks.ParticleLook;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Algo cae del cielo sobre quien está a la intemperie y le hace daño o le pone efectos; un casco
 * puede protegerle a costa de desgastarse. Por defecto es lluvia ácida verde en los biomas donde
 * llueve; cambiando partículas, temperaturas y efectos sirve para ventisca, tormenta de arena o
 * lluvia de ceniza.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class AcidRain extends Phenomenon {

    AcidRain(DisasterEngine engine, World world, DisasterPhenomenon params, double intensity, int durationTicks) {
        super(engine, world, params, intensity, durationTicks, 5);
    }

    @Override
    boolean defaultStorm() {
        return true;
    }

    @Override
    void tick() {

        int interval = (int) Math.max(period, number("interval", 20));
        boolean hit = age % interval < period;
        List<ParticleLook> drops = particles("particles", List.of("FALLING_DUST*30 LIME_CONCRETE_POWDER"));
        double height = number("particle-height", 7);
        double spread = number("particle-spread", 7);
        boolean onlyOpenSky = flag("only-open-sky", true);

        for (Player player : engine.targets(world)) {

            Location at = player.getLocation();
            if (!fallsAt(at)) {
                continue;
            }

            for (ParticleLook drop : drops) {
                drop.spawn(world, at.clone().add(0, height, 0), 30, spread, 1, spread, 0);
            }

            if (hit && (!onlyOpenSky || DisasterEngine.underOpenSky(at))) {
                burn(player);
            }
        }

        if (hit && flag("hit-mobs", false)) {
            burnMobs(onlyOpenSky);
        }
    }

    /** Los mobs a la intemperie cerca de los jugadores (solo si {@code hit-mobs: true}). */
    private void burnMobs(boolean onlyOpenSky) {
        double reach = number("mob-radius", 24);
        for (Player player : engine.targets(world)) {
            for (Entity entity : player.getNearbyEntities(reach, reach, reach)) {
                if (entity instanceof LivingEntity living && !(entity instanceof Player) && hits(entity)
                        && fallsAt(entity.getLocation())
                        && (!onlyOpenSky || DisasterEngine.underOpenSky(entity.getLocation()))) {
                    strike(living, strength("damage", 1.5));
                }
            }
        }
    }

    private void burn(Player player) {

        ItemStack helmet = player.getInventory().getHelmet();

        if (flag("helmet-protects", true) && helmet != null && !helmet.isEmpty()
                && helmet.getType().getMaxDurability() > 0) {
            int wear = (int) Math.round(strength("armor-wear", 3));
            if (wear > 0) {
                player.damageItemStack(EquipmentSlot.HEAD, wear);
            }
            sound("helmet-sound", "BLOCK_FIRE_EXTINGUISH 0.4 1.8").play(player);
            return;
        }

        strike(player, strength("damage", 1.5));
        for (ParticleLook look : particles("hit-particles", List.of("DUST*8 #7CFC00 1.2"))) {
            look.spawn(world, player.getLocation().add(0, 1.8, 0), 8, 0.3, 0.2, 0.3, 0);
        }
        sound("hit-sound", "BLOCK_FIRE_EXTINGUISH 0.6 1.6").play(player);
    }

    /**
     * Si aquí cae según la temperatura del bioma ({@code min-temperature} a {@code max-temperature}).
     * Por defecto donde llueve: ni desiertos ni sabanas (más de 1.0) ni donde nieva (menos de 0.15).
     */
    @SuppressWarnings("deprecation")
    private boolean fallsAt(Location location) {
        double temperature = location.getBlock().getTemperature();
        return temperature >= number("min-temperature", 0.15) && temperature <= number("max-temperature", 1.0);
    }

}
