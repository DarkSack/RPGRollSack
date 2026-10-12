package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.api.event.CharacterCreatedEvent;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.common.stat.Stats;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * Lo que suman los rasgos de un jugador: los que ha adquirido más los pasivos
 * de su raza y de su clase. Hasta ahora los rasgos eran solo una etiqueta: se
 * leía su {@code effect}, pero nadie lo aplicaba.
 * <p>
 * Daño, defensa, evasión y crítico se consultan en cada golpe (ver
 * CombatEffectsListener). Vida, velocidad, suerte y velocidad de ataque son
 * AttributeModifier: se reaplican al entrar y al cambiar de raza o clase,
 * igual que RaceAttributeApplier.
 * <p>
 * También es donde cada letra de S.P.E.C.I.A.L. que no tenía efecto lo gana:
 * la fuerza suma daño, la percepción (wisdom) crítico y la suerte crítico,
 * evasión y experiencia. (El carisma da experiencia, ver Bootstrap.) Un
 * atributo que llega a {@link #PERK_THRESHOLD} da además su perk: el rasgo
 * {@code perk_<atributo>}. Los demás escalones son un archivo cada uno:
 * {@code perk_<atributo>_<valor>} se gana al llegar a ese valor.
 * <p>
 * Un rasgo también puede sumar a un atributo ({@code strength-bonus}…) o al
 * maná máximo ({@code mana-bonus}): {@link #modifier} es el modificador que
 * cuenta de verdad, y {@link #apply} mueve los máximos de vida y de maná.
 */
public class PassiveEffects implements Listener {

    /** Vida y suerte en puntos (2 de vida = un corazón); el resto en fracción (0.05 = 5 %). */
    public record Totals(int health, double damage, double defense, double speed, double luck, double evasion,
            double critical, double attackSpeed) {
    }

    private static final double DAMAGE_PER_STRENGTH_MODIFIER = 0.02;
    private static final double CRITICAL_PER_PERCEPTION_MODIFIER = 0.01;
    // Los mismos que da el atributo base en CombatStats.
    private static final double EVASION_PER_DEXTERITY_MODIFIER = 0.02;
    private static final double CRITICAL_PER_DEXTERITY_MODIFIER = 0.01;
    private static final int POOL_PER_MODIFIER = 5;
    public static final int PERK_THRESHOLD = 15;
    /** Cada punto de suerte: crítico y evasión. Además es el atributo de suerte de Minecraft (botín, pesca). */
    public static final double CHANCE_PER_LUCK = 0.01;
    /** Y este porcentaje de experiencia, ver Bootstrap. */
    public static final double EXPERIENCE_PERCENT_PER_LUCK = 2.0;

    private final PlayerManager playerManager;
    private final TraitManager traitManager;
    private final RaceManager raceManager;
    private final ClassManager classManager;
    private final Plugin plugin;

    public PassiveEffects(Plugin plugin, PlayerManager playerManager, TraitManager traitManager,
            RaceManager raceManager, ClassManager classManager) {
        this.playerManager = playerManager;
        this.traitManager = traitManager;
        this.raceManager = raceManager;
        this.classManager = classManager;
        this.plugin = plugin;
    }

    /** Los rasgos que cuentan: los adquiridos, los pasivos de raza y clase y los perks de sus atributos. */
    private java.util.List<TraitEffect> effects(RPGPlayer rpgPlayer) {

        Set<String> traitIds = rpgPlayer.getTraits().getTraitIds();

        if (rpgPlayer.getRace() != null) {
            raceManager.get(rpgPlayer.getRace()).ifPresent(race -> traitIds.addAll(race.passiveTraits()));
        }
        if (rpgPlayer.getPlayerClass() != null) {
            classManager.get(rpgPlayer.getPlayerClass()).ifPresent(c -> traitIds.addAll(c.passiveTraits()));
        }

        // Los perks salen del atributo sin bonos: un perk que sumara al suyo no se da el siguiente a sí mismo.
        for (StatType stat : StatType.values()) {
            int value = rpgPlayer.getStats().get(stat);
            String perk = "perk_" + stat.name().toLowerCase(java.util.Locale.ROOT);
            if (value >= PERK_THRESHOLD) {
                traitIds.add(perk);
            }
            for (int step = PlayerStats.DEFAULT_STAT + 1; step <= value; step++) {
                traitIds.add(perk + "_" + step);
            }
        }

        return traitIds.stream()
                .map(traitId -> traitManager.get(traitId).map(Trait::effect).orElse(TraitEffect.empty())).toList();
    }

    private static int attributeBonus(TraitEffect effect, StatType stat) {
        return switch (stat) {
            case STRENGTH -> effect.strengthBonus();
            case DEXTERITY -> effect.dexterityBonus();
            case CONSTITUTION -> effect.constitutionBonus();
            case INTELLIGENCE -> effect.intelligenceBonus();
            case WISDOM -> effect.wisdomBonus();
            case CHARISMA -> effect.charismaBonus();
            // La suerte de un rasgo es luck-bonus y va en puntos de suerte, no de atributo.
            case LUCK -> 0;
        };
    }

    private static int modifier(RPGPlayer rpgPlayer, StatType stat, java.util.List<TraitEffect> effects) {
        int value = rpgPlayer.getStats().get(stat);
        for (TraitEffect effect : effects) {
            value += attributeBonus(effect, stat);
        }
        return (value - 10) / 2;
    }

    /** El modificador de un atributo contando lo que le suman los rasgos. */
    public int modifier(RPGPlayer rpgPlayer, StatType stat) {
        return modifier(rpgPlayer, stat, effects(rpgPlayer));
    }

    public Totals totals(RPGPlayer rpgPlayer) {

        java.util.List<TraitEffect> effects = effects(rpgPlayer);
        PlayerStats stats = rpgPlayer.getStats();
        int extraDexterity = modifier(rpgPlayer, StatType.DEXTERITY, effects) - stats.getDexterityModifier();

        int health = 0;
        double damage = DAMAGE_PER_STRENGTH_MODIFIER * modifier(rpgPlayer, StatType.STRENGTH, effects);
        double defense = 0;
        double speed = 0;
        double luck = stats.getLuckModifier();
        double evasion = EVASION_PER_DEXTERITY_MODIFIER * extraDexterity;
        double critical = CRITICAL_PER_PERCEPTION_MODIFIER * modifier(rpgPlayer, StatType.WISDOM, effects)
                + CRITICAL_PER_DEXTERITY_MODIFIER * extraDexterity;
        double attackSpeed = 0;

        for (TraitEffect effect : effects) {
            health += effect.healthBonus();
            damage += effect.damageBonus();
            defense += effect.defenseBonus();
            speed += effect.speedBonus();
            luck += effect.luckBonus();
            evasion += effect.evasionBonus();
            critical += effect.criticalBonus();
            attackSpeed += effect.attackSpeedBonus();
        }

        return new Totals(health, damage, defense, speed, luck, evasion + CHANCE_PER_LUCK * luck,
                critical + CHANCE_PER_LUCK * luck, attackSpeed);
    }

    /**
     * La suerte que el jugador lleva encima y no sale de sus rasgos ni de su atributo: amuletos, ítems,
     * Ascension, una poción. Vale lo mismo que la propia para crítico, evasión y experiencia.
     */
    public double outsideLuck(Player player, Totals totals) {
        AttributeInstance luck = player.getAttribute(Attribute.LUCK);
        return luck == null ? 0 : luck.getValue() - totals.luck();
    }

    /**
     * Vida y maná máximos de más, {vida, maná}: 5 por cada punto de modificador que los rasgos suben a
     * la resistencia y a la inteligencia, el {@code mana-bonus} de cada rasgo y el maná de Ascension.
     */
    int[] pools(Player player, RPGPlayer rpgPlayer) {

        java.util.List<TraitEffect> effects = effects(rpgPlayer);
        PlayerStats stats = rpgPlayer.getStats();

        int health = POOL_PER_MODIFIER
                * (modifier(rpgPlayer, StatType.CONSTITUTION, effects) - stats.getConstitutionModifier());
        int mana = POOL_PER_MODIFIER
                * (modifier(rpgPlayer, StatType.INTELLIGENCE, effects) - stats.getIntelligenceModifier())
                + (int) Math.round(Stats.bonus(player, Stats.MANA));

        for (TraitEffect effect : effects) {
            mana += effect.manaBonus();
        }

        return new int[] {health, mana};
    }

    // shortcut: un rasgo o un talento ganado a mitad de sesión da su vida, maná, velocidad y suerte al volver
    // a entrar; lo que se consulta en cada golpe, al momento.
    public void apply(Player player, RPGPlayer rpgPlayer) {

        Totals totals = totals(rpgPlayer);

        CombatStats combat = rpgPlayer.getCombatStats();
        int[] pools = pools(player, rpgPlayer);
        if (pools[0] != combat.bonusHealth() || pools[1] != combat.bonusMana()) {
            playerManager.getCache().update(rpgPlayer.updateCombatStats(combat.withBonus(pools[0], pools[1])));
        }

        set(player, Attribute.MAX_HEALTH, "trait-health", totals.health(), AttributeModifier.Operation.ADD_NUMBER);
        set(player, Attribute.MOVEMENT_SPEED, "trait-speed", totals.speed(), AttributeModifier.Operation.ADD_SCALAR);
        set(player, Attribute.LUCK, "trait-luck", totals.luck(), AttributeModifier.Operation.ADD_NUMBER);
        set(player, Attribute.ATTACK_SPEED, "trait-attack-speed", totals.attackSpeed(),
                AttributeModifier.Operation.ADD_SCALAR);
    }

    private void set(Player player, Attribute attribute, String key, double amount,
            AttributeModifier.Operation operation) {

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);

        instance.getModifiers().stream()
                .filter(modifier -> modifier.getKey().equals(namespacedKey))
                .findFirst()
                .ifPresent(instance::removeModifier);

        if (amount != 0) {
            instance.addModifier(new AttributeModifier(namespacedKey, amount, operation));
        }
    }

    @EventHandler
    public void onCharacterCreated(CharacterCreatedEvent event) {
        playerManager.getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(rpgPlayer -> apply(event.getPlayer(), rpgPlayer));
    }

}
