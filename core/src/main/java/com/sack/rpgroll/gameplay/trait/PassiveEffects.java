package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.api.event.CharacterCreatedEvent;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

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
 * {@code perk_<atributo>}.
 */
public class PassiveEffects implements Listener {

    /** Vida y suerte en puntos (2 de vida = un corazón); el resto en fracción (0.05 = 5 %). */
    public record Totals(int health, double damage, double defense, double speed, double luck, double evasion,
            double critical, double attackSpeed) {
    }

    private static final double DAMAGE_PER_STRENGTH_MODIFIER = 0.02;
    private static final double CRITICAL_PER_PERCEPTION_MODIFIER = 0.01;
    // shortcut: un solo perk por atributo y a un umbral fijo, ampliar a varios escalones en un YAML si se
    // quieren árboles de perks como los de Fallout.
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

    // shortcut: los bonos de atributo y de maná de un rasgo (strength-bonus, mana-bonus…) siguen sin aplicarse
    // porque las stats se guardan ya calculadas, ampliar cuando el personaje tenga stats efectivas.
    public Totals totals(RPGPlayer rpgPlayer) {

        Set<String> traitIds = rpgPlayer.getTraits().getTraitIds();

        if (rpgPlayer.getRace() != null) {
            raceManager.get(rpgPlayer.getRace()).ifPresent(race -> traitIds.addAll(race.passiveTraits()));
        }
        if (rpgPlayer.getPlayerClass() != null) {
            classManager.get(rpgPlayer.getPlayerClass()).ifPresent(c -> traitIds.addAll(c.passiveTraits()));
        }

        for (com.sack.rpgroll.api.stats.StatType stat : com.sack.rpgroll.api.stats.StatType.values()) {
            if (rpgPlayer.getStats().get(stat) >= PERK_THRESHOLD) {
                traitIds.add("perk_" + stat.name().toLowerCase(java.util.Locale.ROOT));
            }
        }

        int health = 0;
        double damage = DAMAGE_PER_STRENGTH_MODIFIER * rpgPlayer.getStats().getStrengthModifier();
        double defense = 0;
        double speed = 0;
        double luck = rpgPlayer.getStats().getLuckModifier();
        double evasion = 0;
        double critical = CRITICAL_PER_PERCEPTION_MODIFIER * rpgPlayer.getStats().getWisdomModifier();
        double attackSpeed = 0;

        for (String traitId : traitIds) {
            TraitEffect effect = traitManager.get(traitId).map(Trait::effect).orElse(TraitEffect.empty());
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

    // shortcut: un rasgo ganado a mitad de sesión da su vida, velocidad y suerte al volver a entrar; lo que
    // se consulta en cada golpe, al momento.
    public void apply(Player player, RPGPlayer rpgPlayer) {

        Totals totals = totals(rpgPlayer);

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
