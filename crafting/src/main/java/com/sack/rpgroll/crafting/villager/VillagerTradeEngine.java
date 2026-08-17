package com.sack.rpgroll.crafting.villager;

import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.integration.CharacterXpBridge;
import com.sack.rpgroll.crafting.integration.EconomyBridge;
import com.sack.rpgroll.crafting.quality.CraftQuality;
import com.sack.rpgroll.crafting.quality.QualityRoller;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;

import io.papermc.paper.event.player.PlayerTradeEvent;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Un aldeano "vinculado" (ver {@code VillagerBinding} y
 * {@code /craftingadmin villager bind}) reemplaza los comercios que Bukkit le
 * asignaría normalmente por {@code VillagerTradeDefinition}s propias — ver
 * {@link #onAcquire} y {@link #onReplenish}. {@link #onPlayerTrade} es quien
 * aplica condiciones/costo de Economy/xp cuando un jugador efectivamente usa
 * uno de esos comercios (el resultado se etiqueta con el id de la definición
 * en su PDC para poder reconocerlo acá, ya que {@code MerchantRecipe} no
 * tiene un id propio).
 */
public class VillagerTradeEngine implements Listener {

    private final Plugin plugin;
    private final VillagerTradeManager tradeManager;
    private final ConditionEvaluator conditionEvaluator;
    private final RecipeResultFactory resultFactory;
    private final QualityRoller qualityRoller = new QualityRoller();
    private final Random random = new Random();
    private final NamespacedKey tradeIdKey;

    public VillagerTradeEngine(Plugin plugin, VillagerTradeManager tradeManager, ConditionEvaluator conditionEvaluator,
            RecipeResultFactory resultFactory) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.conditionEvaluator = conditionEvaluator;
        this.resultFactory = resultFactory;
        this.tradeIdKey = new NamespacedKey(plugin, "villager_trade_id");
    }

    @EventHandler
    public void onAcquire(VillagerAcquireTradeEvent event) {
        // El comercio nuevo se va a agregar al final de la lista actual, así
        // que su índice futuro es el tamaño actual (antes de agregarlo).
        replacement(event.getEntity(), event.getEntity().getRecipeCount()).ifPresent(event::setRecipe);
    }

    @EventHandler
    public void onReplenish(VillagerReplenishTradeEvent event) {
        // A diferencia de onAcquire, acá NO se agrega un slot nuevo — se repone
        // uno existente. Si usáramos getRecipeCount() (que no cambia al reponer),
        // dos slots agotados del mismo aldeano siempre resolverían al mismo id.
        // Ubicamos el índice real del comercio que se está reponiendo.
        int index = event.getEntity().getRecipes().indexOf(event.getRecipe());
        replacement(event.getEntity(), index >= 0 ? index : event.getEntity().getRecipeCount())
                .ifPresent(event::setRecipe);
    }

    @EventHandler
    public void onPlayerTrade(PlayerTradeEvent event) {

        String tradeId = readTradeId(event.getTrade().getResult());
        if (tradeId == null) {
            return;
        }

        Optional<VillagerTradeDefinition> defOpt = tradeManager.get(tradeId);
        if (defOpt.isEmpty()) {
            return;
        }

        VillagerTradeDefinition def = defOpt.get();
        Player player = event.getPlayer();

        if (!def.conditions().isEmpty() && !conditionEvaluator.evaluateAll(def.conditions(), player)) {
            event.setCancelled(true);
            return;
        }

        if (def.economyCost() > 0 && !EconomyBridge.charge(player.getUniqueId(), def.economyCurrencyId(),
                def.economyCost(), "Comercio: " + def.displayName())) {
            event.setCancelled(true);
            return;
        }

        if (def.xpAmount() > 0) {
            CharacterXpBridge.grant(player.getUniqueId(), def.xpAmount());
        }
    }

    private Optional<MerchantRecipe> replacement(AbstractVillager villager, int slotIndex) {

        List<String> bound = new ArrayList<>(VillagerBinding.boundTradeIds(plugin, villager));
        if (bound.isEmpty()) {
            return Optional.empty();
        }

        String id = bound.get(Math.floorMod(slotIndex, bound.size()));
        return tradeManager.get(id).flatMap(this::buildRecipe);
    }

    private Optional<MerchantRecipe> buildRecipe(VillagerTradeDefinition def) {

        CraftQuality quality = def.qualityEnabled() ? qualityRoller.roll(0.5) : null;
        Optional<ItemStack> resultOpt = resultFactory.build(def.result(), quality);
        if (resultOpt.isEmpty()) {
            return Optional.empty();
        }

        ItemStack result = resultOpt.get();
        tagTradeId(result, def.id());

        MerchantRecipe recipe = new MerchantRecipe(result, 0, def.maxUses(), def.rewardsExperience(),
                def.villagerExperience(), 0f);

        for (RecipeResult cost : def.costs()) {
            resultFactory.build(cost, null).ifPresent(recipe::addIngredient);
        }

        return Optional.of(recipe);
    }

    private void tagTradeId(ItemStack item, String tradeId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(tradeIdKey, PersistentDataType.STRING, tradeId);
        item.setItemMeta(meta);
    }

    private String readTradeId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(tradeIdKey, PersistentDataType.STRING);
    }

}
