package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.api.BuiltStructure;
import com.sack.rpgroll.structures.api.StructureModule;
import com.sack.rpgroll.structures.catalog.Problem;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.common.structure.BlockSource;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@code type: mob_farm}: un spawner vanilla que se para al llegar a {@code max-entities} mobs a 16 bloques. Con
 * {@code mob: mythic:<Nombre>} el spawner queda parado y el módulo genera el mob de MythicMobs si hay un jugador cerca;
 * esos mobs se cuentan por una etiqueta PDC propia ({@link #MYTHIC_KEY}), no por la API de Mythic.
 *
 * <p>{@code mob: spawner:<id>} (mobs de otros plugins vía Things) no se admite: la Lib no tiene ninguna fuente de
 * tipo {@code spawner}.
 */
public final class MobFarmModule implements StructureModule {

    public static final String TYPE = "mob_farm";
    public static final NamespacedKey MYTHIC_KEY = new NamespacedKey("rpgrollstructures", "mob_farm_mythic");
    static final int RADIUS = 16;
    static final int DEFAULT_MAX = 12;
    private static final String MYTHIC = "mythic:";

    private final Function<String, World> worlds;
    private final BooleanSupplier mythicActive;
    private final Supplier<MythicSpawner> mythic;

    /** @param mythic adaptador actual de Mythic; no se llama si MythicMobs no está activo */
    public MobFarmModule(Function<String, World> worlds, BooleanSupplier mythicActive,
                         Supplier<MythicSpawner> mythic) {
        this.worlds = worlds;
        this.mythicActive = mythicActive;
        this.mythic = mythic;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public List<Problem> validate(ConfigurationSection c) {
        List<Problem> out = new ArrayList<>();
        if (position(c) == null) {
            out.add(new Problem("", "spawner", "debe ser [x, y, z] con enteros de 0 en adelante"));
        }
        String mob = c.getString("mob", "");
        if (isMythic(mob)) {
            if (mob.substring(MYTHIC.length()).isBlank()) {
                out.add(new Problem("", "mob", "mythic: necesita un nombre"));
            } else if (!mythicActive.getAsBoolean()) {
                out.add(new Problem("", "mob",
                        "MythicMobs no está activo (no instalado o apagado en integrations.yml)"));
            }
        } else if (mob.contains(":")) {
            out.add(new Problem("", "mob", "solo EntityType vanilla o mythic:<Nombre>; la Lib no tiene fuente spawner"));
        } else if (vanilla(mob) == null) {
            out.add(new Problem("", "mob", "EntityType que no se puede generar: " + mob));
        }
        if (c.contains("max-entities") && c.getInt("max-entities", 0) < 1) {
            out.add(new Problem("", "max-entities", "debe ser 1 o más"));
        }
        return out;
    }

    /** Con el esquemático: el spawner está dentro del tamaño sin girar y ahí hay un bloque SPAWNER. */
    @Override
    public List<Problem> validate(ConfigurationSection c, BlockSource blocks) {
        List<Problem> out = validate(c);
        List<Integer> p = position(c);
        if (p == null || blocks == null) {
            return out;
        }
        int x = p.get(0), y = p.get(1), z = p.get(2);
        String pos = "[" + x + ", " + y + ", " + z + "]";
        if (x >= blocks.width() || y >= blocks.height() || z >= blocks.length()) {
            out.add(new Problem("", "spawner", pos + " queda fuera del esquemático (" + blocks.width() + "×"
                    + blocks.height() + "×" + blocks.length() + ")"));
            return out;
        }
        String found = blocks.blocks().stream().filter(sb -> sb.x() == x && sb.y() == y && sb.z() == z).findFirst()
                .map(sb -> sb.data().getMaterial().name()).orElse("aire");
        if (!Material.SPAWNER.name().equals(found)) {
            out.add(new Problem("", "spawner", "en " + pos + " hay " + found + " y debe haber un SPAWNER"));
        }
        return out;
    }

    /** Integrations puede romperse tras cargar: un mob mythic sin MythicMobs activo no se construye. */
    @Override
    public Optional<String> precheck(UUID owner, String world, Box box, ConfigurationSection c) {
        return isMythic(c.getString("mob", "")) && !mythicActive.getAsBoolean()
                ? Optional.of(ModuleService.DENIED) : Optional.empty();
    }

    @Override
    public void onBuilt(BuiltStructure b) {
        CreatureSpawner s = spawner(b).state();
        String mob = b.config().getString("mob", "");
        if (isMythic(mob)) {
            s.setRequiredPlayerRange(0); // lo genera el módulo; el spawner vanilla queda parado
        } else {
            s.setSpawnedType(vanilla(mob));
        }
        s.update();
    }

    @Override
    public int tickEvery() {
        return 100;
    }

    @Override
    public void onTick(BuiltStructure b) {
        Spot spot = spawner(b);
        CreatureSpawner s = spot.state();
        Location at = spot.at();
        World w = at.getWorld();
        String mob = b.config().getString("mob", "");
        int max = b.config().getInt("max-entities", DEFAULT_MAX);
        if (isMythic(mob)) {
            String name = mob.substring(MYTHIC.length());
            if (!mythicActive.getAsBoolean() || w.getNearbyPlayers(at, RADIUS).isEmpty() || count(w, at,
                    e -> name.equals(e.getPersistentDataContainer().get(MYTHIC_KEY, PersistentDataType.STRING)))
                    >= max) {
                return;
            }
            Entity e = mythic.get().spawn(name, at.clone().add(0.5, 1, 0.5));
            if (e != null) {
                e.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, name);
            }
            return;
        }
        EntityType type = vanilla(mob);
        boolean full = count(w, at, e -> e.getType() == type) >= max;
        int range = s.getRequiredPlayerRange();
        if (full && range != 0) {
            s.setRequiredPlayerRange(0);
            s.update();
        } else if (!full && range == 0) {
            s.setRequiredPlayerRange(16);
            s.update();
        }
    }

    @Override
    public void onRemoved(BuiltStructure b) {
    }

    private static int count(World w, Location at, Predicate<Entity> match) {
        return (int) w.getNearbyEntities(at, RADIUS, RADIUS, RADIUS).stream().filter(match).count();
    }

    /** Mismo giro que BlockSources.rotate: Rotation.apply con el tamaño sin girar, desde la esquina mínima. */
    private record Spot(CreatureSpawner state, Location at) {
    }

    private Spot spawner(BuiltStructure b) {
        BuildRecord r = b.record();
        List<Integer> p = position(b.config());
        Box box = r.box();
        int bw = box.maxX() - box.minX() + 1, bl = box.maxZ() - box.minZ() + 1;
        // La caja guardada ya está girada (ConstructionEngine.box): se des-gira el tamaño.
        Rotation rot = r.rotation();
        int[] xz = rot.apply(p.get(0), p.get(2), rot.rotatedWidth(bw, bl), rot.rotatedLength(bw, bl));
        World world = worlds.apply(r.world());
        if (world == null) {
            throw new IllegalStateException("mundo no cargado: " + r.world());
        }
        int x = r.x() + xz[0], y = r.y() + p.get(1), z = r.z() + xz[1];
        BlockState st = world.getBlockAt(x, y, z).getState();
        if (!(st instanceof CreatureSpawner cs)) {
            throw new IllegalStateException("no hay un spawner en " + x + "," + y + "," + z + " (revisa module.spawner)");
        }
        return new Spot(cs, new Location(world, x, y, z));
    }

    private static boolean isMythic(String mob) {
        return mob.regionMatches(true, 0, MYTHIC, 0, MYTHIC.length());
    }

    private static EntityType vanilla(String name) {
        try {
            EntityType t = EntityType.valueOf(name.trim().toUpperCase(Locale.ROOT));
            return t.isSpawnable() && t.isAlive() ? t : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** [x, y, z] con enteros >= 0, o null. */
    private static List<Integer> position(ConfigurationSection c) {
        List<?> raw = c.getList("spawner");
        if (raw == null || raw.size() != 3) {
            return null;
        }
        List<Integer> out = new ArrayList<>();
        for (Object o : raw) {
            if (!(o instanceof Integer i) || i < 0) {
                return null;
            }
            out.add(i);
        }
        return out;
    }
}
