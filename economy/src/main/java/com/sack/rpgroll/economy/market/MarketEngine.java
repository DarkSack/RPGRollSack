package com.sack.rpgroll.economy.market;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El motor de oferta y demanda. Cada vez que un jugador le vende un
 * producto al mercado, la oferta acumulada sube y el precio baja; cada vez
 * que le compra, la demanda acumulada sube y el precio sube. Sin nueva
 * actividad, {@link #runRecovery()} (llamada periódicamente, ver
 * {@code market-recovery-interval-ticks}) decae ambos acumuladores un
 * {@code recovery-rate} hacia 0, así el precio vuelve solo hacia
 * {@code base-price} con el tiempo.
 * <p>
 * Fórmula: {@code precio = clamp(base * (1 + presión * volatility), min, max)},
 * donde {@code presión = (demanda*demandWeight - oferta*supplyWeight) / 100}
 * — 100 unidades de oferta/demanda neta equivalen a una "unidad completa" de
 * presión. Con los valores por defecto (volatility 0.25), vender 250
 * unidades netas de un producto sin ninguna compra de por medio genera una
 * presión de -2.5 y un precio de {@code base * (1 - 2.5*0.25) = base * 0.375}.
 */
public class MarketEngine {

    private static final double REFERENCE_VOLUME = 100.0;

    private final MarketProductManager productManager;
    private final MarketStateStore stateStore;
    private final MarketRegionManager regionManager;
    private final Map<String, MarketState> states = new ConcurrentHashMap<>();

    public MarketEngine(MarketProductManager productManager, MarketStateStore stateStore, MarketRegionManager regionManager) {
        this.productManager = productManager;
        this.stateStore = stateStore;
        this.regionManager = regionManager;
    }

    public void loadAll() {
        states.clear();
        states.putAll(stateStore.loadAll());
    }

    public void saveAll() {
        stateStore.saveAll(states);
    }

    private MarketState stateFor(String productId) {
        return states.computeIfAbsent(productId, MarketState::new);
    }

    public double price(String productId) {

        MarketProduct product = productManager.get(productId).orElse(null);
        if (product == null) {
            return 0;
        }

        return price(product);
    }

    public double price(MarketProduct product) {
        return price(product, null);
    }

    /**
     * Igual que {@link #price(MarketProduct)} pero, si se pasa una ubicación, aplica además
     * dos multiplicadores opcionales sobre el precio de oferta/demanda ya calculado:
     * <ol>
     *   <li>La {@link MarketRegion} que contiene esa ubicación (si hay una definida) — su
     *   {@code categoryModifiers}/{@code productModifiers}, ej. "el mineral es más barato en
     *   esta ciudad minera".</li>
     *   <li>Si RPGRoll-Seasons está instalado, la estación actual de ese mundo — el
     *   {@code season-modifiers} del producto, buscado por cada tag de la estación activa
     *   (ej. "harvest" -&gt; los cultivos bajan de precio).</li>
     * </ol>
     * Sin ubicación (o sin región/Seasons), el resultado es idéntico al método de un solo
     * argumento — no rompe ningún llamador existente.
     */
    public double price(MarketProduct product, Location location) {

        MarketState state = stateFor(product.id());

        double pressure = (state.demandAccumulator() * product.demandWeight()
                - state.supplyAccumulator() * product.supplyWeight()) / REFERENCE_VOLUME;

        double price = product.basePrice() * (1 + pressure * product.volatility());

        if (location != null) {
            price *= regionModifier(product, location);
            price *= seasonModifier(product, location);
        }

        return Math.max(product.minPrice(), Math.min(product.maxPrice(), price));
    }

    private double regionModifier(MarketProduct product, Location location) {

        if (regionManager == null || location.getWorld() == null) {
            return 1.0;
        }

        Optional<MarketRegion> region = regionManager.resolve(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ());

        return region.map(r -> r.modifierFor(product)).orElse(1.0);
    }

    private double seasonModifier(MarketProduct product, Location location) {

        if (product.seasonTagModifiers().isEmpty()
                || Bukkit.getPluginManager().getPlugin("RPGRoll-Seasons") == null
                || !com.sack.rpgroll.seasons.api.SeasonsAPI.isReady()) {
            return 1.0;
        }

        Optional<com.sack.rpgroll.seasons.core.Season> season = com.sack.rpgroll.seasons.api.SeasonsAPI.get()
                .getCurrentSeason(location);

        if (season.isEmpty()) {
            return 1.0;
        }

        Set<String> tags = season.get().tags();
        double modifier = 1.0;

        for (String tag : tags) {
            Double tagModifier = product.seasonTagModifiers().get(tag);
            if (tagModifier != null) {
                modifier *= tagModifier;
            }
        }

        return modifier;
    }

    /** Un jugador vendió {@code units} de este producto AL mercado — sube la oferta, baja el precio. */
    public void recordSell(String productId, double units) {
        MarketState state = stateFor(productId);
        state.setSupplyAccumulator(state.supplyAccumulator() + units);
        stateStore.saveAll(states);
    }

    /** Un jugador le compró {@code units} de este producto AL mercado — sube la demanda, sube el precio. */
    public void recordBuy(String productId, double units) {
        MarketState state = stateFor(productId);
        state.setDemandAccumulator(state.demandAccumulator() + units);
        stateStore.saveAll(states);
    }

    /** Decae oferta/demanda acumuladas de todos los productos hacia 0, así el precio vuelve solo a la base. */
    public void runRecovery() {

        boolean changed = false;

        for (MarketProduct product : productManager.getAll()) {

            MarketState state = stateFor(product.id());
            double decay = 1 - Math.min(1, product.recoveryRate());

            if (state.supplyAccumulator() > 0.01 || state.demandAccumulator() > 0.01) {
                state.setSupplyAccumulator(state.supplyAccumulator() * decay);
                state.setDemandAccumulator(state.demandAccumulator() * decay);
                state.setLastRecoveryMillis(System.currentTimeMillis());
                changed = true;
            }
        }

        if (changed) {
            stateStore.saveAll(states);
        }
    }

    public MarketState stateOf(String productId) {
        return stateFor(productId);
    }

}
