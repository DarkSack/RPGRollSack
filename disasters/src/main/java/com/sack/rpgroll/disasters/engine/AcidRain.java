package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.engine.Looks.ParticleLook;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Algo cae del cielo sobre quien está a la intemperie y le hace daño o le pone efectos; un casco
 * puede protegerle a costa de desgastarse. Además puede cambiar el suelo alrededor de los jugadores
 * ({@code corrode}: la hierba se seca, las hojas se deshacen), cubrirlo ({@code cover}: nieve que se
 * acumula por capas), congelar ({@code freeze-ticks}; la ropa de cuero lo evita) y disolver los
 * objetos tirados ({@code dissolve-items}). Lo cambiado se repone con el tiempo. Por defecto es
 * lluvia ácida verde; con otros valores, ventisca, tormenta de arena o lluvia de ceniza.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class AcidRain extends Phenomenon {

    /** De qué bloque a cuál, de {@code corrode: ["GRASS_BLOCK>DIRT", "*_LEAVES>AIR"]}. */
    private Map<Material, BlockData> corrosion;

    AcidRain(DisasterEngine engine, World world, DisasterPhenomenon params, Power power, int durationTicks) {
        super(engine, world, params, power, durationTicks, 5);
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
                freeze(player);
            }
            changeGround(at);
        }

        if (hit && flag("hit-mobs", false)) {
            burnMobs(onlyOpenSky);
        }
        if (hit) {
            dissolveItems(onlyOpenSky);
        }
    }

    // ----- El suelo -----

    /**
     * Cada paso, {@code ground-per-tick} columnas al azar a menos de {@code ground-radius} del jugador:
     * si el bloque de arriba está en {@code corrode}, cambia (con {@code corrode-chance}); si no, se
     * cubre con {@code cover} (con {@code cover-chance}), apilando capas hasta {@code cover-max-layers}.
     */
    private void changeGround(Location near) {

        double corrodeChance = strength("corrode-chance", 0);
        double coverChance = strength("cover-chance", 0);
        WorldDamage damage = engine.damage();
        if ((corrodeChance <= 0 && coverChance <= 0) || !damage.enabled()) {
            return;
        }

        double radius = scaled("ground-radius", 12);
        for (int i = 0; i < (int) number("ground-per-tick", 4); i++) {
            double angle = random() * Math.PI * 2;
            double distance = Math.sqrt(random()) * radius;
            Block top = world.getHighestBlockAt(near.getBlockX() + (int) Math.round(Math.cos(angle) * distance),
                    near.getBlockZ() + (int) Math.round(Math.sin(angle) * distance), HeightMap.WORLD_SURFACE);
            if (!fallsAt(top.getLocation())) {
                continue;
            }

            BlockData corroded = corrosion().get(top.getType());
            if (corroded != null) {
                if (random() < corrodeChance && !damage.filter(top, List.of(top)).isEmpty()) {
                    damage.change(top, corroded, regenerate());
                    world.spawnParticle(org.bukkit.Particle.CLOUD, top.getLocation().add(0.5, 1, 0.5), 3, 0.3, 0.1, 0.3,
                            0.01, null, true);
                }
                continue;
            }
            if (random() < coverChance) {
                cover(top, damage);
            }
        }
    }

    private void cover(Block top, WorldDamage damage) {

        BlockData cover = block("cover", "SNOW");
        int maxLayers = (int) number("cover-max-layers", 3);
        if (top.getType() == cover.getMaterial() && top.getBlockData() instanceof Snow snow) {
            if (snow.getLayers() < Math.min(maxLayers, snow.getMaximumLayers())
                    && !damage.filter(top, List.of(top)).isEmpty()) {
                Snow thicker = (Snow) snow.clone();
                thicker.setLayers(snow.getLayers() + 1);
                damage.change(top, thicker, regenerate());
            }
            return;
        }
        Block above = top.getRelative(BlockFace.UP);
        if (top.getType().isOccluding() && above.getType().isAir()
                && cover.isSupported(above) && damage.canPlace(above)) {
            damage.change(above, cover, regenerate());
        }
    }

    private Map<Material, BlockData> corrosion() {

        if (corrosion == null) {
            corrosion = new EnumMap<>(Material.class);
            for (String rule : params.list("corrode", List.of())) {
                String[] parts = rule.split(">", 2);
                try {
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("falta '>' (DE>A)");
                    }
                    BlockData to = Looks.material(parts[1]).createBlockData();
                    for (Material from : Looks.materials(parts[0])) {
                        corrosion.put(from, to);
                    }
                } catch (IllegalArgumentException e) {
                    engine.warn(params.type() + " 'corrode': " + rule + " no vale (" + e.getMessage() + "); se omite");
                }
            }
        }
        return corrosion;
    }

    // ----- Seres vivos y objetos -----

    /** Con {@code freeze-ticks} > 0 se congela poco a poco, como en la nieve polvo; el cuero lo evita. */
    private void freeze(Player player) {

        int ticks = (int) Math.round(strength("freeze-ticks", 0));
        if (ticks <= 0) {
            return;
        }
        if (flag("leather-protects", true)) {
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                if (piece != null && piece.getType().name().startsWith("LEATHER_")) {
                    return;
                }
            }
        }
        player.setFreezeTicks(Math.min(player.getMaxFreezeTicks() + 40, player.getFreezeTicks() + ticks));
    }

    /** Objetos tirados a la intemperie cerca de los jugadores: cada uno se disuelve con {@code dissolve-items}. */
    private void dissolveItems(boolean onlyOpenSky) {

        double chance = number("dissolve-items", 0);
        if (chance <= 0) {
            return;
        }
        for (Player player : engine.targets(world)) {
            for (Item item : player.getLocation().getNearbyEntitiesByType(Item.class, 16)) {
                if (random() < chance && engine.affects(item) && fallsAt(item.getLocation())
                        && (!onlyOpenSky || DisasterEngine.underOpenSky(item.getLocation()))) {
                    world.spawnParticle(org.bukkit.Particle.CLOUD, item.getLocation(), 4, 0.1, 0.1, 0.1, 0.02, null,
                            true);
                    item.remove();
                }
            }
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

        int wear = (int) Math.round(strength("armor-wear", 3));
        if (wear > 0) {
            for (String slotName : params.list("armor-slots", List.of("HEAD"))) {
                try {
                    EquipmentSlot slot = EquipmentSlot.valueOf(slotName.toUpperCase(java.util.Locale.ROOT));
                    ItemStack piece = player.getInventory().getItem(slot);
                    if (slot != EquipmentSlot.HEAD && piece != null && !piece.isEmpty()
                            && piece.getType().getMaxDurability() > 0) {
                        player.damageItemStack(slot, wear);
                    }
                } catch (IllegalArgumentException e) {
                    engine.warn(params.type() + " 'armor-slots': " + slotName + " no es una ranura; se omite");
                }
            }
        }

        if (flag("helmet-protects", true) && helmet != null && !helmet.isEmpty()
                && helmet.getType().getMaxDurability() > 0) {
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
