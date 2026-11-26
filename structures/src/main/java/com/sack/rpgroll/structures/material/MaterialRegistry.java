package com.sack.rpgroll.structures.material;

import com.sack.rpgroll.common.quick.Things;
import com.sack.rpgroll.structures.api.MaterialProvider;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Resuelve ids {@code tipo:id}. Orden: proveedores registrados por la API (el último con un tipo gana),
 * Minecraft y por último los {@code Things.Give} que haya en ese momento (se piden cada vez: los
 * módulos pueden habilitarse después).
 */
public final class MaterialRegistry {

    private static final Logger LOG = Logger.getLogger("RPGRoll-Structures");

    private final Supplier<? extends Iterable<Things.Give>> gives;
    private final MaterialProvider vanilla;
    private final Map<String, MaterialProvider> registered = new LinkedHashMap<>();
    /** Un proveedor (y su caché de identidades) por cada Things.Give; se vacía al recargar. */
    private final Map<Things.Give, ThingsProvider> things = new IdentityHashMap<>();

    public MaterialRegistry() {
        this(() -> Things.sources(Things.Give.class), new VanillaProvider());
    }

    MaterialRegistry(Supplier<? extends Iterable<Things.Give>> gives, MaterialProvider vanilla) {
        this.gives = gives;
        this.vanilla = vanilla;
    }

    /** Un tipo ya registrado se sustituye (el último gana) y se avisa en el log. */
    public synchronized void register(MaterialProvider provider) {
        String type = provider.type().toLowerCase(Locale.ROOT);
        if (registered.put(type, provider) != null) {
            LOG.warning("El proveedor de material '" + type + "' ya existía: se sustituye por el último registrado.");
        }
    }

    /** Al recargar: las identidades de muestra se vuelven a pedir. */
    public synchronized void clearCache() {
        things.clear();
    }

    public boolean knows(String fullId) {
        return resolve(fullId, (p, id) -> p.knows(id));
    }

    public boolean matches(String fullId, ItemStack stack) {
        return stack != null && !stack.isEmpty() && resolve(fullId, (p, id) -> p.matches(id, stack));
    }

    public Optional<ItemStack> sample(String fullId) {
        String n = normalize(fullId);
        int colon = n.indexOf(':');
        MaterialProvider p = provider(n.substring(0, colon));
        return p == null ? Optional.empty() : p.sample(n.substring(colon + 1));
    }

    /** {@code oak_planks} pasa a {@code minecraft:oak_planks}; {@code tipo:id} va en minúsculas. */
    public String normalize(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return s.indexOf(':') > 0 ? s : Things.VANILLA + ":" + s;
    }

    /** Suma las pilas que casan con cualquiera de {@code anyOf}. */
    public int count(Inventory inv, MaterialSpec spec) {
        int total = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            for (String id : spec.anyOf()) {
                if (matches(id, stack)) {
                    total += stack.getAmount();
                    break;
                }
            }
        }
        return total;
    }

    private interface Test {
        boolean run(MaterialProvider provider, String id);
    }

    private boolean resolve(String fullId, Test test) {
        String n = normalize(fullId);
        int colon = n.indexOf(':');
        MaterialProvider p = provider(n.substring(0, colon));
        return p != null && test.run(p, n.substring(colon + 1));
    }

    private synchronized MaterialProvider provider(String type) {
        MaterialProvider p = registered.get(type);
        if (p != null) {
            return p;
        }
        if (Things.VANILLA.equals(type)) {
            return vanilla;
        }
        for (Things.Give give : gives.get()) {
            if (!Things.VANILLA.equals(give.type()) && give.type().equalsIgnoreCase(type)) {
                return things.computeIfAbsent(give, ThingsProvider::new);
            }
        }
        return null;
    }
}
