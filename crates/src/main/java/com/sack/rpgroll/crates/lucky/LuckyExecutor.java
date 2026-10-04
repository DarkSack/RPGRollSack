package com.sack.rpgroll.crates.lucky;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hace lo que dice un {@link LuckyOutcome} al romper un lucky block.
 * <p>
 * Nada de lo que hace rompe el mundo: las explosiones (también la TNT) no
 * rompen bloques ni queman, el rayo es solo efecto (el daño lo pone la
 * acción), la jaula ocupa solo aire y se quita sola sin soltar nada, y las
 * flechas no se pueden recoger.
 */
public class LuckyExecutor implements Listener {

    private static final Color[] FIREWORK_COLORS = {Color.YELLOW, Color.ORANGE, Color.AQUA, Color.LIME,
            Color.FUCHSIA, Color.WHITE};

    private final Plugin plugin;
    private final LangManager lang;
    private final LuckyManager manager;
    private final LuckyItems items;
    private final NamespacedKey harmlessKey;

    /** Bloques de jaula puestos ahora mismo, con lo que había antes (aire o agua). */
    private final Map<Block, BlockData> temporary = new HashMap<>();

    public LuckyExecutor(Plugin plugin, LangManager lang, LuckyManager manager, LuckyItems items) {
        this.plugin = plugin;
        this.lang = lang;
        this.manager = manager;
        this.items = items;
        this.harmlessKey = new NamespacedKey(plugin, "lucky-harmless");
    }

