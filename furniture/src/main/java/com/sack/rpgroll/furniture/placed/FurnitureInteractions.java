package com.sack.rpgroll.furniture.placed;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.core.FurnitureVariant;
import com.sack.rpgroll.furniture.function.FurnitureFunctions;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Action;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Trigger;
import com.sack.rpgroll.furniture.gui.FurnitureMenu;
import com.sack.rpgroll.furniture.seat.SeatService;
import com.sack.rpgroll.furniture.storage.StorageService;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;

import java.util.Locale;
import java.util.Optional;

/**
 * Qué pasa al hacer clic derecho en un mueble (el orden está en {@link FurnitureFunctions}).
 * Lo comparten el clic en una barrera y el clic en una Interaction.
 */
public class FurnitureInteractions {

    private final FurnitureService service;
    private final SeatService seats;
    private final StorageService storage;
    private final LangManager lang;

    public FurnitureInteractions(FurnitureService service, SeatService seats, StorageService storage, LangManager lang) {
        this.service = service;
        this.seats = seats;
        this.storage = storage;
        this.lang = lang;
    }

    /** @return true si el clic hizo algo con el mueble (y no debe llegar a nada más) */
    public boolean use(Player player, PlacedFurniture furniture, ItemStack hand) {

        Optional<FurnitureDefinition> found = service.definition(furniture);
        if (found.isEmpty()) {
            lang.send(player, "use.unknown", "id", String.valueOf(furniture.furnitureId()));
            return true;
        }

        FurnitureDefinition def = found.get();
        FurnitureFunctions f = def.functions();
        boolean sneaking = player.isSneaking();
        boolean emptyHand = hand == null || hand.isEmpty();

        if (!player.hasPermission("rpgroll.furniture.use")) {
            return true;
        }

        if (sneaking) {
            if (f.states() != null && f.states().trigger() == Trigger.SNEAK_CLICK) {
                cycleState(furniture, def);
                return true;
            }
            if (runActions(player, furniture, f, Trigger.SNEAK_CLICK)) {
                return true;
            }
            if (emptyHand && service.settings().rotateOnSneak()) {
                if (!mayModify(player, furniture)) {
                    return true;
                }
                if (!service.rotate(furniture, def)) {
                    lang.send(player, "use.cannot_rotate");
                }
                return true;
            }
            // Agachado con algo en la mano: se deja pasar (poner un bloque o un mueble al lado).
            return false;
        }

        if (!emptyHand && dye(player, furniture, def, hand)) {
            return true;
        }

        if (f.shelf() != null) {
            if (!emptyHand && !service.items().read(hand).isPresent()) {
                if (!mayModify(player, furniture)) {
                    return true;
                }
                if (service.shelve(furniture, def, hand)) {
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        hand.setAmount(hand.getAmount() - 1);
                    }
                } else {
                    lang.send(player, "use.shelf_full");
                }
                return true;
            }
            if (emptyHand && !service.shelved(furniture).isEmpty()) {
                if (!mayModify(player, furniture)) {
                    return true;
                }
                service.unshelve(furniture).ifPresent(item ->
                        player.getInventory().addItem(item).values().forEach(left ->
                                player.getWorld().dropItemNaturally(player.getLocation(), left)));
                return true;
            }
        }

        // Con un mueble en la mano se está colocando otro encima o al lado, no usándolo.
        if (!emptyHand && service.items().read(hand).isPresent()) {
            return false;
        }

        if (f.seat() != null) {
            if (!seats.sit(player, furniture, def)) {
                lang.send(player, "use.seat_taken");
            }
            return true;
        }

        if (f.storage() != null) {
            if (f.storage().ownerOnly() && !furniture.ownedBy(player.getUniqueId())
                    && furniture.owner() != null && !player.hasPermission("rpgroll.furniture.bypass")) {
                lang.send(player, "use.storage_private");
                return true;
            }
            storage.open(player, furniture, def);
            return true;
        }

        if (f.trash() != null) {
            storage.openTrash(player, def, furniture.variant());
            return true;
        }

        if (f.workstation() != null) {
            openWorkstation(player, furniture, f.workstation());
            return true;
        }

        if (f.states() != null && f.states().trigger() == Trigger.CLICK) {
            cycleState(furniture, def);
            return true;
        }

