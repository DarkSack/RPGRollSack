package com.sack.rpgroll.economy.market;

/**
 * Estado vivo de un producto: cuánta oferta/demanda acumulada tiene en este
 * momento. El precio nunca se guarda directamente — siempre se recalcula a
 * partir de este estado + el {@link MarketProduct} (ver {@link MarketEngine#price}),
 * así no hay forma de que el precio "se desincronice" de la oferta/demanda real.
 */
public class MarketState {

    private final String productId;
    private double supplyAccumulator;
    private double demandAccumulator;
    private long lastRecoveryMillis;

    public MarketState(String productId) {
        this.productId = productId;
        this.lastRecoveryMillis = System.currentTimeMillis();
    }

    public String productId() {
        return productId;
    }

    public double supplyAccumulator() {
        return supplyAccumulator;
    }

    public void setSupplyAccumulator(double supplyAccumulator) {
        this.supplyAccumulator = Math.max(0, supplyAccumulator);
    }

    public double demandAccumulator() {
        return demandAccumulator;
    }

    public void setDemandAccumulator(double demandAccumulator) {
        this.demandAccumulator = Math.max(0, demandAccumulator);
    }

    public long lastRecoveryMillis() {
        return lastRecoveryMillis;
    }

    public void setLastRecoveryMillis(long lastRecoveryMillis) {
        this.lastRecoveryMillis = lastRecoveryMillis;
    }

}