    /** Ejecuta el resultado en {@code block} (ya roto) para {@code player}. */
    public void run(Player player, Block block, LuckyBlock lucky, LuckyOutcome outcome) {

        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        World world = block.getWorld();

        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 30, 0.3, 0.3, 0.3, 0.3);
        switch (outcome.luck()) {
            case GOOD -> {
                world.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                player.sendActionBar(lang.component("lucky.good"));
            }
            case BAD -> {
                world.playSound(center, Sound.ENTITY_WITCH_CELEBRATE, 1f, 0.9f);
                player.sendActionBar(lang.component("lucky.bad"));
            }
            case NEUTRAL -> world.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.4f);
        }

        if (outcome.message() != null && !outcome.message().isBlank()) {
            player.sendMessage(ComponentUtils.parse(placeholders(outcome.message(), player, block)));
        }
        if (outcome.announce()) {
            Bukkit.broadcast(lang.component("lucky.announce", "player", player.getName(),
                    "block", lucky.displayName(), "outcome", outcome.message() == null ? outcome.id()
                            : outcome.message()));
        }

        for (LuckyAction action : outcome.actions()) {
            try {
                execute(player, block, center, action);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("✘ Lucky block '" + lucky.id() + "', resultado '" + outcome.id()
                        + "', acción " + action.type() + ": " + e.getMessage());
            }
        }
    }

    private void execute(Player player, Block block, Location center, LuckyAction action) {

        ThreadLocalRandom random = ThreadLocalRandom.current();
        World world = center.getWorld();

        switch (action.type()) {
            case DROP -> drop(center, action, random);
            case RAIN -> rain(player, action, random);
            case COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    placeholders(action.text("command", ""), player, block));
            case MONEY -> {
                int amount = action.amount("amount", 0, random);
                if (amount > 0 && LuckyMoney.deposit(player, amount)) {
                    player.sendMessage(lang.component("lucky.money", "amount", amount));
                }
            }
            case XP -> {
                int amount = action.amount("amount", 10, random);
                world.spawn(center, ExperienceOrb.class, orb -> orb.setExperience(amount));
            }
            case LUCKY -> manager.get(action.text("lucky", "")).ifPresent(other ->
                    world.dropItemNaturally(center, items.create(other, action.amount("amount", 1, random))));
            case MOB -> mobs(center, action, random);
            case POTION -> {
                PotionEffectType type = Registry.EFFECT.get(key(action.text("effect", "speed")));
                if (type != null) {
                    player.addPotionEffect(new PotionEffect(type, (int) (action.number("seconds", 10) * 20),
                            Math.max(0, (int) action.number("level", 1) - 1)));
                }
            }
            case EXPLOSION -> explosion(player, center, action);
            case LIGHTNING -> {
                world.strikeLightningEffect(player.getLocation());
                double damage = action.number("damage", 0);
                if (damage > 0) {
                    player.damage(damage);
                }
            }
            case LAUNCH -> player.setVelocity(player.getVelocity().add(new Vector(0, action.number("power", 1.2), 0)));
            case CAGE -> cage(player, action);
            case ARROWS -> arrows(player, action, random);
            case FIREWORK -> {
                int amount = action.amount("amount", 3, random);
                for (int i = 0; i < amount; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> firework(center), i * 6L);
                }
            }
            case MESSAGE -> player.sendMessage(ComponentUtils.parse(placeholders(action.text("text", ""), player,
                    block)));
            case TITLE -> player.showTitle(Title.title(
                    ComponentUtils.parse(placeholders(action.text("title", ""), player, block)),
                    ComponentUtils.parse(placeholders(action.text("subtitle", ""), player, block))));
            case SOUND -> playSound(center, action);
            case PARTICLE -> {
                Particle particle = Registry.PARTICLE_TYPE.get(key(action.text("particle", "happy_villager")));
                if (particle != null && particle.getDataType() == Void.class) {
                    world.spawnParticle(particle, center, action.amount("count", 20, random), 0.4, 0.4, 0.4, 0.05);
                }
            }
        }
    }

    // ---------------------------------------------------------------- acciones

    private void drop(Location center, LuckyAction action, ThreadLocalRandom random) {

        ItemStack item = item(action, action.amount("amount", 1, random));
        if (item == null) {
            return;
        }

        boolean fake = action.flag("fake", false);
        for (ItemStack stack : split(item)) {
            Item dropped = center.getWorld().dropItemNaturally(center, stack);
            if (fake) {
                // Se ve, pero no se puede coger y se va enseguida: una broma.
                dropped.setPickupDelay(Short.MAX_VALUE);
                dropped.setTicksLived(1);
                Bukkit.getScheduler().runTaskLater(plugin, dropped::remove, 60L);
            }
        }
    }

    private void rain(Player player, LuckyAction action, ThreadLocalRandom random) {

        int amount = action.amount("amount", 16, random);
        double radius = action.number("radius", 3);
        double height = action.number("height", 8);
        ItemStack single = item(action, 1);
        if (single == null) {
            return;
        }

        Location base = player.getLocation();
        for (int i = 0; i < amount; i++) {
            long delay = i * 2L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location at = base.clone().add(random.nextDouble(-radius, radius), height,
                        random.nextDouble(-radius, radius));
                base.getWorld().dropItem(at, single.clone());
            }, delay);
        }
    }

    private void mobs(Location center, LuckyAction action, ThreadLocalRandom random) {

        EntityType type;
        try {
            type = EntityType.valueOf(action.text("entity", "ZOMBIE").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Lucky block: entidad desconocida '" + action.text("entity", "") + "'");
            return;
        }
        if (type.getEntityClass() == null || !type.isSpawnable()) {
            return;
        }

        String name = action.text("name", null);
        boolean baby = action.flag("baby", false);
        int amount = action.amount("amount", 1, random);

        for (int i = 0; i < amount; i++) {
            Location at = center.clone().add(random.nextDouble(-1.5, 1.5), 0, random.nextDouble(-1.5, 1.5));
            at.setY(center.getBlockY());
            center.getWorld().spawnEntity(at, type, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                if (name != null) {
                    entity.customName(ComponentUtils.parse(name));
                    entity.setCustomNameVisible(true);
                }
                if (baby && entity instanceof Ageable ageable) {
                    ageable.setBaby();
                }
            });
        }
    }

    private void explosion(Player player, Location center, LuckyAction action) {

        float power = (float) action.number("power", 3);
        boolean fire = action.flag("fire", false);
        int delay = (int) action.number("delay", 0);

        if (delay <= 0) {
            center.getWorld().createExplosion(center, power, fire, false, player);
            return;
        }

        center.getWorld().spawn(center, TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(delay);
            tnt.setYield(power);
            tnt.setIsIncendiary(fire);
            tnt.setSource(player);
            tnt.getPersistentDataContainer().set(harmlessKey, PersistentDataType.BYTE, (byte) 1);
        });
    }

    private void cage(Player player, LuckyAction action) {

        Material material = Material.matchMaterial(action.text("block", "GLASS"));
        // Un bloque que tapa la luz convertiría en tierra la hierba de debajo mientras dura la jaula.
        if (material == null || !material.isBlock() || material.isOccluding()) {
            material = Material.GLASS;
        }
        BlockData data = material.createBlockData();
        long ticks = (long) (action.number("seconds", 6) * 20);

        Block feet = player.getLocation().getBlock();
        List<Block> placed = new ArrayList<>();
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean inside = dx == 0 && dz == 0 && (dy == 0 || dy == 1);
                    if (inside) {
                        continue;
                    }
                    Block block = feet.getRelative(dx, dy, dz);
                    if ((block.getType().isAir() || block.getType() == Material.WATER) && !temporary.containsKey(block)) {
                        temporary.put(block, block.getBlockData());
                        block.setBlockData(data, false);
                        placed.add(block);
                    }
                }
            }
        }
        // Centrado para que no quede medio dentro de un cristal.
        Location centered = feet.getLocation().add(0.5, 0, 0.5);
        centered.setYaw(player.getLocation().getYaw());
        centered.setPitch(player.getLocation().getPitch());
        player.teleport(centered);

        Bukkit.getScheduler().runTaskLater(plugin, () -> placed.forEach(this::restore), ticks);
    }

    private void arrows(Player player, LuckyAction action, ThreadLocalRandom random) {

        int amount = action.amount("amount", 12, random);
        double height = action.number("height", 10);
        Location base = player.getLocation();

        for (int i = 0; i < amount; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location at = base.clone().add(random.nextDouble(-2, 2), height, random.nextDouble(-2, 2));
                Arrow arrow = base.getWorld().spawn(at, Arrow.class);
                arrow.setVelocity(new Vector(0, -1.2, 0));
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                Bukkit.getScheduler().runTaskLater(plugin, arrow::remove, 100L);
            }, i * 3L);
        }
    }

    private static void firework(Location center) {

        ThreadLocalRandom random = ThreadLocalRandom.current();
        center.getWorld().spawn(center.clone().add(0, 0.5, 0), Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)])
                    .withColor(FIREWORK_COLORS[random.nextInt(FIREWORK_COLORS.length)])
                    .withFade(FIREWORK_COLORS[random.nextInt(FIREWORK_COLORS.length)])
                    .trail(true).flicker(random.nextBoolean()).build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        });
    }

    private void playSound(Location center, LuckyAction action) {

        String name = action.text("sound", "entity.player.levelup").trim();
        float volume = (float) action.number("volume", 1);
        float pitch = (float) action.number("pitch", 1);

        // "entity.player.levelup" va directo; "ENTITY_PLAYER_LEVELUP" se busca como en los crates.
        if (name.contains(".") || name.contains(":")) {
            center.getWorld().playSound(center, name.toLowerCase(Locale.ROOT), volume, pitch);
            return;
        }
        Sound sound = Registry.SOUNDS.stream()
                .filter(candidate -> Registry.SOUNDS.getKeyOrThrow(candidate).getKey().replace('.', '_')
                        .equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (sound != null) {
            center.getWorld().playSound(center, sound, volume, pitch);
        }
    }

    // ---------------------------------------------------------------- utilidades

    /** El ítem vanilla de la acción (item, name, enchantments: "sharpness:3,unbreaking:2"). */
    private ItemStack item(LuckyAction action, int amount) {

        Material material = Material.matchMaterial(action.text("item", "STONE"));
        if (material == null || !material.isItem() || material.isAir() || amount <= 0) {
            plugin.getLogger().warning("✘ Lucky block: ítem desconocido '" + action.text("item", "") + "'");
            return null;
        }

        ItemStack item = new ItemStack(material, amount);
        String name = action.text("name", null);
        String enchantments = action.text("enchantments", null);
        if (name == null && enchantments == null) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (name != null) {
            meta.displayName(ComponentUtils.parse(name).decoration(TextDecoration.ITALIC, false));
        }
        if (enchantments != null) {
            for (String part : enchantments.split(",")) {
                String[] pieces = part.trim().split(":");
                Enchantment enchantment = Registry.ENCHANTMENT.get(key(pieces[0]));
                if (enchantment != null) {
                    int level = pieces.length > 1 ? Integer.parseInt(pieces[1].trim()) : 1;
                    meta.addEnchant(enchantment, level, true);
                }
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private static List<ItemStack> split(ItemStack item) {
        List<ItemStack> stacks = new ArrayList<>();
        int left = item.getAmount();
        while (left > 0) {
            ItemStack stack = item.clone();
            stack.setAmount(Math.min(left, item.getMaxStackSize()));
            left -= stack.getAmount();
            stacks.add(stack);
        }
        return stacks;
    }

    private static NamespacedKey key(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.contains(":") ? NamespacedKey.fromString(value) : NamespacedKey.minecraft(value);
    }

    static String placeholders(String text, Player player, Block block) {
        return text.replace("{player}", player.getName())
                .replace("{x}", String.valueOf(block.getX()))
                .replace("{y}", String.valueOf(block.getY()))
                .replace("{z}", String.valueOf(block.getZ()))
                .replace("{world}", block.getWorld().getName());
    }

    // ---------------------------------------------------------------- lo temporal

    private void restore(Block block) {
        BlockData before = temporary.remove(block);
        if (before != null) {
            block.setBlockData(before, false);
        }
    }

    /** Al apagar el servidor no puede quedar ninguna jaula. */
    public void restoreAll() {
        new ArrayList<>(temporary.keySet()).forEach(this::restore);
    }

    /** Una jaula se puede romper para salir, pero no suelta nada. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCageBreak(BlockBreakEvent event) {
        if (temporary.remove(event.getBlock()) != null) {
            event.setDropItems(false);
            event.setExpToDrop(0);
        }
    }

    /** La TNT de un lucky block hace daño, pero no rompe bloques. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity.getPersistentDataContainer().has(harmlessKey)) {
            event.blockList().clear();
        }
    }

    /** Para quien lo necesite: si un bloque es parte de una jaula ahora mismo. */
    public boolean isTemporary(Block block) {
        return temporary.containsKey(block);
    }

}
