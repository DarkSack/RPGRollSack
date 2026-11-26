package com.sack.rpgroll.structures.material;

import com.sack.rpgroll.common.quick.Things;
import com.sack.rpgroll.structures.api.MaterialProvider;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Un tipo de {@link Things.Give} (item, mueble...) visto como proveedor de material. */
public final class ThingsProvider implements MaterialProvider {

    /** La identidad de la muestra de cada id se guarda tanto: crear el ítem cuesta y se compara por cada pila. */
    static final long IDENTITY_TTL_MS = 5_000;

    private record Cached(Optional<String> identity, long at) {
    }

    private final Things.Give give;
    private final LongSupplier clock;
    private final Map<String, Cached> identities = new ConcurrentHashMap<>();

    public ThingsProvider(Things.Give give) {
        this(give, System::currentTimeMillis);
    }

    ThingsProvider(Things.Give give, LongSupplier clock) {
        this.give = give;
        this.clock = clock;
    }

    @Override
    public String type() {
        return give.type();
    }

    @Override
    public boolean knows(String id) {
        return give.ids().contains(id);
    }

    @Override
    public boolean matches(String id, ItemStack stack) {
        Optional<String> own = ItemIdentity.of(stack);
        return own.isPresent() && own.equals(identity(id));
    }

    private Optional<String> identity(String id) {
        long now = clock.getAsLong();
        Cached c = identities.get(id);
        if (c == null || now - c.at() >= IDENTITY_TTL_MS) {
            c = new Cached(give.create(id).flatMap(ItemIdentity::of), now);
            identities.put(id, c);
        }
        return c.identity();
    }

    @Override
    public Optional<ItemStack> sample(String id) {
        return give.create(id);
    }
}
