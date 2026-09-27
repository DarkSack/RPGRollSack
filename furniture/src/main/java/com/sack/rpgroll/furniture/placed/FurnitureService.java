package com.sack.rpgroll.furniture.placed;

import com.sack.rpgroll.furniture.FurnitureKeys;
import com.sack.rpgroll.furniture.FurnitureSettings;
import com.sack.rpgroll.furniture.core.DisplaySettings;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.core.FurnitureManager;
import com.sack.rpgroll.furniture.core.Hitbox;
import com.sack.rpgroll.furniture.core.Offset;
import com.sack.rpgroll.furniture.core.Surface;
import com.sack.rpgroll.furniture.function.FurnitureFunctions;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.ShelfSlot;
import com.sack.rpgroll.furniture.item.FurnitureItems;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Colocar, retirar, girar, teñir y cambiar de estado los muebles del mundo.
 * <p>
 * Un mueble es un ItemDisplay en el centro de su bloque, que lleva en el PDC todo lo suyo, más
 * lo que ocupa: barreras (se chocan) o una Interaction (se atraviesa), un bloque de luz y los
 * objetos que exhiba. Las comprobaciones de permiso y de protección se hacen antes de tocar el
 * mundo; si algo falla, no queda nada a medias.
 */
public class FurnitureService {

    /** Por qué no se pudo colocar (la clave de idioma que se le enseña al jugador). */
    public enum PlaceResult {
        OK("place.ok"),
        WRONG_SURFACE("place.wrong_surface"),
        BLOCKED("place.blocked"),
        ENTITY_IN_WAY("place.entity_in_way"),
        CHUNK_LIMIT("place.chunk_limit"),
        TYPE_LIMIT("place.type_limit"),
        NO_PERMISSION("place.no_permission"),
        PROTECTED("place.protected");

        public final String langKey;

        PlaceResult(String langKey) {
            this.langKey = langKey;
        }
    }

    public enum RemoveResult {
        OK,
        NOT_OWNER,
        PROTECTED
    }

    private final FurnitureKeys keys;
    private final FurnitureManager manager;
    private final FurnitureItems items;
    private final FurnitureIndex index;
    private final Supplier<FurnitureSettings> settings;

    /** Se llama antes de retirar un mueble: bajar a quien esté sentado, cerrar su almacén... */
    private final List<java.util.function.Consumer<PlacedFurniture>> beforeRemove = new ArrayList<>();

    public FurnitureService(FurnitureKeys keys, FurnitureManager manager, FurnitureItems items, FurnitureIndex index,
            Supplier<FurnitureSettings> settings) {
        this.keys = keys;
        this.manager = manager;
        this.items = items;
        this.index = index;
        this.settings = settings;
    }

    public void onBeforeRemove(java.util.function.Consumer<PlacedFurniture> hook) {
        beforeRemove.add(hook);
    }

    public FurnitureKeys keys() {
        return keys;
    }

    public FurnitureIndex index() {
        return index;
    }

    public FurnitureManager manager() {
        return manager;
    }

    public FurnitureItems items() {
        return items;
    }

    public FurnitureSettings settings() {
        return settings.get();
    }

    public Optional<FurnitureDefinition> definition(PlacedFurniture furniture) {
        return manager.get(furniture.furnitureId());
    }

    // ---------------------------------------------------------------- colocar

    /**
     * La cara efectiva del clic: sobre hierba alta, nieve fina y demás bloques que se reemplazan,
     * el mueble va en ese mismo bloque y apoyado en el suelo, como un bloque vanilla.
     */
    public static BlockFace effectiveFace(Block clicked, BlockFace face) {
        return clicked.isReplaceable() ? BlockFace.UP : face;
    }

    /** Dónde va el mueble al hacer clic en {@code clicked} por su cara {@code face}. */
    public static Block anchorFor(Block clicked, BlockFace face) {
        return clicked.isReplaceable() ? clicked : clicked.getRelative(face);
    }

    /** Hacia dónde mira el mueble: al jugador si va en el suelo o el techo, hacia fuera en una pared. */
    public static float yawFor(FurnitureDefinition def, Surface surface, BlockFace face, float playerYaw) {

        if (surface == Surface.WALL) {
            return switch (face) {
                case SOUTH -> 0f;
                case WEST -> 90f;
                case NORTH -> 180f;
                default -> 270f;
            };
        }
        return def.placement().snap(playerYaw + 180f);
    }

