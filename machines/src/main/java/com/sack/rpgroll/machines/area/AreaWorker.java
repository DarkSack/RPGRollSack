package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.block.MachineBreakEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Cada tick, cada tipo de máquina de área gasta como mucho {@code performance.actions-per-tick}
 * acciones entre todas sus máquinas. Como la Cantera: solo con el dueño conectado, el chunk cargado,
 * un cofre al lado y el área en su claim.
 */
public class AreaWorker implements Runnable, Listener, AreaContext {

    private final AreaService service;
    private final Random random = new Random();
    private Location capturing;
    private final List<ItemStack> captured = new ArrayList<>();

    public AreaWorker(AreaService service) {
        this.service = service;
    }

    @Override
    public void run() {
        for (AreaKind kind : service.kinds()) {
            if (!kind.settings().enabled()) {
                continue;
            }
            int budget = kind.settings().actionsPerTick();
            for (AreaMachine machine : List.copyOf(service.store().all())) {
                if (budget <= 0) {
                    break;
                }
                if (machine.kind().equals(kind.id())) {
                    budget -= work(kind, machine, budget);
                }
            }
        }
    }

    private int work(AreaKind kind, AreaMachine machine, int budget) {
        World world = Bukkit.getWorld(machine.world());
        if (world == null || !world.isChunkLoaded(machine.x() >> 4, machine.z() >> 4)) {
            return 0;
        }
        Block block = world.getBlockAt(machine.x(), machine.y(), machine.z());
        if (block.getType() != kind.settings().block()) {
            // Ya no está (la quitó algo que no avisa: WorldEdit, un comando...). Se olvida.
            service.spill(machine, block.getLocation().add(0.5, 0.5, 0.5));
            service.removeFrame(block);
            service.store().remove(machine);
            return 0;
        }
        if (!machine.enabled()) {
            machine.status(AreaStatus.OFF);
            return 0;
        }
        Player owner = Bukkit.getPlayer(machine.owner());
        if (owner == null) {
            machine.status(AreaStatus.NO_OWNER);
            return 0;
        }
        machine.ownerName(owner.getName());
        Optional<Inventory> output = service.output(block);
        if (output.isEmpty()) {
            machine.status(AreaStatus.NO_CHEST);
            return 0;
        }
        if (!service.flush(machine, output.get())) {
            machine.status(AreaStatus.FULL);
            return 0;
        }
        double speed = kind.settings().value("speed", machine);
        machine.credit(Math.min(machine.credit() + speed / 20.0, Math.max(1.0, speed)));
        // El claim se pide al ponerla, pero se puede abandonar después: sin él no trabaja.
        if (machine.credit() >= 1.0 && !service.areaAllowed(machine, world, kind.side(machine))) {
            machine.status(AreaStatus.NO_CLAIM);
            return 0;
        }
        int used = kind.work(machine, block, owner, budget, this);
        if (!machine.buffer().isEmpty() && !service.flush(machine, output.get())) {
            machine.status(AreaStatus.FULL);
        }
        if (used > 0) {
            service.store().dirty();
        }
        return used;
    }

    @Override
    public List<ItemStack> breakAs(Player owner, Block target, ItemStack tool) {
        List<ItemStack> drops = new ArrayList<>();
        capturing = target.getLocation().add(0.5, 0.5, 0.5);
        captured.clear();
        try {
            MachineBreakEvent event = new MachineBreakEvent(target, owner, tool, -1);
            if (!event.callEvent()) {
                return null;
            }
            if (event.isDropItems()) {
                drops.addAll(target.getDrops(tool, owner));
            }
            target.setType(Material.AIR, true);
            drops.addAll(captured);
        } finally {
            capturing = null;
            captured.clear();
        }
        drops.removeIf(item -> item == null || item.getType().isAir() || item.getAmount() <= 0);
        return drops;
    }

    @Override
    public Random random() {
        return random;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (capturing == null || !event.getLocation().getWorld().equals(capturing.getWorld())
                || event.getLocation().distanceSquared(capturing) > 4.0) {
            return;
        }
        captured.add(event.getEntity().getItemStack().clone());
        event.setCancelled(true);
    }
}
