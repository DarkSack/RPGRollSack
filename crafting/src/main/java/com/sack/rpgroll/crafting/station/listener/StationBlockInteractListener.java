package com.sack.rpgroll.crafting.station.listener;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.crafting.api.event.StationOpenEvent;
import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.runtime.StationRuntime;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;
import com.sack.rpgroll.crafting.station.structure.StructureDetector;
import com.sack.rpgroll.crafting.station.structure.StructureRequirement;
import com.sack.rpgroll.common.block.Protection;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Click derecho sobre un bloque cuyo material coincide con
 * {@code CustomStation#triggerBlockMaterial} abre el inventario propio de
 * esa estación (una por ubicación, creada la primera vez que se interactúa
 * con ese bloque específico). Si la estación define
 * {@code structureRequirements}, primero verifica que los bloques
 * alrededor del disparador coincidan — si falta alguno, no abre y avisa.
 * <p>
 * Dos estaciones pueden compartir el mismo {@code triggerBlockMaterial}
 * (por ejemplo, una versión "básica" sin estructura y una "avanzada" que
 * exige una estructura alrededor del mismo bloque) — entre las candidatas
 * que matchean el material, se prefiere la primera cuya estructura esté
 * satisfecha (o que no exija ninguna); solo si ninguna estructurada matchea
 * se cae a una candidata sin requisitos, si existe.
 */
public class StationBlockInteractListener implements Listener {

    private final CustomStationManager stationManager;
    private final StationRuntimeRegistry runtimeRegistry;
    private final LangManager lang;
    private final boolean requireBuildPermission;
    private final StructureDetector structureDetector = new StructureDetector();

    public StationBlockInteractListener(CustomStationManager stationManager, StationRuntimeRegistry runtimeRegistry,
            LangManager lang, boolean requireBuildPermission) {
        this.stationManager = stationManager;
        this.runtimeRegistry = runtimeRegistry;
        this.lang = lang;
        this.requireBuildPermission = requireBuildPermission;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        // Solo la mano principal: el clic llega una vez por mano y abría (y avisaba) dos veces.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        Material blockType = block.getType();

        List<CustomStation> matching = stationManager.getAll().stream()
                .filter(station -> matchesMaterial(station.triggerBlockMaterial(), blockType))
                .toList();

        if (matching.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();

        CustomStation station = resolveStation(matching, block, player);

        // Sin estación que abrir el bloque sigue siendo el de siempre: antes el clic se cancelaba igual
        // y una mesa de herrería sin la estructura de la forja no servía ni para mejorar a netherita.
        if (station == null) {
            return;
        }

        event.setCancelled(true);

        // Lo que hay dentro es de quien puede construir ahí, como un cofre en un claim.
        if (requireBuildPermission && !Protection.canPlace(player, List.of(block), null)) {
            lang.send(player, "station.protected");
            return;
        }

        StationRuntime runtime = runtimeRegistry.getOrCreate(station.id(), block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(), station.inventorySize(), station.guiTitle());

        StationOpenEvent openEvent = new StationOpenEvent(player, station, runtime.key());
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) {
            return;
        }

        // No pisar el "dueño" de una receta que ya está en curso — si un jugador
        // distinto abre la estación mientras otro la dejó procesando, no debería
        // llevarse el xp/descubrimiento/calidad de un trabajo que no empezó ni pagó.
        if (!runtime.isProcessing()) {
            runtime.setLastPlayerId(player.getUniqueId());
        }
        player.openInventory(runtime.inventory());
        // El menú es un cofre sin marcas: se le dice dónde va cada cosa (ranuras contadas desde 1).
        lang.send(player, station.fuelSlot() >= 0 ? "station.slots_hint_fuel" : "station.slots_hint",
                "ingredients", station.ingredientSlots().stream().map(slot -> String.valueOf(slot + 1))
                        .collect(java.util.stream.Collectors.joining(", ")),
                "fuel", station.fuelSlot() + 1, "output", station.outputSlot() + 1);
    }

    /** @return la estación a abrir, o null si ninguna candidata aplica (ya se le avisó al jugador por qué). */
    private CustomStation resolveStation(List<CustomStation> candidates, Block block, Player player) {

        List<CustomStation> structured = new ArrayList<>();
        List<CustomStation> plain = new ArrayList<>();

        for (CustomStation candidate : candidates) {
            (candidate.structureRequirements().isEmpty() ? plain : structured).add(candidate);
        }

        for (CustomStation candidate : structured) {
            if (structureDetector.matches(block, candidate.structureRequirements())) {
                return candidate;
            }
        }

        if (!plain.isEmpty()) {
            return plain.get(0);
        }

        // El aviso solo si la estructura está a medias: quien usa el bloque suelto no quiere montar nada.
        if (!structured.isEmpty() && structured.get(0).structureRequirements().stream()
                .anyMatch(piece -> structureDetector.matches(block, List.of(piece)))) {
            StructureRequirement req = structureDetector.findMissing(block, structured.get(0).structureRequirements())
                    .orElseThrow();
            player.sendMessage(ComponentUtils.parseWithDefault(
                    lang.raw("station.structure_incomplete", "material", req.material(), "dx", req.dx(), "dy",
                            req.dy(), "dz", req.dz()), NamedTextColor.RED));
        }

        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!Protection.testing()) {
            release(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(this::release);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(this::release);
    }

    /**
     * Al desaparecer el bloque, lo que guardaba cae al suelo. Antes se quedaba en esas coordenadas,
     * fuera del alcance de su dueño, y la estación seguía en memoria y en disco para siempre.
     */
    private void release(Block block) {

        runtimeRegistry.get(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).ifPresent(runtime -> {

            List.copyOf(runtime.inventory().getViewers()).forEach(HumanEntity::closeInventory);

            Location at = block.getLocation().add(0.5, 0.5, 0.5);
            Stream.concat(Arrays.stream(runtime.inventory().getContents()), runtime.consumedForCurrentRecipe().stream())
                    .filter(item -> item != null && !item.getType().isAir())
                    .forEach(item -> block.getWorld().dropItemNaturally(at, item));

            runtime.inventory().clear();
            runtime.clearRecipe();

            // Un nivel mejorado se pagó: se conserva en ese sitio por si vuelven a poner el bloque.
            if (runtime.tier() <= 1) {
                runtimeRegistry.remove(runtime.world(), runtime.x(), runtime.y(), runtime.z());
            }
        });
    }

    private boolean matchesMaterial(String configured, Material actual) {

        if (configured == null || configured.isBlank()) {
            return false;
        }

        try {
            return Material.valueOf(configured.trim().toUpperCase(Locale.ROOT)) == actual;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