    /** Las casillas que ocupará el mueble. */
    public static List<Block> cellsFor(FurnitureDefinition def, Block anchor, float yaw) {

        List<Block> cells = new ArrayList<>();
        if (def.hitbox().type() == Hitbox.Type.INTERACTION) {
            cells.add(anchor);
            return cells;
        }
        for (Offset offset : def.hitbox().blocks()) {
            int[] d = offset.rotateBlock(yaw);
            Block cell = anchor.getRelative(d[0], d[1], d[2]);
            if (!cells.contains(cell)) {
                cells.add(cell);
            }
        }
        return cells;
    }

    public PlaceResult place(Player player, FurnitureDefinition def, String variantId, Block clicked, BlockFace face,
            ItemStack hand) {

        Block anchor = anchorFor(clicked, face);
        face = effectiveFace(clicked, face);
        Surface surface = Surface.of(face);
        if (!def.placement().allows(surface)) {
            return PlaceResult.WRONG_SURFACE;
        }
        if (def.permission() != null && !player.hasPermission(def.permission())) {
            return PlaceResult.NO_PERMISSION;
        }

        float yaw = yawFor(def, surface, face, player.getLocation().getYaw());
        List<Block> cells = cellsFor(def, anchor, yaw);

        for (Block cell : cells) {
            if (!cell.isReplaceable() || index.isOccupied(cell) || cell.getY() >= cell.getWorld().getMaxHeight()
                    || cell.getY() < cell.getWorld().getMinHeight()) {
                return PlaceResult.BLOCKED;
            }
            if (def.hitbox().type() == Hitbox.Type.BARRIER && hasLivingInside(cell)) {
                return PlaceResult.ENTITY_IN_WAY;
            }
        }

        boolean bypass = player.hasPermission("rpgroll.furniture.bypass");
        BlockKey.ChunkKey chunk = BlockKey.of(anchor).chunk();
        if (!bypass) {
            int limit = settings.get().chunkLimit();
            if (limit > 0 && index.countInChunk(chunk) >= limit) {
                return PlaceResult.CHUNK_LIMIT;
            }
            if (def.placement().limit() > 0 && index.countInChunk(chunk, def.id()) >= def.placement().limit()) {
                return PlaceResult.TYPE_LIMIT;
            }
        }

        if (settings.get().protectionEvents() && !Protection.canPlace(player, cells, hand)) {
            return PlaceResult.PROTECTED;
        }

        spawn(def, def.resolveVariant(variantId), anchor, surface, yaw, cells, player.getUniqueId());
        playSound(anchor, def.sounds().place());
        return PlaceResult.OK;
    }