        return runActions(player, furniture, f, Trigger.CLICK);
    }

    private void cycleState(PlacedFurniture furniture, FurnitureDefinition def) {
        service.setState(furniture, def, furniture.state() + 1);
    }

    /** Tinte en la mano sobre un mueble que tiene versión de ese color. */
    private boolean dye(Player player, PlacedFurniture furniture, FurnitureDefinition def, ItemStack hand) {

        DyeColor color = dyeColor(hand);
        if (color == null) {
            return false;
        }

        Optional<FurnitureVariant> variant = def.variantForDye(color);
        if (variant.isEmpty()) {
            return false;
        }
        if (variant.get().id().equals(furniture.variant())) {
            return true;
        }
        if (!mayModify(player, furniture)) {
            return true;
        }

        service.setVariant(furniture, def, variant.get().id());
        if (service.settings().consumeDye() && player.getGameMode() != GameMode.CREATIVE) {
            hand.setAmount(hand.getAmount() - 1);
        }
        player.getWorld().playSound(furniture.center(), org.bukkit.Sound.ITEM_DYE_USE, 1f, 1f);
        return true;
    }

    private static DyeColor dyeColor(ItemStack item) {

        String name = item.getType().name();
        // Un ítem propio hecho sobre un tinte (otro modelo, datos de modelo) no tiñe.
        if (!name.endsWith("_DYE") || item.hasItemMeta()
                && (item.getItemMeta().hasCustomModelDataComponent() || item.getItemMeta().hasItemModel())) {
            return null;
        }
        try {
            return DyeColor.valueOf(name.substring(0, name.length() - "_DYE".length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Dueño (si el config lo exige) y protección del terreno; avisa si no. */
    private boolean mayModify(Player player, PlacedFurniture furniture) {

        FurnitureService.RemoveResult result = service.canRemove(player, furniture);
        if (result == FurnitureService.RemoveResult.OK) {
            return true;
        }
        lang.send(player, result == FurnitureService.RemoveResult.NOT_OWNER ? "use.not_owner" : "use.protected");
        return false;
    }

    private void openWorkstation(Player player, PlacedFurniture furniture, FurnitureFunctions.Workstation workstation) {

        if (workstation.carpenter()) {
            new FurnitureMenu(player, service.manager(), service.items(), lang, FurnitureMenu.Mode.CARPENTER,
                    workstation.station()).open();
            return;
        }

        MenuType menu = Registry.MENU.get(NamespacedKey.minecraft(workstation.type().toLowerCase(Locale.ROOT)));
        if (menu == null) {
            lang.send(player, "use.unknown_workstation", "type", workstation.type());
            return;
        }

        Location at = furniture.anchor().getLocation();
        var builder = menu.typed().builder();
        if (builder instanceof org.bukkit.inventory.view.builder.LocationInventoryViewBuilder<?> located) {
            // Sin checkReachable el menú se cerraría al instante: no hay un bloque de
            // mesa de trabajo en esa casilla, hay un mueble.
            located.location(at).checkReachable(false).build(player).open();
        } else {
            builder.build(player).open();
        }
    }

    private boolean runActions(Player player, PlacedFurniture furniture, FurnitureFunctions f, Trigger trigger) {

        boolean any = false;
        Location at = furniture.center();

        for (Action action : f.actions()) {
            if (action.trigger() != trigger) {
                continue;
            }
            any = true;

            if (action.command() != null) {
                String command = action.command()
                        .replace("{player}", player.getName())
                        .replace("{world}", at.getWorld().getName())
                        .replace("{x}", String.valueOf(at.getBlockX()))
                        .replace("{y}", String.valueOf(at.getBlockY()))
                        .replace("{z}", String.valueOf(at.getBlockZ()));
                Bukkit.dispatchCommand(action.console() ? Bukkit.getConsoleSender() : player, command);
            }
            if (action.sound() != null) {
                at.getWorld().playSound(at, action.sound(), org.bukkit.SoundCategory.BLOCKS, 1f, 1f);
            }
            if (action.message() != null) {
                player.sendMessage(ComponentUtils.parse(action.message().replace("{player}", player.getName())));
            }
        }
        return any;
    }
}
