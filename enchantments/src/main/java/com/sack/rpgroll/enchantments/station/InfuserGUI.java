package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.MenuBackground;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Infusor: arriba la pieza de la mano; debajo, las pociones que llevas. Clic en una para infundirla. */
final class InfuserGUI extends InventoryGUI {

    private static final int ARMOR = 4;
    private static final int FIRST = 9;
    private static final int LAST = 35;
    private static final int CLOSE = 40;

    private final Infuser infuser;
    private final LangManager lang;
    private final Map<Integer, Integer> potionSlots = new HashMap<>();
    private ItemStack armor;

    InfuserGUI(Player player, Infuser infuser, LangManager lang) {
        super(player, MenuBackground.title(infuser.menuBackground(), lang.component("station.infuser.title")), 45);
        this.infuser = infuser;
        this.lang = lang;
    }

    private static Component plain(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        var meta = item.getItemMeta();
        meta.displayName(plain(name));
        meta.lore(lore.stream().map(InfuserGUI::plain).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void build() {
        clear();
        potionSlots.clear();
        armor = player.getInventory().getItemInMainHand().clone();

        ItemStack shown = armor.clone();
        var meta = shown.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        Map<String, Integer> current = infuser.infusions(armor);
        lore.add(plain(lang.component("station.infuser.slots", "used", current.size(), "max", infuser.maxPerPiece())));
        meta.lore(lore);
        shown.setItemMeta(meta);
        setItem(ARMOR, shown);

        int slot = FIRST;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && slot <= LAST; i++) {
            ItemStack stack = contents[i];
            PotionEffect effect = infuser.effectOf(stack);
            if (stack == null || effect == null) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            lines.add(lang.component("station.infuser.gives", "effect", infuser.describe(effect)));
            lines.add(lang.component("station.cost_xp", "levels", infuser.costLevels()));
            String key = effect.getType().getKey().getKey();
            if (!current.containsKey(key) && current.size() >= infuser.maxPerPiece()) {
                String oldest = current.keySet().iterator().next();
                lines.add(lang.component("station.infuser.replaces", "effect", infuser.describe(oldest, current.get(oldest))));
            }
            lines.add(lang.component(player.getLevel() >= infuser.costLevels() ? "station.click" : "station.no_xp"));
            ItemStack icon = stack.clone();
            icon.setAmount(1);
            var im = icon.getItemMeta();
            im.lore(lines.stream().map(InfuserGUI::plain).toList());
            icon.setItemMeta(im);
            setItem(slot, icon);
            potionSlots.put(slot, i);
            slot++;
        }
        if (potionSlots.isEmpty()) {
            setItem(22, named(Material.GLASS_BOTTLE, lang.component("station.infuser.no_potions"),
                    List.of(lang.component("station.infuser.no_potions_hint"))));
        }
        setItem(CLOSE, named(Material.BARRIER, lang.component("station.close"), List.of()));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == CLOSE) {
            close();
            return;
        }
        Integer inventorySlot = potionSlots.get(slot);
        if (inventorySlot == null) {
            return;
        }
        ItemStack expected = player.getInventory().getItem(inventorySlot);
        String result = infuser.infuse(player, armor, inventorySlot, expected == null ? null : expected.clone());
        player.sendActionBar(lang.component(result));
        if (!result.endsWith("done")) {
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
        }
        if (infuser.isArmor(player.getInventory().getItemInMainHand())) {
            build();
        } else {
            close();
        }
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        // Nada entra ni sale: la estación trabaja sobre lo que llevas en la mano y en el inventario.
        event.setCancelled(true);
    }
}
