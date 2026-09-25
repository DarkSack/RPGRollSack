package com.sack.rpgroll.pass.reward;

/**
 * Una recompensa del pase, del diario o de los votos. Se escribe en YAML como
 * texto corto:
 * <pre>
 * money:500                  dinero por Vault (la moneda principal)
 * key:legendario:1           llave de RPGRoll-Crates
 * item:phoenix_feather:1     ítem de RPGRoll-Items
 * material:diamond:8         ítem vanilla
 * exp:400                    experiencia de personaje de RPGRoll
 * command:lp user {player} permission set tag.fundador true
 * </pre>
 */
public record Reward(Type type, String key, int amount, String raw) {

    public enum Type {
        MONEY, KEY, ITEM, MATERIAL, EXP, COMMAND
    }

}
