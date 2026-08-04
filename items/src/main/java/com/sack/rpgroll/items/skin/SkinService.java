package com.sack.rpgroll.items.skin;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.instance.ItemInstanceService;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Cicla la skin activa de un ítem (0 = apariencia base, 1..N = skins
 * definidas) sin tocar ninguno de sus stats — puramente cosmético.
 */
public class SkinService {

    private final ItemInstanceService instanceService;
    private final ItemFactory itemFactory;

    public SkinService(ItemInstanceService instanceService, ItemFactory itemFactory) {
        this.instanceService = instanceService;
        this.itemFactory = itemFactory;
    }

    public int cycleSkin(ItemStack item, ItemDefinition definition) {

        if (definition.skins().isEmpty()) {
            return 0;
        }

        int current = instanceService.getSkinIndex(item);
        int next = (current + 1) % (definition.skins().size() + 1);

        ItemMeta meta = item.getItemMeta();
        instanceService.setSkinIndex(meta, next);
        item.setItemMeta(meta);

        itemFactory.rebuildDisplay(item, definition);

        return next;
    }

}
