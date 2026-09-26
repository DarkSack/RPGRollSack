package com.sack.rpgroll.extras.backpack;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Abre, pinta y guarda las mochilas. Una mochila solo la puede tener abierta
 * una persona a la vez: así dos copias del mismo UUID (creativo, un bloque y
 * su ítem) nunca muestran el contenido dos veces.
 */
public class BackpackService {

    public static final String USE_PERMISSION = "rpgrollextras.backpack.use";
    public static final String BYPASS_PERMISSION = "rpgrollextras.backpack.bypass";

    private final BackpackItems items;
    private final BackpackStorage storage;
    private final LangManager lang;
    private final Map<UUID, BackpackHolder> open = new HashMap<>();
    private BackpackSettings settings = BackpackSettings.DISABLED;

    public BackpackService(BackpackItems items, BackpackStorage storage, LangManager lang) {
        this.items = items;
        this.storage = storage;
        this.lang = lang;
    }

    public void configure(BackpackSettings settings) {
        this.settings = settings;
        items.configure(settings);
    }

    public BackpackSettings settings() {
        return settings;
    }

    public BackpackItems items() {
        return items;
    }

    public boolean enabled() {
        return settings.enabled();
    }

    // ---------------------------------------------------------------- abrir

    /** Abre la mochila que está en la casilla {@code slot} del inventario del jugador. */
    public void openItem(Player player, int slot) {

        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItem(slot);

        if (!items.isBackpack(item) || !checkUse(player)) {
            return;
        }

        Optional<BackpackTier> tier = tierOf(player, items.tierId(item).orElse(""));

        if (tier.isEmpty()) {
            return;
        }

        UUID id = items.ensureId(item);
        inventory.setItem(slot, item);
        open(player, tier.get(), id, null);
    }

    /** Abre la mochila puesta en el bloque. */
    public void openBlock(Player player, Block block) {

        if (!(block.getState() instanceof TileState state) || !checkUse(player)) {
            return;
        }

        Optional<String> tierId = items.tierId(state.getPersistentDataContainer());
        Optional<UUID> id = items.id(state.getPersistentDataContainer());

        if (tierId.isEmpty() || id.isEmpty()) {
            return;
        }

        if (!mayTouchPlaced(player, state)) {
            lang.send(player, "backpack.placed_denied");
            return;
        }

        tierOf(player, tierId.get()).ifPresent(tier -> open(player, tier, id.get(), block));
    }

    private boolean checkUse(Player player) {

        if (!settings.enabled()) {
            lang.send(player, "backpack.disabled");
            return false;
        }

        if (!player.hasPermission(USE_PERMISSION)) {
            lang.send(player, "backpack.no_permission");
            return false;
        }

        return true;
    }

    private Optional<BackpackTier> tierOf(Player player, String tierId) {

        Optional<BackpackTier> tier = settings.tier(tierId);

        if (tier.isEmpty()) {
            lang.send(player, "backpack.unknown_tier", "tier", tierId);
        }

        return tier;
    }

    private void open(Player player, BackpackTier tier, UUID id, Block block) {

        if (open.containsKey(id)) {
            lang.send(player, "backpack.busy");
            return;
        }

        BackpackData data = storage.get(id);

        if (!mayAccess(player, data)) {
            lang.send(player, "backpack.not_owner", "owner", data.ownerName());
            storage.evict(id);
            return;
        }

        List<ItemStack> leftover = data.fit(tier.slots());
        if (!leftover.isEmpty()) {
            giveOrDrop(player, leftover);
            lang.send(player, "backpack.overflow", "count", leftover.size());
        }

        BackpackHolder holder = new BackpackHolder(player, data, tier, block);
        Inventory inventory = Bukkit.createInventory(holder, BackpackPages.inventorySize(tier.slots()),
                BackpackItems.text(BackpackItems.fill(settings.title().replace("{name}", tier.name()), tier,
                        data.ownerName())));
        holder.attach(inventory);

        render(holder);
        open.put(id, holder);
        player.openInventory(inventory);
        sound(player, settings.sounds().open());
    }

