package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;

@FunctionalInterface
public interface ItemActionHandler {
    void execute(ItemAction action, ItemActionContext context);
}
