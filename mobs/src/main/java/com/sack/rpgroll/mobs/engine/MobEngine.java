package com.sack.rpgroll.mobs.engine;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.reskin.EntityReskin;
import com.sack.rpgroll.common.reskin.EntityReskinService;

import com.sack.rpgroll.mobs.api.MobDeathEvent;
import com.sack.rpgroll.mobs.api.MobSpawnEvent;
import com.sack.rpgroll.mobs.condition.MobConditionContext;
import com.sack.rpgroll.mobs.condition.MobConditionEvaluator;
import com.sack.rpgroll.mobs.core.LootScope;
import com.sack.rpgroll.mobs.core.MobAction;
import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobLootEntry;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.core.MobModel;
import com.sack.rpgroll.mobs.core.MobPhase;
import com.sack.rpgroll.mobs.core.MobSkill;
import com.sack.rpgroll.mobs.core.MobSkin;
import com.sack.rpgroll.mobs.core.MobTrigger;
import com.sack.rpgroll.mobs.instance.MobInstanceService;
import com.sack.rpgroll.mobs.integration.ItemsIntegration;
import com.sack.rpgroll.mobs.integration.QuestsIntegration;
import com.sack.rpgroll.mobs.rarity.MobRarityResolver;
import com.sack.rpgroll.mobs.registry.ActionRegistry;
import com.sack.rpgroll.mobs.registry.MobActionContext;

import com.sack.rpgroll.api.RPGRollAPI;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Orquesta el ciclo de vida completo de un mob: creación, daño/fases,
 * muerte y loot. Es el único lugar que muta {@link ActiveMobState} y
 * aplica cambios de fase — listeners y la tarea de IA solo llaman a estos
 * métodos.
 */
public class MobEngine {


    private final Plugin plugin;
    private final MobManager mobManager;
    private final MobInstanceService instanceService;
    private final ActionRegistry actionRegistry;
    private final MobConditionEvaluator conditionEvaluator;
    private final MobRarityResolver rarityResolver;

    private final Map<UUID, ActiveMobState> activeMobs = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public MobEngine(
            Plugin plugin,
            MobManager mobManager,
            MobInstanceService instanceService,
            ActionRegistry actionRegistry,
            MobConditionEvaluator conditionEvaluator,
            MobRarityResolver rarityResolver) {

        this.plugin = plugin;
        this.mobManager = mobManager;
        this.instanceService = instanceService;
        this.actionRegistry = actionRegistry;
        this.conditionEvaluator = conditionEvaluator;
        this.rarityResolver = rarityResolver;
    }

    public Map<UUID, ActiveMobState> getActiveMobs() {
        return activeMobs;
    }

    /**
     * Vuelve a registrar un mob que ya existía en el mundo pero se perdió de
     * {@link #activeMobs}.
     * <p>
     * Minecraft persiste la entidad al descargar su chunk, y su id de
     * definición sobrevive en el PersistentDataContainer — pero el estado de
     * runtime (fases, daño acumulado, bossbar) vive solo en memoria y se
     * perdía al recargar. Sin esto, un jefe que se aleja y vuelve deja de
     * cambiar de fase y de repartir loot por contribución: sigue vivo pero ya
     * no se comporta como jefe.
     *
     * @return el estado restaurado, o vacío si no es un mob del plugin
     */
    public Optional<ActiveMobState> restoreIfMissing(LivingEntity entity) {

        UUID id = entity.getUniqueId();

        if (activeMobs.containsKey(id)) {
            return Optional.of(activeMobs.get(id));
        }

        Optional<MobDefinition> definitionOpt = instanceService.getDefinitionId(entity)
                .flatMap(mobManager::get);

        if (definitionOpt.isEmpty()) {
            return Optional.empty();
        }

        MobDefinition definition = definitionOpt.get();

        // El daño acumulado no se puede recuperar: nunca se persistió. Se
        // arranca de cero, que es preferible a no rastrear nada.
        ActiveMobState state = new ActiveMobState(id, definition.id(), definition.skills());
        activeMobs.put(id, state);

        if (definition.bossBar().enabled() && !bossBars.containsKey(id)) {
            createBossBar(entity, definition);
        }

        return Optional.of(state);
    }

