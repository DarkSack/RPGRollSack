package com.sack.rpgroll.economy.wallet;

/**
 * La única comprobación de importes de dinero.
 * <p>
 * {@code amount <= 0} NO basta: con NaN da false y el importe pasaba. Un
 * "/pay alguien NaN" dejaba en NaN el saldo de los dos, y con un saldo NaN
 * {@code saldo - x < mínimo} también da false, así que desde ahí cualquier
 * retiro "salía bien": dinero infinito.
 */
public final class Amounts {

    private Amounts() {
    }

    /** Un importe que se puede mover: finito y mayor que cero. */
    public static boolean valid(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }

}
