package com.sack.rpgroll.structures.material;

import com.sack.rpgroll.common.quick.Things;
import com.sack.rpgroll.structures.api.MaterialProvider;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Function;

/** Material vanilla sin datos propios: una espada con nombre o un ítem de RPGRoll del mismo material no cuenta. */
public final class VanillaProvider implements MaterialProvider {

    private final Function<Material, ItemStack> plain;

    public VanillaProvider() {
        this(ItemStack::of);
    }

    /** {@code plain} da el ítem pelado de un material; se inyecta por si hay que probar sin servidor. */
    VanillaProvider(Function<Material, ItemStack> plain) {
        this.plain = plain;
    }

    @Override
    public String type() {
        return Things.VANILLA;
    }

    private static Material item(String id) {
        Material m = Material.matchMaterial(id);
        return m != null && m.isItem() ? m : null;
    }

    @Override
    public boolean knows(String id) {
        return item(id) != null;
    }

    @Override
    public boolean matches(String id, ItemStack stack) {
        Material m = item(id);
        return m != null && stack.getType() == m && isPlain(stack, plain.apply(m));
    }

    /** Sin datos propios: igual que el ítem pelado de su material (nombre, lore o PDC lo rompen). */
    static boolean isPlain(ItemStack stack, ItemStack plain) {
        return stack.asOne().isSimilar(plain);
    }

    @Override
    public Optional<ItemStack> sample(String id) {
        Material m = item(id);
        return m == null ? Optional.empty() : Optional.of(plain.apply(m));
    }
}
