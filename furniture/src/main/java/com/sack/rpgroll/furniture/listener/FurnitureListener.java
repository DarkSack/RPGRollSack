package com.sack.rpgroll.furniture.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.item.FurnitureItems;
import com.sack.rpgroll.furniture.placed.FurnitureInteractions;
import com.sack.rpgroll.furniture.placed.FurnitureService;
import com.sack.rpgroll.furniture.placed.FurnitureService.PlaceResult;
import com.sack.rpgroll.furniture.placed.FurnitureService.RemoveResult;
import com.sack.rpgroll.furniture.placed.PlacedFurniture;
import com.sack.rpgroll.furniture.placed.Protection;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Colocar, usar y retirar muebles con el ratón. */
public class FurnitureListener implements Listener {

    private final FurnitureService service;
    private final FurnitureInteractions interactions;
    private final FurnitureItems items;
    private final LangManager lang;

    public FurnitureListener(FurnitureService service, FurnitureInteractions interactions, LangManager lang) {
        this.service = service;
        this.interactions = interactions;
        this.items = service.items();
        this.lang = lang;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        Optional<PlacedFurniture> furniture = service.at(clicked);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            furniture.ifPresent(f -> {
                deny(event);
                tryBreak(player, f);
            });
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (furniture.isPresent() && interactions.use(player, furniture.get(), hand)) {
            deny(event);
            player.swingMainHand();
            return;
        }

        Optional<FurnitureDefinition> placing = items.definition(hand);
        if (placing.isEmpty()) {
            if (items.read(hand).isPresent()) {
                // Un mueble cuyo YAML ya no existe: que no se coloque como bloque ni haga nada.
                deny(event);
                lang.send(player, "use.unknown", "id", items.read(hand).get().furnitureId());
            }
            return;
        }

        // Con un cofre o una puerta delante manda el bloque, salvo agachado (como con un bloque).
        // isInteractable está obsoleto porque también da true en escaleras y vallas; aquí eso solo
        // pide agacharse para poner un mueble encima, que es lo mismo que pide un bloque vanilla.
        if (furniture.isEmpty() && clicked.getType().asBlockType().isInteractable() && !player.isSneaking()) {
            return;
        }

        deny(event);
        if (!player.hasPermission("rpgroll.furniture.use")) {
            return;
        }

        String variant = items.read(hand).map(FurnitureItems.Ref::variant).orElse(null);
        PlaceResult result = service.place(player, placing.get(), variant, clicked, event.getBlockFace(), hand);

        if (result == PlaceResult.OK) {
            player.swingMainHand();
            if (player.getGameMode() != GameMode.CREATIVE) {
                hand.setAmount(hand.getAmount() - 1);
            }
        } else {
            lang.send(player, result.langKey);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {

        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Interaction interaction)) {
            return;
        }

        service.fromEntity(interaction).ifPresent(furniture -> {
            event.setCancelled(true);
            Player player = event.getPlayer();
            interactions.use(player, furniture, player.getInventory().getItemInMainHand());
            player.swingMainHand();
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttackEntity(PrePlayerAttackEntityEvent event) {

        if (!(event.getAttacked() instanceof Interaction interaction)) {
            return;
        }
        service.fromEntity(interaction).ifPresent(furniture -> {
            event.setCancelled(true);
            tryBreak(event.getPlayer(), furniture);
        });
    }

    /** En creativo una barrera se rompe de un golpe: se trata como retirar el mueble. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        if (Protection.testing()) {
            return;
        }
        service.at(event.getBlock()).ifPresent(furniture -> {
            event.setCancelled(true);
            tryBreak(event.getPlayer(), furniture);
        });
    }

    /** Nada se pone dentro de un mueble (la casilla de una lámpara parece aire, pero no lo está). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {

        if (Protection.testing()) {
            return;
        }
        if (service.index().isOccupied(event.getBlockPlaced()) || items.read(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
        }
    }

    private void tryBreak(Player player, PlacedFurniture furniture) {

        if (player.getGameMode() == GameMode.ADVENTURE && !player.hasPermission("rpgroll.furniture.bypass")) {
            return;
        }
        if (!player.hasPermission("rpgroll.furniture.use")) {
            return;
        }

        RemoveResult result = service.canRemove(player, furniture);
        switch (result) {
            case OK -> service.removeBy(player, furniture);
            case NOT_OWNER -> lang.send(player, "use.not_owner");
            case PROTECTED -> lang.send(player, "use.protected");
        }
    }

    private static void deny(PlayerInteractEvent event) {
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }
}
