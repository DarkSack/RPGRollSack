package com.sack.rpgroll.crafting.station.listener;

import com.sack.rpgroll.crafting.api.event.StationOpenEvent;
import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.runtime.StationRuntime;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;
import com.sack.rpgroll.crafting.station.structure.StructureDetector;
import com.sack.rpgroll.crafting.station.structure.StructureRequirement;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
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
    private final StructureDetector structureDetector = new StructureDetector();

    public StationBlockInteractListener(CustomStationManager stationManager, StationRuntimeRegistry runtimeRegistry,
            LangManager lang) {
        this.stationManager = stationManager;
        this.runtimeRegistry = runtimeRegistry;
        this.lang = lang;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
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

        event.setCancelled(true);
        Player player = event.getPlayer();

        CustomStation station = resolveStation(matching, block, player);
        if (station == null) {
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

        if (!structured.isEmpty()) {
            StructureRequirement req = structureDetector.findMissing(block, structured.get(0).structureRequirements())
                    .orElseThrow();
            player.sendMessage(Component.text(
                    lang.raw("station.structure_incomplete", "material", req.material(), "dx", req.dx(), "dy",
                            req.dy(), "dz", req.dz()),
                    NamedTextColor.RED));
        }

        return null;
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
