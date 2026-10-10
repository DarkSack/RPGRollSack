package com.sack.rpgroll.magic.listener;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.common.integration.SoftDepend;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.magic.core.Grimoire;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.item.MagicItemFactory;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;
import com.sack.rpgroll.magic.runtime.SpellbookManager;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.fx.api.RPGRollFXAPI;

import net.kyori.adventure.title.Title;

import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Optional;

/**
 * Click derecho con un grimorio en mano: consume 1 del stack e intenta
 * enseñar todos sus hechizos — los que ya sabe, los que no cumplen el
 * nivel, o los que requieren otro hechizo del árbol todavía no aprendido,
 * se saltean con un mensaje propio, sin cancelar el resto del lote.
 */
public class GrimoireListener implements Listener {

    private static final String LEARN_EFFECT = "spell_learned";

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

        if (learned > 0) {
            celebrate(player, grimoire, learned);
        }
    }

    /**
     * Título, sonido y partículas al aprender. Antes solo salía una línea de chat
     * por hechizo y abrir un grimorio no se sentía como un momento importante.
     */
    private void celebrate(Player player, Grimoire grimoire, int learned) {

        // El nombre del grimorio va de subtítulo: como título no cabe en pantalla.
        player.showTitle(Title.title(
                lang.component(learned == 1 ? "grimoire_listener.title_one" : "grimoire_listener.title_many",
                        "count", learned),
                ComponentUtils.parse(grimoire.displayName()),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(800))));

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        // Con RPGRoll-FX, su efecto de conocimiento arcano; si no, un remolino de runas vanilla.
        boolean played = SoftDepend.enabled("RPGRoll-FX") && RPGRollFXAPI.isReady()
                && RPGRollFXAPI.get().play(LEARN_EFFECT, player);

        if (!played) {
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.2, 0),
                    120, 0.6, 0.8, 0.6, 1.0);
        }
    }

}
