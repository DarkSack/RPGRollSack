package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShootActionTest {

    private Player player;
    private World world;
    private MockedStatic<ShotMuzzle> muzzleScale;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getEyeLocation()).thenReturn(new Location(world, 0, 65, 0));
        muzzleScale = org.mockito.Mockito.mockStatic(ShotMuzzle.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        muzzleScale.when(() -> ShotMuzzle.playerScale(player)).thenReturn(1.0);
    }

    @AfterEach void closeMuzzleScale() { muzzleScale.close(); }

    @Test
    void damagesTheFirstCreatureOnTheRayAsThePlayer() {
        LivingEntity target = mock(LivingEntity.class);
        when(world.rayTrace(any(Location.class), any(Vector.class), eq(20.0), eq(FluidCollisionMode.NEVER),
                eq(true), anyDouble(), any())).thenReturn(new RayTraceResult(new Vector(0, 65, 5), target));

        BuiltinItemActions.shoot(new ItemAction("SHOOT", Map.of("range", "20", "amount", "9")),
                new ItemActionContext(player, null, null, null), mock(Plugin.class));

        verify(target).damage(9.0, player);
    }

    @Test
    void missingDoesNotFail() {
        BuiltinItemActions.shoot(new ItemAction("SHOOT", Map.of("particle", "DUST", "color", "#33FF66")),
                new ItemActionContext(player, null, null, null), mock(Plugin.class));
    }

    @Test
    void explosiveShotBreaksBlocksOnlyWhenAsked() {
        when(world.rayTrace(any(Location.class), any(Vector.class), eq(40.0), eq(FluidCollisionMode.NEVER),
                eq(true), anyDouble(), any())).thenReturn(new RayTraceResult(new Vector(0, 65, 8)));

        BuiltinItemActions.shoot(new ItemAction("SHOOT", Map.of("range", "40", "explosion", "2.5",
                "break-blocks", "true")), new ItemActionContext(player, null, null, null), mock(Plugin.class));

        verify(world).createExplosion(any(Location.class), eq(2.5f), eq(false), eq(true), eq(player));
    }

    // ------------------------------------------------------------ munición

    private PlayerInventory inventory(ItemStack... contents) {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getContents()).thenReturn(contents);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        return inventory;
    }

    /** Un ItemStack de `material` con el id de RPGRoll-Items `id` (o ninguno si es null). */
    private static ItemStack stack(Material material, String id, int amount) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenReturn(amount);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(id);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(stack.hasItemMeta()).thenReturn(true);
        when(stack.getItemMeta()).thenReturn(meta);
        return stack;
    }

    private static Plugin plugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.namespace()).thenReturn("rpgroll-items"); // NamespacedKey(plugin, ...) lo lee de aquí
        return plugin;
    }

    private LivingEntity targetAhead() {
        LivingEntity target = mock(LivingEntity.class);
        when(world.rayTrace(any(Location.class), any(Vector.class), eq(32.0), eq(FluidCollisionMode.NEVER),
                eq(true), anyDouble(), any())).thenReturn(new RayTraceResult(new Vector(0, 65, 5), target));
        return target;
    }

    private void shoot(Map<String, String> params) {
        BuiltinItemActions.shoot(new ItemAction("SHOOT", params), new ItemActionContext(player, null, null, null),
                plugin());
    }

    @Test
    void withoutAmmoNothingIsFired() {
        inventory(stack(Material.PAPER, "otra_cosa", 5));
        LivingEntity target = targetAhead();

        shoot(Map.of("ammo", "laser_cell", "no-ammo-sound", ""));

        verify(target, never()).damage(anyDouble(), any(Player.class));
    }

    @Test
    void ammoIsSpentAndTheShotFires() {
        ItemStack cells = stack(Material.PAPER, "laser_cell", 3);
        PlayerInventory inventory = inventory(stack(Material.DIRT, null, 64), cells);
        LivingEntity target = targetAhead();

        shoot(Map.of("ammo", "laser_cell", "ammo-amount", "2", "amount", "7"));

        verify(cells).setAmount(1);
        verify(inventory).setItem(1, cells);
        verify(target).damage(7.0, player);
    }

    @Test
    void theLastShotEmptiesTheSlot() {
        ItemStack cells = stack(Material.PAPER, "laser_cell", 1);
        PlayerInventory inventory = inventory(cells);
        targetAhead();

        shoot(Map.of("ammo", "laser_cell"));

        verify(inventory).setItem(eq(0), isNull());
    }

    @Test
    void creativeNeedsAmmoButKeepsIt() {
        ItemStack cells = stack(Material.PAPER, "laser_cell", 4);
        inventory(cells);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        LivingEntity target = targetAhead();

        shoot(Map.of("ammo", "laser_cell"));

        verify(cells, never()).setAmount(anyInt());
        verify(target).damage(6.0, player);
    }

    @Test
    void vanillaAmmoDoesNotCountRpgrollItemsOnTheSameMaterial() {
        inventory(stack(Material.PAPER, "laser_cell", 10));
        LivingEntity target = targetAhead();

        shoot(Map.of("ammo", "minecraft:paper", "no-ammo-sound", ""));

        verify(target, never()).damage(anyDouble(), any(Player.class));
    }

    // ------------------------------------------------------------ láser sostenido

    @Test
    void sustainedBeamFollowsTheAimWhileHeldAndThenGoesOut() {
        Plugin plugin = plugin();
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(1L))).thenReturn(task);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        LivingEntity target = targetAhead();
        ItemAction beam = new ItemAction("SHOOT", Map.of("sustain", "2", "amount", "3"));
        ItemActionContext ctx = new ItemActionContext(player, null, null, null);

        BuiltinItemActions.shoot(beam, ctx, plugin);
        BuiltinItemActions.shoot(beam, ctx, plugin);    // el clic mantenido repite: renueva, no abre otro rayo

        ArgumentCaptor<Runnable> tick = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(1)).runTaskTimer(eq(plugin), tick.capture(), eq(0L), eq(1L));
        verify(target, times(2)).setNoDamageTicks(0);
        verify(target, times(2)).damage(3.0, player);

        tick.getValue().run();
        tick.getValue().run();
        verify(task, never()).cancel();
        tick.getValue().run();
        verify(task).cancel();
    }

    @Test
    void disablingThePluginStopsTheBeamBeforeItsFirstTick() {
        Plugin plugin = plugin();
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(1L))).thenReturn(task);
        BuiltinItemActions.shoot(new ItemAction("SHOOT", Map.of("sustain", "20")),
                new ItemActionContext(player, null, null, null), plugin);
        BuiltinItemActions.shutdown(plugin);
        BuiltinItemActions.shutdown(plugin);
        verify(task).cancel();
    }

}
