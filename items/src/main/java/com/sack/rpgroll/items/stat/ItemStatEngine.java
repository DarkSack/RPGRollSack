package com.sack.rpgroll.items.stat;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.socket.SocketService;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Recalcula, por jugador, la suma de stats de todos sus ítems RPGRoll
 * equipados (armadura + mano principal + secundaria), y aplica el
 * resultado donde hay un efecto mecánico directo posible sin tocar otros
 * módulos: vida máxima y velocidad de movimiento vía atributos de Bukkit.
 * El resto de los stats (daño, defensa, crítico, etc.) quedan expuestos
 * vía {@link #getStat(Player, String)} para que un listener de combate
 * (ver {@link com.sack.rpgroll.items.listener.ItemCombatListener}) los
 * consulte al resolver daño.
 */
public class ItemStatEngine {

    private final ItemManager itemManager;
    private final ItemFactory itemFactory;
    private final ItemInstanceService instanceService;
    private final SocketService socketService;
    private final NamespacedKey healthModifierKey;
    private final NamespacedKey speedModifierKey;

    private final Map<UUID, ItemStatSheet> sheets = new HashMap<>();

    public ItemStatEngine(Plugin plugin, ItemManager itemManager, ItemFactory itemFactory,
            ItemInstanceService instanceService, SocketService socketService) {
        this.itemManager = itemManager;
        this.itemFactory = itemFactory;
        this.instanceService = instanceService;
        this.socketService = socketService;
        this.healthModifierKey = new NamespacedKey(plugin, "items-health-modifier");
        this.speedModifierKey = new NamespacedKey(plugin, "items-speed-modifier");
    }

    public double getStat(Player player, String statId) {
        return sheets.computeIfAbsent(player.getUniqueId(), id -> new ItemStatSheet()).get(statId);
    }

    public void recompute(Player player) {

        ItemStatSheet sheet = new ItemStatSheet();
        PlayerInventory inventory = player.getInventory();

        for (ItemStack item : relevantItems(inventory)) {
            addContribution(sheet, item);
        }

        sheets.put(player.getUniqueId(), sheet);
        applyToAttributes(player, sheet);
    }

    public void clear(Player player) {
        sheets.remove(player.getUniqueId());
    }

    private ItemStack[] relevantItems(PlayerInventory inventory) {
        return new ItemStack[] {
                inventory.getItemInMainHand(), inventory.getItemInOffHand(),
                inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()
        };
    }

    private void addContribution(ItemStatSheet sheet, ItemStack item) {

        if (item == null || item.getType().isAir()) {
            return;
        }

        var idOpt = instanceService.getId(item);
        if (idOpt.isEmpty()) {
            return;
        }

        var definitionOpt = itemManager.get(idOpt.get());
        if (definitionOpt.isEmpty()) {
            return;
        }

        ItemDefinition definition = definitionOpt.get();
        int upgradeLevel = instanceService.getUpgradeLevel(item);

        itemFactory.combinedStats(definition, upgradeLevel).forEach(sheet::add);
        socketService.socketStatBonus(item).forEach(sheet::add);
    }

    private void applyToAttributes(Player player, ItemStatSheet sheet) {

        applyAttribute(player, Attribute.MAX_HEALTH, healthModifierKey, sheet.get("health"),
                AttributeModifier.Operation.ADD_NUMBER);
        applyAttribute(player, Attribute.MOVEMENT_SPEED, speedModifierKey, sheet.get("speed") / 100.0,
                AttributeModifier.Operation.ADD_SCALAR);
    }

    private void applyAttribute(Player player, Attribute attribute, NamespacedKey key, double amount,
            AttributeModifier.Operation operation) {

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.getModifiers().stream()
                .filter(modifier -> modifier.getKey().equals(key))
                .findFirst()
                .ifPresent(instance::removeModifier);

        if (amount == 0) {
            return;
        }

        instance.addModifier(new AttributeModifier(key, amount, operation));
    }

}