    /**
     * Una mochila en el suelo sin ligar la toca solo quien la puso (con
     * placed-access: placer): las cabezas no son contenedores para los plugins
     * de protección, y sin esto cualquiera la vaciaría dentro de un claim ajeno.
     */
    public boolean mayTouchPlaced(Player player, TileState state) {

        if (!settings.placedPlacerOnly() || player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }

        UUID placer = items.placer(state.getPersistentDataContainer()).orElse(null);
        return placer == null || placer.equals(player.getUniqueId());
    }

    public boolean mayAccess(Player player, BackpackData data) {
        return !data.isBound() || data.owner().equals(player.getUniqueId()) || player.hasPermission(BYPASS_PERMISSION);
    }

    // ---------------------------------------------------------------- pintar

    void render(BackpackHolder holder) {

        Inventory inventory = holder.getInventory();
        BackpackSettings.Gui gui = settings.gui();
        int buttons = holder.buttonRowStart();

        for (int slot = 0; slot < buttons; slot++) {
            inventory.setItem(slot, holder.isContentSlot(slot)
                    ? holder.data().get(holder.index(slot)) : button(gui.locked(), holder, 0));
        }

        for (int column = 0; column < 9; column++) {
            inventory.setItem(buttons + column, button(gui.filler(), holder, 0));
        }

        if (gui.infoSlot() >= 0) {
            inventory.setItem(buttons + gui.infoSlot(), button(gui.info(), holder, 0));
        }

        if (holder.pages() > 1) {
            for (int page = 0; page < holder.pages(); page++) {
                BackpackButton look = page == holder.page() ? gui.tabSelected() : gui.tab();
                ItemStack tab = button(look, holder, page + 1);
                tab.setAmount(Math.min(page + 1, 64));
                inventory.setItem(buttons + gui.tabSlots().get(page), tab);
            }
        }

        if (gui.bindSlot() >= 0) {
            inventory.setItem(buttons + gui.bindSlot(),
                    button(holder.data().isBound() ? gui.bindBound() : gui.bindUnbound(), holder, 0));
        }
    }

    private ItemStack button(BackpackButton look, BackpackHolder holder, int page) {

        ItemStack item = new ItemStack(look.material());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(BackpackItems.text(placeholders(look.name(), holder, page)));
        meta.lore(look.lore().stream().map(line -> BackpackItems.text(placeholders(line, holder, page))).toList());
        meta.setHideTooltip(look.name().isBlank() && look.lore().isEmpty());
        item.setItemMeta(meta);
        return item;
    }

    private String placeholders(String text, BackpackHolder holder, int page) {

        String owner = holder.data().isBound() ? holder.data().ownerName() : "";

        return BackpackItems.fill(text.replace("{name}", holder.tier().name()), holder.tier(), owner)
                .replace("{page}", String.valueOf(page > 0 ? page : holder.page() + 1))
                .replace("{current}", String.valueOf(holder.page() + 1))
                .replace("{used}", String.valueOf(holder.data().used()));
    }

    // ---------------------------------------------------------------- acciones

    /** Copia lo que hay en la pestaña visible al contenido de la mochila. */
    void storePage(BackpackHolder holder) {

        Inventory inventory = holder.getInventory();

        for (int slot = 0; slot < holder.buttonRowStart(); slot++) {
            if (holder.isContentSlot(slot)) {
                ItemStack item = inventory.getItem(slot);
                holder.data().set(holder.index(slot), item == null ? null : item.clone());
            }
        }
    }

    public void switchPage(BackpackHolder holder, int page) {

        if (page == holder.page() || page < 0 || page >= holder.pages()) {
            return;
        }

        storePage(holder);
        holder.page(page);
        render(holder);
        sound(holder.viewer(), settings.sounds().page());
    }

