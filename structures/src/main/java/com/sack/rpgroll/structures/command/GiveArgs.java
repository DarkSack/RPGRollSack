package com.sack.rpgroll.structures.command;

import com.sack.rpgroll.structures.workbench.WorkbenchTier;

/** Argumentos de {@code /structuresadmin give <jugador> <nivel> [cantidad]}. */
public record GiveArgs(String player, WorkbenchTier tier, int amount) {

    public static final int MAX_AMOUNT = 64;

    /** Un resultado: {@code args} si todo está bien, o {@code error} con la clave del lang. Nunca ambos. */
    public record Result(GiveArgs args, String error) {
        /** Los argumentos, o {@link IllegalStateException} si hubo error. */
        public GiveArgs orElseThrow() {
            if (args == null) {
                throw new IllegalStateException(error);
            }
            return args;
        }

        public boolean ok() {
            return args != null;
        }
    }

    public static Result parse(String[] args) {
        if (args.length < 2) {
            return new Result(null, "admin.give.usage");
        }
        WorkbenchTier tier = WorkbenchTier.byName(args[1]).orElse(null);
        if (tier == null) {
            return new Result(null, "admin.give.bad_tier");
        }
        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                return new Result(null, "admin.give.bad_amount");
            }
        }
        return new Result(new GiveArgs(args[0], tier, Math.max(1, Math.min(MAX_AMOUNT, amount))), null);
    }
}
