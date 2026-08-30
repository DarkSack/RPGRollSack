package com.sack.rpgroll.enchantments.listener;

import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Combinar un ítem (izquierda) con uno de los "libros" que da {@code /renchant give}
 * (derecha) en un yunque aplica el encantamiento guardado en el libro — el único
 * lugar donde esos libros hacen algo, ya que por sí solos son inertes
 * ({@link EnchantmentItem#createBook}).
 */
public class AnvilEnchantListener implements Listener {

    private final EnchantmentManager manager;
    private final EnchantmentItem enchantmentItem;

    public AnvilEnchantListener(EnchantmentManager manager, EnchantmentItem enchantmentItem) {
        this.manager = manager;
        this.enchantmentItem = enchantmentItem;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {

        ItemStack left = event.getInventory().getItem(0);
        ItemStack right = event.getInventory().getItem(1);

        if (left == null || left.getType().isAir() || right == null) {
            return;
        }

        var bookContents = enchantmentItem.readBook(right);
        if (bookContents.isEmpty()) {
            return;
        }

        var enchantmentOpt = manager.get(bookContents.get().enchantId());
        if (enchantmentOpt.isEmpty()) {
            return;
        }

        CustomEnchantment enchantment = enchantmentOpt.get();

        ItemStack result = left.clone();
        EnchantmentItem.ApplyResult applyResult = enchantmentItem.apply(result, enchantment,
                bookContents.get().level());

        // apply() ya valida nivel/categoría/conflicto — si no dio OK no hay que mostrar ningún
        // resultado (antes esto se ignoraba: un libro con un nivel que ya no es válido, ej. por
        // haberse bajado el max-level tras un reload, terminaba consumiéndose y cobrando XP sin
        // aplicar nada de verdad).
        if (applyResult != EnchantmentItem.ApplyResult.OK) {
            event.setResult(null);
            return;
        }

        event.setResult(result);
        event.getInventory().setRepairCost(Math.max(1, bookContents.get().level()) * 2);
    }

}