    public Optional<ActiveMobState> getState(UUID entityId) {
        return Optional.ofNullable(activeMobs.get(entityId));
    }

    /**
     * Auto-sanado barato del passenger de reskin — para llamar desde el tick
     * periódico de IA. Resuelve la skin ya sorteada al spawnear (persistida
     * en el PDC de la entidad) en vez de volver a sortear — así el
     * auto-sanado nunca le cambia la apariencia a un mob ya vivo.
     */
    public void ensureReskinAttached(LivingEntity entity, MobDefinition definition) {

        EntityReskin reskin = resolveActiveReskin(entity, definition.model());
        EntityReskinService.ensureAttached(plugin, entity, reskin);
    }

    private EntityReskin resolveActiveReskin(LivingEntity entity, MobModel model) {

        String skinId = instanceService.getSkinId(entity).orElse(null);

        if (skinId == null) {
            return EntityReskin.NONE;
        }

        return model.skins().stream()
                .filter(skin -> skin.id().equals(skinId))
                .findFirst()
                .map(MobSkin::reskin)
                .orElse(EntityReskin.NONE);
    }

    private MobSkin rollSkin(List<MobSkin> skins) {

        double totalWeight = skins.stream().mapToDouble(MobSkin::weight).sum();
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0;

        for (MobSkin skin : skins) {
            cumulative += skin.weight();
            if (roll < cumulative) {
                return skin;
            }
        }

        return skins.get(skins.size() - 1);
    }

    // ============ Spawn ============

    public Optional<LivingEntity> spawnMob(MobDefinition definition, Location location) {

        EntityType type;
        try {
            type = EntityType.valueOf(definition.model().baseEntityType());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        if (!(location.getWorld().spawnEntity(location, type) instanceof LivingEntity entity)) {
            return Optional.empty();
        }

        instanceService.setDefinitionId(entity, definition.id());
        instanceService.setPhaseIndex(entity, -1);

        applyModel(entity, definition);
        applyStats(entity, definition);
        applyEquipment(entity, definition);

        ActiveMobState state = new ActiveMobState(entity.getUniqueId(), definition.id(), definition.skills());
        activeMobs.put(entity.getUniqueId(), state);

        if (definition.bossBar().enabled()) {
            createBossBar(entity, definition);
        }

        actionRegistry.executeAll(definition.actionsFor(MobTrigger.SPAWN),
                new MobActionContext(entity, definition, null));

        Bukkit.getPluginManager().callEvent(new MobSpawnEvent(entity, definition));

        return Optional.of(entity);
    }

    private void applyModel(LivingEntity entity, MobDefinition definition) {

        var model = definition.model();

        entity.setGlowing(model.glow());
        entity.setInvisible(model.invisible());

        AttributeInstance scaleAttribute = entity.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(model.scale());
        }

        TextColor baseColor = definition.nameColor() != null
                ? parseNameColor(definition.nameColor())
                : rarityResolver.resolveColor(definition.rarityId());

        Component nameComponent = Component.text().color(baseColor)
                .append(ComponentUtils.parse(definition.displayName()))
                .build();

        entity.customName(nameComponent);
        entity.setCustomNameVisible(true);

        applyRandomSkin(entity, model);
    }

    private void applyRandomSkin(LivingEntity entity, MobModel model) {

        if (model.skins().isEmpty()) {
            EntityReskinService.apply(plugin, entity, EntityReskin.NONE);
            return;
        }

        MobSkin chosen = rollSkin(model.skins());
        instanceService.setSkinId(entity, chosen.id());
        EntityReskinService.apply(plugin, entity, chosen.reskin());
    }

