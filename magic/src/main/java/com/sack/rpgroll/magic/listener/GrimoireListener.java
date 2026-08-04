package com.sack.rpgroll.magic.listener;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.magic.core.Grimoire;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.item.MagicItemFactory;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;
import com.sack.rpgroll.magic.runtime.SpellbookManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Click derecho con un grimorio en mano: consume 1 del stack e intenta
 * enseñar todos sus hechizos — los que ya sabe, los que no cumplen el
 * nivel, o los que requieren otro hechizo del árbol todavía no aprendido,
 * se saltean con un mensaje propio, sin cancelar el resto del lote.
 */
public class GrimoireListener implements Listener {

    private final GrimoireManager grimoireManager;
    private final SpellManager spellManager;
    private final SpellbookManager spellbookManager;

    public GrimoireListener(GrimoireManager grimoireManager, SpellManager spellManager,
            SpellbookManager spellbookManager) {
        this.grimoireManager = grimoireManager;
        this.spellManager = spellManager;
        this.spellbookManager = spellbookManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String grimoireId = MagicItemFactory.getGrimoireId(item);

        if (grimoireId == null) {
            return;
        }

        event.setCancelled(true);

        Optional<Grimoire> grimoireOpt = grimoireManager.get(grimoireId);

        if (grimoireOpt.isEmpty()) {
            player.sendMessage(Component.text("Este grimorio ya no existe.", NamedTextColor.RED));
            return;
        }

        Grimoire grimoire = grimoireOpt.get();
        PlayerSpellbook spellbook = spellbookManager.getOrLoad(player);
        int playerLevel = RPGRollAPI.isReady()
                ? RPGRollAPI.get().getPlayer(player.getUniqueId()).map(rp -> rp.getLevel()).orElse(0)
                : 0;

        int learned = 0;

        for (String spellId : grimoire.spellIds()) {

            Optional<Spell> spellOpt = spellManager.get(spellId);

            if (spellOpt.isEmpty()) {
                continue;
            }

            Spell spell = spellOpt.get();

            if (spellbook.knows(spellId)) {
                player.sendMessage(Component.text("Ya sabías '" + spell.displayName() + "'.", NamedTextColor.GRAY));
                continue;
            }

            if (playerLevel < spell.level()) {
                player.sendMessage(Component.text(
                        "Te falta nivel para '" + spell.displayName() + "' (necesitás nivel " + spell.level() + ").",
                        NamedTextColor.RED));
                continue;
            }

            if (spell.hasTreeParent() && !spellbook.knows(spell.treeParentId())) {
                player.sendMessage(Component.text(
                        "Primero necesitás aprender el hechizo anterior del árbol de '" + spell.displayName() + "'.",
                        NamedTextColor.RED));
                continue;
            }

            spellbook.learn(spellId);
            player.sendMessage(Component.text("✔ Aprendiste '" + spell.displayName() + "'.", NamedTextColor.GREEN));
            learned++;
        }

        if (learned > 0 && item != null) {
            item.setAmount(item.getAmount() - 1);
        }
    }

}
