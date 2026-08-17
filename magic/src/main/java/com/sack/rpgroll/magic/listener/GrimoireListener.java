package com.sack.rpgroll.magic.listener;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.magic.core.Grimoire;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.item.MagicItemFactory;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;
import com.sack.rpgroll.magic.runtime.SpellbookManager;

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
    private final LangManager lang;

    public GrimoireListener(GrimoireManager grimoireManager, SpellManager spellManager,
            SpellbookManager spellbookManager, LangManager lang) {
        this.grimoireManager = grimoireManager;
        this.spellManager = spellManager;
        this.spellbookManager = spellbookManager;
        this.lang = lang;
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
            lang.send(player, "grimoire_listener.no_longer_exists");
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
                lang.send(player, "grimoire_listener.already_known", "spell", spell.displayName());
                continue;
            }

            if (playerLevel < spell.level()) {
                lang.send(player, "grimoire_listener.missing_level", "spell", spell.displayName(),
                        "level", spell.level());
                continue;
            }

            if (spell.hasTreeParent() && !spellbook.knows(spell.treeParentId())) {
                lang.send(player, "grimoire_listener.missing_tree_parent", "spell", spell.displayName());
                continue;
            }

            spellbook.learn(spellId);
            lang.send(player, "grimoire_listener.learned", "spell", spell.displayName());
            learned++;
        }

        if (learned > 0 && item != null) {
            item.setAmount(item.getAmount() - 1);
        }
    }

}
