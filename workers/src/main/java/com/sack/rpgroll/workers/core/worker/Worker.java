package com.sack.rpgroll.workers.core.worker;

import com.sack.rpgroll.workers.core.ai.AiAction;
import com.sack.rpgroll.workers.core.economy.WageType;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Un worker puntual — el estado mutable de toda la simulación (nivel/xp
 * por habilidad, necesidades, personalidad, inventario cargado, hogar,
 * contrato, evento activo). La definición de su profesión vive aparte en
 * {@code ProfessionManager}; este objeto solo guarda su id.
 * <p>
 * {@code id} es el MISMO uuid que la entidad de Bukkit real que
 * representa a este worker en el mundo.
 * <p>
 * El inventario cargado es deliberadamente un mapa (material → cantidad),
 * no un {@code Inventory} real de 27 slots — un worker no es un jugador
 * y no necesita preservar metadata de ítems individuales, solo cuánto
 * lleva de qué para saber cuándo está "lleno" y qué depositar en un
 * almacén.
 */
public class Worker {

    public static final int INVENTORY_CAPACITY = 64;

    private final UUID id;
    private String professionId;
    private String customName;
    private PersonalityTrait personality;

    private final Map<String, Integer> skillLevels = new HashMap<>();
    private final Map<String, Double> skillExperience = new HashMap<>();

    private double hunger = 100;
    private double energy = 100;
    private double sleep = 100;
    private double stress;
    private double motivation = 80;
    private double health = 100;
    private double happiness = 80;

    private final Map<String, Integer> carriedItems = new HashMap<>();

    private Location homeLocation;

    private UUID employerId;
    private double wageAmount;
    private WageType wageType = WageType.PER_TASK;
    private long wagePaymentRemainingTicks;

    private String activeWorkerEventId;
    private long eventRemainingTicks;
    private double eventWorkSpeedMultiplier = 1.0;

    private AiAction currentAction = AiAction.IDLE;
    private Location currentTarget;

    public Worker(UUID id, String professionId, PersonalityTrait personality) {
        this.id = id;
        this.professionId = professionId;
        this.personality = personality;
    }

    public UUID id() {
        return id;
    }

    public String professionId() {
        return professionId;
    }

    public void setProfessionId(String professionId) {
        this.professionId = professionId;
    }

    public String customName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public PersonalityTrait personality() {
        return personality;
    }

    public void setPersonality(PersonalityTrait personality) {
        this.personality = personality;
    }

    public int skillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, 0);
    }

    public Map<String, Integer> skillLevels() {
        return skillLevels;
    }

    public double skillExperience(String skillId) {
        return skillExperience.getOrDefault(skillId, 0.0);
    }

    /** Mapa mutable directo — usado por {@code WorkerStore} para restaurar estado ya calculado sin recomputar niveles. */
    public Map<String, Double> skillExperienceMap() {
        return skillExperience;
    }

    /** @return true si subió de nivel con esta experiencia. */
    public boolean addSkillExperience(String skillId, double amount, int maxLevel, double experiencePerLevel) {

        double newExperience = skillExperience(skillId) + amount;
        int currentLevel = skillLevel(skillId);
        boolean leveledUp = false;

        while (currentLevel < maxLevel && newExperience >= experiencePerLevel) {
            newExperience -= experiencePerLevel;
            currentLevel++;
            leveledUp = true;
        }

        skillExperience.put(skillId, newExperience);
        skillLevels.put(skillId, currentLevel);

        return leveledUp;
    }

    public double hunger() {
        return hunger;
    }

    public void setHunger(double hunger) {
        this.hunger = clamp(hunger);
    }

    public double energy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = clamp(energy);
    }

    public double sleep() {
        return sleep;
    }

    public void setSleep(double sleep) {
        this.sleep = clamp(sleep);
    }

    public double stress() {
        return stress;
    }

    public void setStress(double stress) {
        this.stress = clamp(stress);
    }

    public double motivation() {
        return motivation;
    }

    public void setMotivation(double motivation) {
        this.motivation = clamp(motivation);
    }

    public double health() {
        return health;
    }

    public void setHealth(double health) {
        this.health = clamp(health);
    }

    public double happiness() {
        return happiness;
    }

    public void setHappiness(double happiness) {
        this.happiness = clamp(happiness);
    }

    /** Puntaje general de moral (0-100) usado para eventos/rendimiento — promedio de las 7 necesidades. */
    public double morale() {
        return (hunger + energy + sleep + (100 - stress) + motivation + health + happiness) / 7.0;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    public Map<String, Integer> carriedItems() {
        return carriedItems;
    }

    public int carriedTotal() {
        return carriedItems.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isInventoryFull() {
        return carriedTotal() >= INVENTORY_CAPACITY;
    }

    public void addCarried(String materialName, int amount) {
        carriedItems.merge(materialName, amount, Integer::sum);
    }

    public void clearCarried() {
        carriedItems.clear();
    }

    public Location homeLocation() {
        return homeLocation;
    }

    public void setHomeLocation(Location homeLocation) {
        this.homeLocation = homeLocation;
    }

    public boolean isEmployed() {
        return employerId != null;
    }

    public UUID employerId() {
        return employerId;
    }

    public void hire(UUID employerId, double wageAmount, WageType wageType) {
        this.employerId = employerId;
        this.wageAmount = wageAmount;
        this.wageType = wageType;
        this.wagePaymentRemainingTicks = 0;
    }

    public void fire() {
        this.employerId = null;
        this.wageAmount = 0;
        this.wagePaymentRemainingTicks = 0;
    }

    public double wageAmount() {
        return wageAmount;
    }

    public WageType wageType() {
        return wageType;
    }

    public long wagePaymentRemainingTicks() {
        return wagePaymentRemainingTicks;
    }

    public void setWagePaymentRemainingTicks(long ticks) {
        this.wagePaymentRemainingTicks = ticks;
    }

    public boolean hasActiveEvent() {
        return activeWorkerEventId != null;
    }

    public String activeWorkerEventId() {
        return activeWorkerEventId;
    }

    public long eventRemainingTicks() {
        return eventRemainingTicks;
    }

    public double eventWorkSpeedMultiplier() {
        return hasActiveEvent() ? eventWorkSpeedMultiplier : 1.0;
    }

    public void triggerEvent(String eventId, long durationTicks, double workSpeedMultiplier) {
        this.activeWorkerEventId = eventId;
        this.eventRemainingTicks = durationTicks;
        this.eventWorkSpeedMultiplier = workSpeedMultiplier;
    }

    public void clearEvent() {
        this.activeWorkerEventId = null;
        this.eventRemainingTicks = 0;
        this.eventWorkSpeedMultiplier = 1.0;
    }

    public void reduceEventDuration(long ticks) {

        if (!hasActiveEvent()) {
            return;
        }

        eventRemainingTicks -= ticks;

        if (eventRemainingTicks <= 0) {
            clearEvent();
        }
    }

    public AiAction currentAction() {
        return currentAction;
    }

    public void setCurrentAction(AiAction currentAction) {
        this.currentAction = currentAction;
    }

    public Location currentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(Location currentTarget) {
        this.currentTarget = currentTarget;
    }

}
