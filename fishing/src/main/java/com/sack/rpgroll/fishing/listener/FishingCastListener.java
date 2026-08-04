package com.sack.rpgroll.fishing.listener;

import com.sack.rpgroll.effects.api.EffectsAPI;
import com.sack.rpgroll.fishing.core.Bait;
import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.core.FishingRodManager;
import com.sack.rpgroll.fishing.engine.CatchResult;
import com.sack.rpgroll.fishing.engine.FishingCatchEngine;
import com.sack.rpgroll.fishing.item.FishingItemFactory;
import com.sack.rpgroll.fishing.minigame.FishBattleSession;
import com.sack.rpgroll.fishing.minigame.FishingMinigameManager;
import com.sack.rpgroll.fishing.runtime.FishingProfileManager;
import com.sack.rpgroll.sackeffects.api.SackEffectsAPI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reemplaza el resultado de la pesca vanilla sin reimplementar el cast/
 * espera/mordida — deja que Minecraft haga todo eso, y en {@code
 * CAUGHT_FISH} sustituye lo que iba a soltar por nuestra propia tirada.
 */
public class FishingCastListener implements Listener {

    private final FishingRodManager rodManager;
    private final BaitManager baitManager;
    private final FishingCatchEngine catchEngine;
    private final FishingMinigameManager minigameManager;
    private final FishingProfileManager profileManager;
    private final boolean rpgMode;

    private final Map<UUID, Bait> activeBaitByPlayer = new HashMap<>();

    public FishingCastListener(FishingRodManager rodManager, BaitManager baitManager,
            FishingCatchEngine catchEngine, FishingMinigameManager minigameManager,
            FishingProfileManager profileManager, boolean rpgMode) {
        this.rodManager = rodManager;
        this.baitManager = baitManager;
        this.catchEngine = catchEngine;
        this.minigameManager = minigameManager;
        this.profileManager = profileManager;
        this.rpgMode = rpgMode;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {

        switch (event.getState()) {
            case FISHING -> handleCast(event.getPlayer());
            case CAUGHT_FISH -> handleCatch(event);
            default -> {
            }
        }
    }

    private void handleCast(Player player) {

        ItemStack offhand = player.getInventory().getItemInOffHand();
        String baitId = FishingItemFactory.getBaitId(offhand);

        if (baitId == null) {
            activeBaitByPlayer.remove(player.getUniqueId());
            return;
        }

        baitManager.get(baitId).ifPresentOrElse(bait -> {
            activeBaitByPlayer.put(player.getUniqueId(), bait);
            offhand.setAmount(offhand.getAmount() - 1);
        }, () -> activeBaitByPlayer.remove(player.getUniqueId()));
    }

    private void handleCatch(PlayerFishEvent event) {

        Player player = event.getPlayer();

        if (minigameManager.isFighting(player.getUniqueId())) {
            return;
        }

        Entity caught = event.getCaught();
        if (caught instanceof Item item) {
            item.remove();
        }

        event.setExpToDrop(0);

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String rodId = FishingItemFactory.getRodId(mainHand);
        FishingRod rod = rodId != null ? rodManager.get(rodId).orElse(FishingRod.defaultRod())
                : FishingRod.defaultRod();

        Bait bait = activeBaitByPlayer.remove(player.getUniqueId());

        CatchResult result = catchEngine.resolveCatch(player, event.getHook().getLocation(), rod, bait);

        if (rpgMode && result.outcome() == CatchResult.CatchOutcome.FISH) {

            FishBattleSession session = new FishBattleSession(result, rod.resistance());

            minigameManager.start(player, session,
                    (winner, session1) -> awardCatch(winner, session1.pendingCatch()),
                    loser -> loser.sendMessage(Component.text("✘ El pez se escapó...", NamedTextColor.RED)));
            return;
        }

        awardCatch(player, result);
    }

    private void awardCatch(Player player, CatchResult result) {

        ItemStack item = FishingItemFactory.createCatchItem(result);

        if (item == null) {
            player.sendMessage(Component.text("El agua estaba vacía esta vez...", NamedTextColor.GRAY));
            return;
        }

        player.getInventory().addItem(item).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));

        switch (result.outcome()) {

            case FISH -> {

                var species = result.species();

                player.sendMessage(Component.text(
                        "✔ Pescaste: " + species.displayName() + " (" + result.quality() + ")",
                        NamedTextColor.GREEN));

                profileManager.getOrLoad(player).registerCatch(species.id(), result.weight(), result.length(),
                        result.quality());

                if (species.catchEffectId() != null && SackEffectsAPI.isReady()) {
                    SackEffectsAPI.get().play(species.catchEffectId(), player);
                }

                if (species.catchStatusEffectId() != null && EffectsAPI.isReady()) {
                    EffectsAPI.get().apply(species.catchStatusEffectId(), player);
                }
            }

            case TREASURE -> {
                player.sendMessage(Component.text("✔ ¡Encontraste un tesoro: " + result.treasure().displayName() + "!",
                        NamedTextColor.GOLD));
                profileManager.getOrLoad(player).registerTreasure();
            }

            case JUNK -> {
                player.sendMessage(Component.text("Sacaste basura: " + result.junk().displayName(), NamedTextColor.GRAY));
                profileManager.getOrLoad(player).registerJunk();
            }

            case NOTHING -> {
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeBaitByPlayer.remove(event.getPlayer().getUniqueId());
        profileManager.unload(event.getPlayer().getUniqueId());
    }

}
