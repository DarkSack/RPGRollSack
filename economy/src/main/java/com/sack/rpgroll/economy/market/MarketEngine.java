package com.sack.rpgroll.economy.market;

import java.util.Map;
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
    private final Map<String, MarketState> states = new ConcurrentHashMap<>();

    public MarketEngine(MarketProductManager productManager, MarketStateStore stateStore) {
        this.productManager = productManager;
        this.stateStore = stateStore;
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

        MarketState state = stateFor(product.id());

        double pressure = (state.demandAccumulator() * product.demandWeight()
                - state.supplyAccumulator() * product.supplyWeight()) / REFERENCE_VOLUME;

        double price = product.basePrice() * (1 + pressure * product.volatility());
        return Math.max(product.minPrice(), Math.min(product.maxPrice(), price));
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