    /** Pone el mueble sin comprobar nada (el llamante ya lo hizo, o es un admin). */
    public PlacedFurniture spawn(FurnitureDefinition def, String variant, Block anchor, Surface surface, float yaw,
            List<Block> cells, UUID owner) {

        Location at = anchor.getLocation().add(0.5, 0.5, 0.5);
        at.setYaw(yaw);
        at.setPitch(0);

        ItemDisplay display = anchor.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(items.displayItem(def, variant, 0));
            applyDisplay(d, def.display());
            d.setPersistent(true);
            d.getPersistentDataContainer().set(keys.id, PersistentDataType.STRING, def.id());
            if (variant != null) {
                d.getPersistentDataContainer().set(keys.variant, PersistentDataType.STRING, variant);
            }
            d.getPersistentDataContainer().set(keys.owner, PersistentDataType.STRING, owner.toString());
        });

        PlacedFurniture furniture = new PlacedFurniture(display, keys);

        if (def.hitbox().type() == Hitbox.Type.BARRIER) {
            cells.forEach(cell -> cell.setType(Material.BARRIER, false));
            furniture.barriers(cells);
        } else {
            Interaction interaction = spawnInteraction(def, anchor, surface, display.getUniqueId());
            furniture.addChild(interaction.getUniqueId());
        }

        applyLight(furniture, def);
        index.add(furniture);
        return furniture;
    }

    private Interaction spawnInteraction(FurnitureDefinition def, Block anchor, Surface surface, UUID parent) {

        // La caja de la Interaction crece hacia arriba desde su posición: en el techo se
        // cuelga desde arriba y en la pared se centra en el bloque.
        Hitbox hitbox = def.hitbox();
        double y = switch (surface) {
            case FLOOR -> anchor.getY();
            case CEILING -> anchor.getY() + 1 - hitbox.height();
            case WALL -> anchor.getY() + Math.max(0, (1 - hitbox.height()) / 2);
        };
        Location at = new Location(anchor.getWorld(), anchor.getX() + 0.5, y, anchor.getZ() + 0.5);

        return anchor.getWorld().spawn(at, Interaction.class, i -> {
            i.setInteractionWidth(hitbox.width());
            i.setInteractionHeight(hitbox.height());
            i.setResponsive(true);
            i.setPersistent(true);
            i.getPersistentDataContainer().set(keys.parent, PersistentDataType.STRING, parent.toString());
        });
    }

    private static boolean hasLivingInside(Block cell) {
        BoundingBox box = BoundingBox.of(cell);
        return !cell.getWorld().getNearbyEntities(box, e -> e instanceof LivingEntity).isEmpty();
    }

    /** Escala, desplazamiento, brillo y distancia de visión del modelo. */
    public static void applyDisplay(ItemDisplay display, DisplaySettings s) {

        display.setItemDisplayTransform(s.transform());
        // El giro del mueble es el de la entidad (su yaw); aquí solo la escala y el
        // desplazamiento. La traslación va en el marco de la entidad, así que gira sola.
        Offset t = s.translation();
        display.setTransformation(new Transformation(
                new Vector3f((float) t.x(), (float) t.y(), (float) t.z()),
                new Quaternionf(), new Vector3f(s.scale(), s.scale(), s.scale()), new Quaternionf()));
        display.setViewRange(s.viewRange());
        display.setShadowRadius(s.shadowRadius());
        display.setBrightness(s.brightness() >= 0 ? new Display.Brightness(s.brightness(), s.brightness()) : null);
    }

    // ---------------------------------------------------------------- retirar

    /**
     * Si {@code player} puede retirar el mueble: dueño (si el config lo exige) y protección del
     * terreno. Null = consola o sistema, siempre puede.
     */
    public RemoveResult canRemove(Player player, PlacedFurniture furniture) {

        if (player == null || player.hasPermission("rpgroll.furniture.bypass")) {
            return RemoveResult.OK;
        }
        if (settings.get().ownerOnly() && furniture.owner() != null && !furniture.ownedBy(player.getUniqueId())) {
            return RemoveResult.NOT_OWNER;
        }
        if (settings.get().protectionEvents() && !Protection.canBreak(player, furniture.anchor())) {
            return RemoveResult.PROTECTED;
        }
        return RemoveResult.OK;
    }

    /**
     * Quita el mueble y todo lo suyo. Con {@code drop}, suelta su ítem, lo que guardaba y lo que
     * exhibía; sin él (limpieza de admin) solo suelta lo que había dentro.
     */
    public void remove(PlacedFurniture furniture, boolean drop) {

        Optional<FurnitureDefinition> def = definition(furniture);
        beforeRemove.forEach(hook -> hook.accept(furniture));

        Location dropAt = furniture.center().add(0, 0.5, 0);

        for (UUID childId : furniture.children()) {
            Entity child = Bukkit.getEntity(childId);
            if (child == null) {
                continue;
            }
            if (child instanceof ItemDisplay shown && shown.getPersistentDataContainer().has(keys.slot)) {
                ItemStack item = shown.getItemStack();
                if (item != null && !item.isEmpty()) {
                    dropAt.getWorld().dropItemNaturally(dropAt, item);
                }
            }
            child.remove();
        }

        for (BlockKey key : furniture.barriers()) {
            Block block = key.block();
            if (block != null && block.getType() == Material.BARRIER) {
                block.setType(Material.AIR, false);
            }
        }

        clearLight(furniture);

        byte[] stored = furniture.storage();
        if (stored != null) {
            for (ItemStack item : ItemStack.deserializeItemsFromBytes(stored)) {
                if (item != null && !item.isEmpty()) {
                    dropAt.getWorld().dropItemNaturally(dropAt, item);
                }
            }
        }

        if (drop) {
            def.ifPresent(d -> dropAt.getWorld().dropItemNaturally(dropAt, items.create(d, furniture.variant(), 1)));
        }
        def.ifPresent(d -> playSound(furniture.anchor(), d.sounds().breakSound()));

        index.remove(furniture.uuid());
        furniture.display().remove();
    }

    /** Retirado por un jugador: en creativo no suelta el ítem. */
    public void removeBy(Player player, PlacedFurniture furniture) {
        remove(furniture, player == null || player.getGameMode() != GameMode.CREATIVE);
    }

    // ---------------------------------------------------------------- girar, teñir, estados

    /** Gira un paso. False si las barreras no caben en la nueva orientación. */
    public boolean rotate(PlacedFurniture furniture, FurnitureDefinition def) {

        float yaw = def.placement().snap(furniture.yaw() + def.placement().step());
        Block anchor = furniture.anchor();
        List<Block> cells = cellsFor(def, anchor, yaw);
        List<BlockKey> current = furniture.barriers();

        if (def.hitbox().type() == Hitbox.Type.BARRIER) {
            for (Block cell : cells) {
                boolean own = current.contains(BlockKey.of(cell));
                if (!own && (!cell.isReplaceable() || index.isOccupied(cell) || hasLivingInside(cell))) {
                    return false;
                }
            }
            beforeRemove.forEach(hook -> hook.accept(furniture));
            for (BlockKey key : current) {
                Block block = key.block();
                if (block != null && block.getType() == Material.BARRIER && !cells.contains(block)) {
                    block.setType(Material.AIR, false);
                }
            }
            cells.forEach(cell -> cell.setType(Material.BARRIER, false));
            furniture.barriers(cells);
        } else {
            beforeRemove.forEach(hook -> hook.accept(furniture));
        }

        furniture.display().setRotation(yaw, 0);
        repositionShelf(furniture, def, yaw);
        index.add(furniture);
        return true;
    }

    public void setVariant(PlacedFurniture furniture, FurnitureDefinition def, String variant) {
        furniture.variant(variant);
        furniture.display().setItemStack(items.displayItem(def, variant, furniture.state()));
    }

    public void setState(PlacedFurniture furniture, FurnitureDefinition def, int state) {

        FurnitureFunctions.States states = def.functions().states();
        int normalized = states == null ? 0 : Math.floorMod(state, states.states().size());
        furniture.state(normalized);
        furniture.display().setItemStack(items.displayItem(def, furniture.variant(), normalized));
        applyLight(furniture, def);
        index.updateState(furniture.uuid(), normalized);

        if (states != null && states.get(normalized).sound() != null) {
            playSound(furniture.anchor(), states.get(normalized).sound());
        }
    }

    // ---------------------------------------------------------------- luz

    /** Pone, cambia o quita el bloque de luz según el estado actual. */
    public void applyLight(PlacedFurniture furniture, FurnitureDefinition def) {

        clearLight(furniture);

        int level = def.functions().lightLevel(furniture.state());
        if (level <= 0) {
            return;
        }

        Offset at = def.functions().light() != null ? def.functions().light().at() : Offset.ZERO;
        int[] d = at.rotateBlock(furniture.yaw());
        Block block = furniture.anchor().getRelative(d[0], d[1], d[2]);

        // Solo en una casilla libre: no se pisa un bloque del jugador ni una barrera del mueble.
        if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR) {
            return;
        }

        Light light = (Light) Material.LIGHT.createBlockData();
        light.setLevel(level);
        block.setBlockData(light, false);
        furniture.light(block);
    }

    private void clearLight(PlacedFurniture furniture) {

        BlockKey key = furniture.light();
        if (key == null) {
            return;
        }
        Block block = key.block();
        if (block != null && block.getType() == Material.LIGHT) {
            block.setType(Material.AIR, false);
        }
        furniture.light(null);
    }

    // ---------------------------------------------------------------- estante

    /** Pone una copia de un objeto en el primer hueco libre. False si no quedan huecos. */
    public boolean shelve(PlacedFurniture furniture, FurnitureDefinition def, ItemStack item) {

        FurnitureFunctions.Shelf shelf = def.functions().shelf();
        if (shelf == null) {
            return false;
        }

        boolean[] used = new boolean[shelf.slots().size()];
        for (ItemDisplay shown : shelved(furniture)) {
            int slot = shown.getPersistentDataContainer().getOrDefault(keys.slot, PersistentDataType.INTEGER, -1);
            if (slot >= 0 && slot < used.length) {
                used[slot] = true;
            }
        }

        for (int slot = 0; slot < used.length; slot++) {
            if (!used[slot]) {
                ItemStack one = item.clone();
                one.setAmount(1);
                ItemDisplay shown = spawnShelved(furniture, shelf.slots().get(slot), slot, one, furniture.yaw());
                furniture.addChild(shown.getUniqueId());
                return true;
            }
        }
        return false;
    }

    /** Quita el último objeto exhibido y lo devuelve (vacío si no había). */
    public Optional<ItemStack> unshelve(PlacedFurniture furniture) {

        List<ItemDisplay> shown = shelved(furniture);
        if (shown.isEmpty()) {
            return Optional.empty();
        }

        ItemDisplay last = shown.stream().max(java.util.Comparator.comparingInt(d ->
                d.getPersistentDataContainer().getOrDefault(keys.slot, PersistentDataType.INTEGER, 0))).orElseThrow();
        ItemStack item = last.getItemStack();
        furniture.removeChild(last.getUniqueId());
        last.remove();
        return Optional.ofNullable(item);
    }

    public List<ItemDisplay> shelved(PlacedFurniture furniture) {

        List<ItemDisplay> out = new ArrayList<>();
        for (UUID childId : furniture.children()) {
            if (Bukkit.getEntity(childId) instanceof ItemDisplay shown
                    && shown.getPersistentDataContainer().has(keys.slot)) {
                out.add(shown);
            }
        }
        return out;
    }

    private ItemDisplay spawnShelved(PlacedFurniture furniture, ShelfSlot slot, int index, ItemStack item, float yaw) {

        Location at = shelfLocation(furniture, slot, yaw);
        return at.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplayTransform.FIXED);
            Quaternionf rotation = slot.flat()
                    ? new Quaternionf(new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0))
                    : new Quaternionf();
            d.setTransformation(new Transformation(new Vector3f(), rotation,
                    new Vector3f(slot.scale(), slot.scale(), slot.scale()), new Quaternionf()));
            d.setPersistent(true);
            d.getPersistentDataContainer().set(keys.parent, PersistentDataType.STRING, furniture.uuid().toString());
            d.getPersistentDataContainer().set(keys.slot, PersistentDataType.INTEGER, index);
        });
    }

    private void repositionShelf(PlacedFurniture furniture, FurnitureDefinition def, float yaw) {

        FurnitureFunctions.Shelf shelf = def.functions().shelf();
        if (shelf == null) {
            return;
        }
        for (ItemDisplay shown : shelved(furniture)) {
            int slot = shown.getPersistentDataContainer().getOrDefault(keys.slot, PersistentDataType.INTEGER, -1);
            if (slot >= 0 && slot < shelf.slots().size()) {
                shown.teleport(shelfLocation(furniture, shelf.slots().get(slot), yaw));
            }
        }
    }

    private static Location shelfLocation(PlacedFurniture furniture, ShelfSlot slot, float yaw) {

        Offset o = slot.position().rotate(yaw);
        Location at = furniture.anchor().getLocation().add(0.5 + o.x(), o.y(), 0.5 + o.z());
        at.setYaw(yaw);
        return at;
    }

    // ---------------------------------------------------------------- utilidades

    public static void playSound(Block at, String sound) {
        if (sound != null && !sound.isBlank()) {
            at.getWorld().playSound(at.getLocation().add(0.5, 0.5, 0.5), sound, SoundCategory.BLOCKS, 1f, 1f);
        }
    }

    /** El mueble al que pertenece una entidad: él mismo, o el padre de su Interaction u objeto. */
    public Optional<PlacedFurniture> fromEntity(Entity entity) {

        PlacedFurniture self = PlacedFurniture.of(entity, keys);
        if (self != null) {
            return Optional.of(self);
        }

        String parent = entity.getPersistentDataContainer().get(keys.parent, PersistentDataType.STRING);
        if (parent == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(PlacedFurniture.of(Bukkit.getEntity(UUID.fromString(parent)), keys));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<PlacedFurniture> at(Block block) {
        return index.entityAt(block).map(d -> new PlacedFurniture(d, keys));
    }
}
