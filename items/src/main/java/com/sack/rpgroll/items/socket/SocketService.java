package com.sack.rpgroll.items.socket;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.SocketDefinition;
import com.sack.rpgroll.items.instance.ItemInstanceService;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SocketService {

    public enum Result {
        OK,
        SOCKET_NOT_FOUND,
        ALREADY_FILLED,
        TYPE_NOT_ACCEPTED,
        GEM_NOT_FOUND
    }

    private final ItemInstanceService instanceService;
    private final ItemFactory itemFactory;
    private final GemManager gemManager;
    private final GemItem gemItem;

    public SocketService(ItemInstanceService instanceService, ItemFactory itemFactory, GemManager gemManager,
            GemItem gemItem) {
        this.instanceService = instanceService;
        this.itemFactory = itemFactory;
        this.gemManager = gemManager;
        this.gemItem = gemItem;
    }

    public Result insertGem(ItemStack targetItem, ItemDefinition definition, String socketId, ItemStack gemStack) {

        SocketDefinition socket = definition.sockets().stream()
                .filter(s -> s.id().equalsIgnoreCase(socketId))
                .findFirst()
                .orElse(null);

        if (socket == null) {
            return Result.SOCKET_NOT_FOUND;
        }

        Map<String, String> sockets = instanceService.getSockets(targetItem);

        if (!sockets.getOrDefault(socket.id(), "").isBlank()) {
            return Result.ALREADY_FILLED;
        }

        Optional<String> gemIdOpt = gemItem.getGemId(gemStack);
        if (gemIdOpt.isEmpty()) {
            return Result.GEM_NOT_FOUND;
        }

        Optional<Gem> gemOpt = gemManager.get(gemIdOpt.get());
        if (gemOpt.isEmpty()) {
            return Result.GEM_NOT_FOUND;
        }

        Gem gem = gemOpt.get();

        if (!socket.accepts(gem.type())) {
            return Result.TYPE_NOT_ACCEPTED;
        }

        Map<String, String> updated = new LinkedHashMap<>(sockets);
        updated.put(socket.id(), gem.id());

        ItemMeta meta = targetItem.getItemMeta();
        instanceService.setSockets(meta, updated);
        targetItem.setItemMeta(meta);

        itemFactory.rebuildDisplay(targetItem, definition);

        return Result.OK;
    }

    public Result removeGem(ItemStack targetItem, ItemDefinition definition, String socketId) {

        Map<String, String> sockets = instanceService.getSockets(targetItem);

        if (sockets.getOrDefault(socketId, "").isBlank()) {
            return Result.SOCKET_NOT_FOUND;
        }

        Map<String, String> updated = new LinkedHashMap<>(sockets);
        updated.put(socketId, "");

        ItemMeta meta = targetItem.getItemMeta();
        instanceService.setSockets(meta, updated);
        targetItem.setItemMeta(meta);

        itemFactory.rebuildDisplay(targetItem, definition);

        return Result.OK;
    }

    /** Suma de los stats aportados por todas las gemas insertadas en un ítem. */
    public Map<String, Double> socketStatBonus(ItemStack item) {

        Map<String, Double> bonus = new LinkedHashMap<>();

        for (String gemId : instanceService.getSockets(item).values()) {

            if (gemId.isBlank()) {
                continue;
            }

            gemManager.get(gemId).ifPresent(gem ->
                    gem.statBonus().forEach((stat, amount) -> bonus.merge(stat, amount, Double::sum)));
        }

        return bonus;
    }

}
