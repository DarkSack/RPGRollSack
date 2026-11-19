package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.fishing.CatchSource;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Pescadora: con agua alrededor, cada cierto tiempo saca una captura (o varias, con más cañas). Pesca
 * lo vanilla y, con RPGRoll-Fishing, una parte de sus peces con calidad limitada.
 */
public class FisherKind extends AreaKind {

    /** Mira el agua en un cuadrado de 5×5 alrededor (y el que pide en claim). */
    private static final int RADIUS = 2;

    private int minWater;
    private List<FishLoot.Entry> fish = List.of();
    private List<FishLoot.Entry> junk = List.of();
    private List<FishLoot.Entry> treasure = List.of();
    private double fishingChance;
    private int maxQuality;

    @Override
    public String id() {
        return "fisher";
    }

    @Override
    public String file() {
        return "fishers.yml";
    }

    @Override
    public String art() {
        return "pescadora";
    }

    @Override
    public Material defaultBlock() {
        return Material.PRISMARINE_BRICKS;
    }

    @Override
    public List<String> upgrades() {
        return List.of("speed", "luck", "rods");
    }

    @Override
    protected void loadExtra(FileConfiguration config, Consumer<String> warn) {
        minWater = Math.max(1, config.getInt("min-water", 8));
        fish = FishLoot.parse(config.getStringList("loot.fish"), warn);
        junk = FishLoot.parse(config.getStringList("loot.junk"), warn);
        treasure = FishLoot.parse(config.getStringList("loot.treasure"), warn);
        fishingChance = Math.clamp(config.getDouble("rpgroll-fishing.chance", 0.25), 0.0, 1.0);
        maxQuality = config.getInt("rpgroll-fishing.max-quality", 2);
    }

    @Override
    public int side(AreaMachine machine) {
        return RADIUS * 2 + 1;
    }

    @Override
    public String placeProblem(Block block) {
        return water(block) < minWater ? "area.fisher.need_water" : null;
    }

    /** Fuentes de agua en el 5×5 de su altura y el de debajo. */
    static int water(Block block) {
        int count = 0;
        for (int dy = -1; dy <= 0; dy++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    Block near = block.getRelative(dx, dy, dz);
                    if (near.getType() == Material.WATER && near.getBlockData() instanceof Levelled level
                            && level.getLevel() == 0) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public int work(AreaMachine machine, Block block, Player owner, int budget, AreaContext context) {
        if (water(block) < minWater) {
            machine.status(AreaStatus.NO_WATER);
            machine.gauge(0);
            return 0;
        }
        machine.status(AreaStatus.WORKING);
        int used = 0;
        while (machine.credit() >= 1.0 && used < budget) {
            machine.credit(machine.credit() - 1.0);
            int rods = Math.max(1, (int) settings().value("rods", machine));
            int luck = (int) settings().value("luck", machine);
            for (int i = 0; i < rods; i++) {
                ItemStack caught = catchOne(block, owner, luck, context);
                if (caught != null) {
                    machine.buffer().add(caught);
                }
            }
            used++;
        }
        machine.gauge(Math.min(1.0, machine.credit()));
        return used;
    }

    private ItemStack catchOne(Block block, Player owner, int luck, AreaContext context) {
        if (context.random().nextDouble() < fishingChance) {
            Optional<ItemStack> special = CatchSource.get()
                    .flatMap(source -> source.roll(owner, waterSpot(block), maxQuality));
            if (special.isPresent()) {
                return special.get();
            }
        }
        List<FishLoot.Entry> list = switch (FishLoot.category(context.random().nextDouble(), luck)) {
            case FISH -> fish;
            case JUNK -> junk;
            case TREASURE -> treasure;
        };
        Material material = FishLoot.pick(list, context.random().nextDouble());
        if (material == null) {
            return null;
        }
        ItemStack item = new ItemStack(material);
        if (material == Material.BOOK || material == Material.BOW || material == Material.FISHING_ROD) {
            // Como el tesoro vanilla: libro, arco y caña salen encantados.
            item = Bukkit.getItemFactory().enchantWithLevels(item, 30, true, context.random());
        }
        return item;
    }

    private static Location waterSpot(Block block) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block near = block.getRelative(dx, 0, dz);
                if (near.getType() == Material.WATER) {
                    return near.getLocation().add(0.5, 0.5, 0.5);
                }
            }
        }
        return block.getLocation().add(0.5, 0, 0.5);
    }

    @Override
    public List<Component> lore(AreaMachine machine, LangManager lang) {
        return List.of(lang.component("area.fisher.status_water", "min", minWater));
    }
}
