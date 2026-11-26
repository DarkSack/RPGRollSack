package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.api.BuiltStructure;
import com.sack.rpgroll.structures.catalog.Problem;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import org.bukkit.Location;
import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.SourceBlock;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

class MobFarmModuleTest {

    final World world = mock(World.class);
    final Block block = mock(Block.class);
    final CreatureSpawner spawner = mock(CreatureSpawner.class);
    final List<Entity> nearby = new ArrayList<>();
    final List<Player> players = new ArrayList<>();
    final List<String> spawned = new ArrayList<>();
    boolean mythicActive = true;
    int range = 16;

    /** MythicSpawner falso: apunta el nombre y devuelve una entidad nueva. */
    final MythicSpawner fakeMythic = (name, at) -> {
        spawned.add(name);
        Entity e = entity(EntityType.ZOMBIE, name);
        nearby.add(e);
        return e;
    };

    final MobFarmModule module = new MobFarmModule(n -> world, () -> mythicActive, () -> fakeMythic);

    MobFarmModuleTest() {
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.getState()).thenReturn(spawner);
        when(world.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble()))
                .thenAnswer(i -> new ArrayList<>(nearby));
        when(world.getNearbyPlayers(any(Location.class), anyDouble())).thenAnswer(i -> new ArrayList<>(players));
        when(spawner.getRequiredPlayerRange()).thenAnswer(i -> range);
        doAnswer(i -> {
            range = i.getArgument(0);
            return null;
        }).when(spawner).setRequiredPlayerRange(anyInt());
    }

    static Entity entity(EntityType type, String mythicTag) {
        Entity e = mock(Entity.class);
        when(e.getType()).thenReturn(type);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.get(MobFarmModule.MYTHIC_KEY, PersistentDataType.STRING)).thenReturn(mythicTag);
        when(e.getPersistentDataContainer()).thenReturn(pdc);
        return e;
    }

    static ConfigurationSection cfg(String mob, int max, int... pos) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("type", "mob_farm");
        y.set("spawner", List.of(pos[0], pos[1], pos[2]));
        y.set("mob", mob);
        y.set("max-entities", max);
        return y;
    }

    BuiltStructure built(ConfigurationSection c, Rotation r) {
        // Esquemático de 5x3x3 en (10,64,20); girado 90 la caja pasa a 3x5.
        Box box = r == Rotation.CW_90 || r == Rotation.CCW_90
                ? new Box(10, 64, 20, 12, 66, 24) : new Box(10, 64, 20, 14, 66, 22);
        BuildRecord rec = new BuildRecord(UUID.randomUUID(), UUID.randomUUID(), "granja", UUID.randomUUID(), "w", 10,
                64, 20, r, box, 0);
        return new BuiltStructure(rec, null, c);
    }

    @Test
    void validatesSpawnerShape() {
        assertTrue(module.validate(cfg("zombie", 12, 3, 1, 1)).isEmpty());
        assertTrue(problem(cfg("zombie", 12, -1, 1, 1), "spawner"));
        assertTrue(problem(cfg("nope", 12, 3, 1, 1), "mob"));
        assertTrue(problem(cfg("player", 12, 3, 1, 1), "mob"));
        assertTrue(problem(cfg("spawner:x", 12, 3, 1, 1), "mob")); // Things no tiene fuente spawner
        assertTrue(problem(cfg("zombie", 0, 3, 1, 1), "max-entities"));
    }

    /** Esquemático falso de 5x3x3 con un spawner en (3,1,1) y piedra en (0,0,0). */
    static BlockSource schematic() {
        return new BlockSource() {
            @Override public int width() { return 5; }
            @Override public int height() { return 3; }
            @Override public int length() { return 3; }
            @Override public List<SourceBlock> blocks() {
                return List.of(new SourceBlock(3, 1, 1, data(Material.SPAWNER), null),
                        new SourceBlock(0, 0, 0, data(Material.STONE), null));
            }
        };
    }

    static BlockData data(Material m) {
        BlockData d = mock(BlockData.class);
        when(d.getMaterial()).thenReturn(m);
        return d;
    }

    @Test
    void validatesSpawnerAgainstSchematic() {
        assertTrue(module.validate(cfg("zombie", 12, 3, 1, 1), schematic()).isEmpty());
        assertTrue(module.validate(cfg("zombie", 12, 9, 1, 1), schematic()).stream()
                .anyMatch(p -> p.key().equals("spawner") && p.reason().contains("fuera")));
        assertTrue(module.validate(cfg("zombie", 12, 5, 1, 1), schematic()).stream()
                .anyMatch(p -> p.key().equals("spawner") && p.reason().contains("fuera")));
        assertTrue(module.validate(cfg("zombie", 12, 0, 0, 0), schematic()).stream()
                .anyMatch(p -> p.reason().contains("STONE") && p.reason().contains("[0, 0, 0]")));
        assertTrue(module.validate(cfg("zombie", 12, 1, 1, 1), schematic()).stream()
                .anyMatch(p -> p.reason().contains("aire")));
        assertTrue(module.validate(cfg("zombie", 12, 3, 1, 1), null).isEmpty()); // sin esquemático legible
    }

    @Test
    void precheckDeniesMythicWhenMythicMobsIsNotActive() {
        Box box = new Box(0, 0, 0, 4, 2, 2);
        assertTrue(module.precheck(UUID.randomUUID(), "w", box, cfg("mythic:Boss", 12, 3, 1, 1)).isEmpty());
        mythicActive = false;
        assertTrue(module.precheck(UUID.randomUUID(), "w", box, cfg("mythic:Boss", 12, 3, 1, 1)).isPresent());
        assertTrue(module.precheck(UUID.randomUUID(), "w", box, cfg("zombie", 12, 3, 1, 1)).isEmpty());
    }

    boolean problem(ConfigurationSection c, String key) {
        return module.validate(c).stream().map(Problem::key).anyMatch(key::equals);
    }

    @Test
    void rejectsMythicWithoutMythicMobs() {
        mythicActive = false;
        assertTrue(problem(cfg("mythic:Boss", 12, 3, 1, 1), "mob"));
        mythicActive = true;
        assertTrue(module.validate(cfg("mythic:Boss", 12, 3, 1, 1)).isEmpty());
        assertTrue(problem(cfg("mythic:", 12, 3, 1, 1), "mob"));
    }

    @Test
    void onBuiltSetsSpawnerTypeAtRotatedPosition() {
        module.onBuilt(built(cfg("zombie", 12, 3, 1, 1), Rotation.CW_90));
        // apply(3,1, 5,3) con CW_90 = {l-1-z, x} = {1,3} -> (11, 65, 23)
        verify(world).getBlockAt(11, 65, 23);
        verify(spawner).setSpawnedType(EntityType.ZOMBIE);
        verify(spawner).update();
    }

    @Test
    void stopsSpawnerAtMaxEntities() {
        BuiltStructure b = built(cfg("zombie", 2, 3, 1, 1), Rotation.NONE);
        nearby.add(entity(EntityType.ZOMBIE, null));
        nearby.add(entity(EntityType.ZOMBIE, null));
        nearby.add(entity(EntityType.COW, null));
        module.onTick(b);
        assertEquals(0, range);
        assertEquals(100, module.tickEvery());
    }

    @Test
    void restartsSpawnerBelowMax() {
        BuiltStructure b = built(cfg("zombie", 2, 3, 1, 1), Rotation.NONE);
        range = 0;
        nearby.add(entity(EntityType.ZOMBIE, null));
        module.onTick(b);
        assertEquals(16, range);
    }

    @Test
    void mythicSpawnsOnlyWithPlayerNearAndBelowMax() {
        BuiltStructure b = built(cfg("mythic:Boss", 2, 3, 1, 1), Rotation.NONE);
        module.onTick(b);
        assertTrue(spawned.isEmpty()); // sin jugador
        players.add(mock(Player.class));
        module.onTick(b);
        assertEquals(List.of("Boss"), spawned);
        nearby.add(entity(EntityType.ZOMBIE, "Boss")); // ya hay 2 con la etiqueta
        module.onTick(b);
        assertEquals(1, spawned.size() , "tope alcanzado");
        // los que no llevan nuestra etiqueta no cuentan
        nearby.clear();
        nearby.add(entity(EntityType.ZOMBIE, null));
        module.onTick(b);
        assertEquals(2, spawned.size());
    }
}