    public void toggleBind(BackpackHolder holder) {

        Player player = holder.viewer();
        BackpackData data = holder.data();

        if (data.isBound()) {
            if (!data.owner().equals(player.getUniqueId()) && !player.hasPermission(BYPASS_PERMISSION)) {
                lang.send(player, "backpack.bind_denied", "owner", data.ownerName());
                return;
            }
            data.unbind();
            lang.send(player, "backpack.unbound");
        } else {
            data.bind(player.getUniqueId(), player.getName());
            lang.send(player, "backpack.bound");
        }

        storePage(holder);
        storage.save(data);
        refreshCopies(player, holder.tier(), data);
        render(holder);
        sound(player, settings.sounds().bind());
    }

    /** Actualiza el texto de «ligada a» en las copias de esta mochila que lleve el jugador. */
    private void refreshCopies(Player player, BackpackTier tier, BackpackData data) {

        PlayerInventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (items.id(item).filter(data.id()::equals).isPresent()) {
                inventory.setItem(slot, items.create(tier, data.id(), data.ownerName()));
            }
        }
    }

    /**
     * Mete un ítem en los espacios libres de la pestaña visible (el shift-clic
     * se hace a mano: el de vanilla podría apilarlo sobre los botones).
     *
     * @return lo que no cupo
     */
    public ItemStack moveIntoPage(BackpackHolder holder, ItemStack item) {

        Inventory inventory = holder.getInventory();
        int remaining = item.getAmount();

        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < holder.buttonRowStart() && remaining > 0; slot++) {

                if (!holder.isContentSlot(slot)) {
                    continue;
                }

                ItemStack current = inventory.getItem(slot);

                if (pass == 0 && current != null && current.isSimilar(item)) {
                    int moved = Math.min(remaining, current.getMaxStackSize() - current.getAmount());
                    if (moved > 0) {
                        current.setAmount(current.getAmount() + moved);
                        inventory.setItem(slot, current);
                        remaining -= moved;
                    }
                } else if (pass == 1 && (current == null || current.getType().isAir())) {
                    ItemStack placed = item.clone();
                    int moved = Math.min(remaining, item.getMaxStackSize());
                    placed.setAmount(moved);
                    inventory.setItem(slot, placed);
                    remaining -= moved;
                }
            }
        }

        if (remaining <= 0) {
            return null;
        }

        ItemStack left = item.clone();
        left.setAmount(remaining);
        return left;
    }

    public boolean isForbidden(ItemStack item) {
        return item != null && !item.getType().isAir()
                && (items.isBackpack(item) || settings.forbidden().contains(item.getType()));
    }

    // ---------------------------------------------------------------- cerrar

    public void onClose(BackpackHolder holder) {

        if (open.get(holder.data().id()) != holder) {
            return;
        }

        storePage(holder);
        storage.save(holder.data());
        open.remove(holder.data().id());
        storage.evict(holder.data().id());
        sound(holder.viewer(), settings.sounds().close());
    }

    /** Cierra a quien tenga abierta esta mochila (antes de romper su bloque). */
    public void closeIfOpen(UUID id) {
        BackpackHolder holder = open.get(id);
        if (holder != null) {
            holder.viewer().closeInventory();
            onClose(holder);
        }
    }

    public void closeAll() {
        new ArrayList<>(open.values()).forEach(holder -> {
            holder.viewer().closeInventory();
            onClose(holder);
        });
    }

    /** El ítem que representa la mochila {@code id} (al romper su bloque o mejorarla). */
    public ItemStack itemFor(BackpackTier tier, UUID id) {
        BackpackData data = peek(id);
        return items.create(tier, id, data.isBound() ? data.ownerName() : null);
    }

    /** Lee la mochila sin dejarla en memoria (salvo que esté abierta). */
    public BackpackData peek(UUID id) {
        BackpackData data = storage.get(id);
        if (!open.containsKey(id)) {
            storage.evict(id);
        }
        return data;
    }

    // ---------------------------------------------------------------- utilidades

    private static void giveOrDrop(Player player, List<ItemStack> stacks) {
        Location location = player.getLocation();
        player.getInventory().addItem(stacks.toArray(ItemStack[]::new)).values()
                .forEach(left -> player.getWorld().dropItemNaturally(location, left));
    }

    private static void sound(Player player, String key) {
        if (key != null && !key.isBlank()) {
            player.playSound(player.getLocation(), key, 0.8f, 1f);
        }
    }

}