    private TextColor parseNameColor(String raw) {

        TextColor named = NamedTextColor.NAMES.value(raw.trim().toLowerCase(Locale.ROOT));
        if (named != null) {
            return named;
        }

        TextColor hex = TextColor.fromHexString(raw.trim());
        return hex != null ? hex : NamedTextColor.WHITE;
    }

    private void applyStats(LivingEntity entity, MobDefinition definition) {

        applyAttribute(entity, Attribute.MAX_HEALTH, definition.stat("health") > 0
                ? definition.stat("health") : entity.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
        entity.setHealth(entity.getAttribute(Attribute.MAX_HEALTH).getValue());

        if (definition.stat("speed") > 0) {
            applyAttribute(entity, Attribute.MOVEMENT_SPEED, definition.stat("speed") / 100.0);
        }
        if (definition.stat("armor") > 0) {
            applyAttribute(entity, Attribute.ARMOR, definition.stat("armor"));
        }
        if (definition.stat("knockback_resistance") > 0) {
            applyAttribute(entity, Attribute.KNOCKBACK_RESISTANCE,
                    Math.min(1.0, definition.stat("knockback_resistance") / 100.0));
        }
    }

    private void applyAttribute(LivingEntity entity, Attribute attribute, double value) {

        AttributeInstance instance = entity.getAttribute(attribute);

        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private void applyEquipment(LivingEntity entity, MobDefinition definition) {

        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return;
        }

        for (var entry : definition.model().equipment().entrySet()) {

            org.bukkit.inventory.ItemStack item = resolveEquipmentItem(entry.getValue());
            if (item == null) {
                continue;
            }

            switch (entry.getKey().toUpperCase(Locale.ROOT)) {
                case "HAND" -> equipment.setItemInMainHand(item);
                case "OFFHAND" -> equipment.setItemInOffHand(item);
                case "HEAD" -> equipment.setHelmet(item);
                case "CHEST" -> equipment.setChestplate(item);
                case "LEGS" -> equipment.setLeggings(item);
                case "FEET" -> equipment.setBoots(item);
                default -> {
                }
            }
        }
    }

    private org.bukkit.inventory.ItemStack resolveEquipmentItem(String reference) {

        if (ItemsIntegration.isAvailable()) {
            var resolved = ItemsIntegration.resolveItemStack(reference);
            if (resolved != null) {
                return resolved;
            }
        }

        try {
            return new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.valueOf(reference.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ============ Daño / Fases ============

    public double applyResistancesAndWeaknesses(MobDefinition definition, double rawDamage,
            org.bukkit.event.entity.EntityDamageEvent.DamageCause cause, LivingEntity attacker) {

        double damage = rawDamage;

        Double resistance = definition.resistances().get(mapCauseToElement(cause));
        if (resistance != null) {
            damage *= (1 - Math.min(1.0, resistance / 100.0));
        }

        for (var weakness : definition.weaknesses()) {
            if (weakness.type() == com.sack.rpgroll.mobs.core.MobWeaknessType.ELEMENT
                    && weakness.key().equalsIgnoreCase(mapCauseToElement(cause))) {
                damage *= (1 + weakness.extraDamagePercent() / 100.0);
            }
        }

        double defense = definition.stat("defense") + definition.stat("resistance");
        if (defense > 0) {
            damage = Math.max(0, damage - defense);
        }

        return damage;
    }

    private String mapCauseToElement(org.bukkit.event.entity.EntityDamageEvent.DamageCause cause) {

        if (cause == null) {
            return "";
        }

        return switch (cause) {
            case FIRE, FIRE_TICK, LAVA -> "fire";
            case DROWNING -> "water";
            case FREEZE -> "ice";
            case LIGHTNING -> "electric";
            case POISON -> "poison";
            case WITHER -> "dark";
            case MAGIC -> "magic";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            case PROJECTILE -> "arrow";
            default -> cause.name().toLowerCase(Locale.ROOT);
        };
    }

    public void recordDamageContribution(UUID entityId, Player attacker, double amount) {
        getState(entityId).ifPresent(state -> state.addDamageContribution(attacker.getUniqueId(), amount));
    }

    /** Tirada de esquive del mob-víctima: true si el ataque entrante debe anularse por completo. */
    public boolean rollDodge(MobDefinition definition) {
        double dodge = definition.stat("dodge");
        return dodge > 0 && Math.random() * 100 < dodge;
    }

    /** Daño del propio ataque del mob, tras aplicar su accuracy (chance de fallar) y critical. */
    public double rollAttackDamage(MobDefinition definition) {

        double accuracy = definition.stat("accuracy");
        if (accuracy > 0 && Math.random() * 100 >= accuracy) {
            return 0;
        }

        double damage = definition.stat("damage");

        double criticalChance = definition.stat("critical_chance");
        if (criticalChance > 0 && Math.random() * 100 < criticalChance) {
            double criticalDamage = definition.stat("critical_damage");
            damage *= 1 + (criticalDamage > 0 ? criticalDamage / 100.0 : 0.5);
        }

        return damage;
    }

    public void handlePostDamage(LivingEntity entity, MobDefinition definition, LivingEntity attacker) {

        ActiveMobState state = activeMobs.get(entity.getUniqueId());
        if (state == null) {
            return;
        }

        actionRegistry.executeAll(definition.actionsFor(MobTrigger.DAMAGED),
                new MobActionContext(entity, definition, attacker));

        runSkillsFor(MobTrigger.DAMAGED, entity, definition, state, attacker);

        checkPhaseTransition(entity, definition, state, attacker);
        updateBossBar(entity, definition);
    }

    private void checkPhaseTransition(LivingEntity entity, MobDefinition definition, ActiveMobState state,
            LivingEntity attacker) {

        var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        double healthPercent = maxHealth <= 0 ? 0 : (entity.getHealth() / maxHealth) * 100.0;

        Optional<MobPhase> phaseOpt = definition.phaseFor(healthPercent);
        if (phaseOpt.isEmpty()) {
            return;
        }

        MobPhase phase = phaseOpt.get();
        int newIndex = definition.phases().indexOf(phase);

        if (newIndex == state.phaseIndex()) {
            return;
        }

        state.setPhaseIndex(newIndex);
        instanceService.setPhaseIndex(entity, newIndex);
        state.addSkills(phase.skills());

        if (phase.bossBarTitle() != null || phase.bossBarColor() != null) {
            updateBossBarOverride(entity, phase);
        }

        if (phase.dialogueLine() != null) {
            broadcastNearby(entity, phase.dialogueLine());
        }

        actionRegistry.executeAll(definition.actionsFor(MobTrigger.PHASE_CHANGE),
                new MobActionContext(entity, definition, attacker));
    }

    private void broadcastNearby(LivingEntity entity, String text) {

        Component message = Component.text().color(NamedTextColor.GOLD)
                .append(ComponentUtils.parse(text)).build();

        entity.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation().distanceSquared(entity.getLocation()) <= 40 * 40)
                .forEach(player -> player.sendMessage(message));
    }

    // ============ Skills / Triggers ============

    public void runSkillsFor(MobTrigger trigger, LivingEntity entity, MobDefinition definition, ActiveMobState state,
            LivingEntity target) {

        for (MobSkill skill : List.copyOf(state.activeSkills())) {

            if (skill.trigger() != trigger) {
                continue;
            }

            if (state.isSkillOnCooldown(skill.id(), skill.cooldownMillis())) {
                continue;
            }

            if (Math.random() * 100 >= skill.chance()) {
                continue;
            }

            if (!conditionEvaluator.evaluateAll(skill.conditions(), new MobConditionContext(entity, target))) {
                continue;
            }

            actionRegistry.executeAll(skill.actions(), new MobActionContext(entity, definition, target));
            state.markSkillUsed(skill.id());
        }
    }

    // ============ Muerte / Loot ============

    public void onMobDeath(LivingEntity entity, MobDefinition definition, Player killer) {

        ActiveMobState state = activeMobs.remove(entity.getUniqueId());
        EntityReskinService.remove(entity);

        actionRegistry.executeAll(definition.actionsFor(MobTrigger.DEATH),
                new MobActionContext(entity, definition, killer));

        if (state != null) {
            rollLoot(definition, state, killer);
        } else if (killer != null) {
            rollLoot(definition, new ActiveMobState(entity.getUniqueId(), definition.id(), List.of()), killer);
        }

        removeBossBar(entity.getUniqueId());
        Bukkit.getPluginManager().callEvent(new MobDeathEvent(entity, definition, killer));
    }

    private void rollLoot(MobDefinition definition, ActiveMobState state, Player killer) {

        if (definition.loot().isEmpty()) {
            return;
        }

        for (MobLootEntry entry : definition.loot()) {

            if (entry.scope() == LootScope.PER_PLAYER && !state.damageContribution().isEmpty()) {
                for (UUID playerId : state.damageContribution().keySet()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        grantLoot(entry, player);
                    }
                }
            } else if (killer != null) {
                grantLoot(entry, killer);
            }
        }
    }

    private void grantLoot(MobLootEntry entry, Player player) {

        if (Math.random() * 100 >= entry.chance()) {
            return;
        }

        int amount = entry.amountMin() == entry.amountMax()
                ? entry.amountMin()
                : entry.amountMin() + (int) (Math.random() * (entry.amountMax() - entry.amountMin() + 1));

        switch (entry.type()) {
            case ITEM -> ItemsIntegration.giveItem(player, entry.reference(), amount);
            case MONEY -> giveMoney(player, amount);
            case EXPERIENCE -> player.giveExp(amount);
            case COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    entry.reference().replace("{player}", player.getName()));
            case QUEST -> QuestsIntegration.startQuest(player, entry.reference());
        }
    }

    private void giveMoney(Player player, double amount) {

        if (!RPGRollAPI.isReady()) {
            return;
        }

        RPGRollAPI.get().getEconomyProvider().getEconomy()
                .ifPresent(economy -> economy.depositPlayer(player, amount));
    }

    // ============ BossBar ============

    private void createBossBar(LivingEntity entity, MobDefinition definition) {

        var def = definition.bossBar();
        Component title = ComponentUtils.parse(def.title() != null ? def.title() : definition.displayName());

        BossBar bossBar = BossBar.bossBar(title, 1.0f, parseColor(def.color()), parseStyle(def.style()));
        bossBars.put(entity.getUniqueId(), bossBar);
    }

    private void updateBossBar(LivingEntity entity, MobDefinition definition) {

        BossBar bossBar = bossBars.get(entity.getUniqueId());
        if (bossBar == null) {
            return;
        }

        var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        float progress = maxHealth <= 0 ? 0 : (float) Math.max(0, Math.min(1.0, entity.getHealth() / maxHealth));

        bossBar.progress(progress);

        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= 50 * 50) {
                player.showBossBar(bossBar);
            } else {
                player.hideBossBar(bossBar);
            }
        }
    }

    private void updateBossBarOverride(LivingEntity entity, MobPhase phase) {

        BossBar bossBar = bossBars.get(entity.getUniqueId());
        if (bossBar == null) {
            return;
        }

        if (phase.bossBarColor() != null) {
            bossBar.color(parseColor(phase.bossBarColor()));
        }
        if (phase.bossBarTitle() != null) {
            bossBar.name(ComponentUtils.parse(phase.bossBarTitle()));
        }
    }

    private void removeBossBar(UUID entityId) {

        BossBar bossBar = bossBars.remove(entityId);

        if (bossBar != null) {
            Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bossBar));
        }
    }

    private BossBar.Color parseColor(String raw) {
        try {
            return BossBar.Color.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay parseStyle(String raw) {
        try {
            return BossBar.Overlay.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

}
