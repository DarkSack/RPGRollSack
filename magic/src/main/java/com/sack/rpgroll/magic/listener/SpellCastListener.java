package com.sack.rpgroll.magic.listener;

import com.sack.rpgroll.magic.core.CatalystManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCastTrigger;
import com.sack.rpgroll.magic.core.SpellCatalyst;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.engine.CastResult;
import com.sack.rpgroll.magic.engine.SpellCastEngine;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Sostener un catalizador válido + click, con el hechizo correspondiente
 * seleccionado en el spellbook, dispara el cast. HOLD delega en {@link
 * SpellChannelManager} en vez de castear directo.
 */
public class SpellCastListener implements Listener {

    private final SpellManager spellManager;
    private final CatalystManager catalystManager;
    private final SpellbookManager spellbookManager;
    private final SpellCastEngine engine;
    private final SpellChannelManager channelManager;

    public SpellCastListener(SpellManager spellManager, CatalystManager catalystManager,
            SpellbookManager spellbookManager, SpellCastEngine engine, SpellChannelManager channelManager) {
        this.spellManager = spellManager;
        this.catalystManager = catalystManager;
        this.spellbookManager = spellbookManager;
        this.engine = engine;
        this.channelManager = channelManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        boolean isLeft = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean isRight = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

        if (!isLeft && !isRight) {
            return;
        }

        ItemStack item = event.getItem();
        String catalystId = MagicItemFactory.getCatalystId(item);

        if (catalystId == null) {
            return;
        }

        Player player = event.getPlayer();
        PlayerSpellbook spellbook = spellbookManager.getOrLoad(player);
        String selectedId = spellbook.selectedSpellId();

        if (selectedId == null) {
            if (isRight) {
                player.sendMessage(Component.text(
                        "No tenés ningún hechizo seleccionado — usá /magic select <id>.", NamedTextColor.RED));
            }
            return;
        }

        Optional<Spell> spellOpt = spellManager.get(selectedId);

        if (spellOpt.isEmpty()) {
            return;
        }

        Spell spell = spellOpt.get();

        boolean matchesTrigger = switch (spell.trigger()) {
            case LEFT_CLICK -> isLeft;
            case RIGHT_CLICK, HOLD -> isRight;
        };

        if (!matchesTrigger) {
            return;
        }

        event.setCancelled(true);

        SpellCatalyst catalyst = catalystManager.get(catalystId).orElse(null);

        if (spell.trigger() == SpellCastTrigger.HOLD) {
            channelManager.start(player, spell, spellbook, catalyst);
            return;
        }

        CastResult result = engine.cast(spell, player, spellbook, catalyst);

        if (!result.success()) {
            result.reasons().forEach(reason -> player.sendMessage(Component.text("✘ " + reason, NamedTextColor.RED)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spellbookManager.unload(event.getPlayer().getUniqueId());
    }

}
